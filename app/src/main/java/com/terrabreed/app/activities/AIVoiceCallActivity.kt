package com.terrabreed.app.activities

import android.Manifest
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.MotionEvent
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.terrabreed.app.R
import com.terrabreed.app.api.ApiClient
import com.terrabreed.app.api.ChatRequest
import com.terrabreed.app.api.TtsRequest
import com.terrabreed.app.databinding.ActivityAiVoiceCallBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

class AIVoiceCallActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAiVoiceCallBinding
    private val api get() = ApiClient.getApi(this)

    // Audio recording
    private var mediaRecorder: android.media.MediaRecorder? = null
    private var audioFile: File? = null
    private var isRecording = false

    // MediaPlayer for TTS playback
    private var mediaPlayer: MediaPlayer? = null
    private var isAISpeaking = false

    // Chat history for display
    private val chatMessages = mutableListOf<Pair<String, String>>() // role, content

    private val AUDIO_PERMISSION_CODE = 101

    enum class VoiceState { IDLE, RECORDING, PROCESSING, AI_SPEAKING }
    private var currentState = VoiceState.IDLE

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAiVoiceCallBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupUI()
        checkAudioPermission()
        speakAIGreeting()
    }

    private fun setupUI() {
        binding.toolbar.setNavigationOnClickListener { finish() }
        binding.tvCallStatus.text = getString(R.string.ai_voice_greeting)

        // Push-to-talk button
        binding.btnPushToTalk.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    if (currentState == VoiceState.IDLE) {
                        stopAISpeaking()
                        startRecording()
                    }
                    true
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    if (currentState == VoiceState.RECORDING) {
                        stopRecordingAndProcess()
                    }
                    true
                }
                else -> false
            }
        }

        binding.btnEndCall.setOnClickListener { finish() }
        binding.btnClearChat.setOnClickListener {
            chatMessages.clear()
            binding.chatContainer.removeAllViews()
        }
    }

    private fun setState(state: VoiceState) {
        currentState = state
        runOnUiThread {
            when (state) {
                VoiceState.IDLE -> {
                    binding.btnPushToTalk.isEnabled = true
                    binding.tvCallStatus.text = getString(R.string.ai_voice_tap_speak)
                    binding.lottieWave.pauseAnimation()
                    binding.lottieWave.visibility = View.INVISIBLE
                    binding.btnPushToTalk.alpha = 1f
                    binding.tvMicIcon.text = "🎤"
                    binding.pulseRing.visibility = View.GONE
                }
                VoiceState.RECORDING -> {
                    binding.tvCallStatus.text = getString(R.string.ai_voice_listening)
                    binding.lottieWave.playAnimation()
                    binding.lottieWave.visibility = View.VISIBLE
                    binding.btnPushToTalk.alpha = 0.7f
                    binding.tvMicIcon.text = "🔴"
                    binding.pulseRing.visibility = View.VISIBLE
                    vibrateShort()
                }
                VoiceState.PROCESSING -> {
                    binding.btnPushToTalk.isEnabled = false
                    binding.tvCallStatus.text = getString(R.string.ai_voice_processing)
                    binding.lottieWave.pauseAnimation()
                    binding.lottieWave.visibility = View.INVISIBLE
                    binding.tvMicIcon.text = "⏳"
                    binding.pulseRing.visibility = View.GONE
                }
                VoiceState.AI_SPEAKING -> {
                    binding.btnPushToTalk.isEnabled = true
                    binding.tvCallStatus.text = getString(R.string.ai_voice_speaking)
                    binding.lottieWave.playAnimation()
                    binding.lottieWave.visibility = View.VISIBLE
                    binding.tvMicIcon.text = "🔊"
                    binding.pulseRing.visibility = View.VISIBLE
                }
            }
        }
    }

    // ── Recording ─────────────────────────────────────────────────
    private fun startRecording() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED) {
            checkAudioPermission()
            return
        }
        try {
            audioFile = File(cacheDir, "voice_input_${System.currentTimeMillis()}.m4a")
            mediaRecorder = android.media.MediaRecorder().apply {
                setAudioSource(android.media.MediaRecorder.AudioSource.MIC)
                setOutputFormat(android.media.MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(android.media.MediaRecorder.AudioEncoder.AAC)
                setAudioSamplingRate(16000)
                setAudioChannels(1)
                setOutputFile(audioFile!!.absolutePath)
                prepare()
                start()
            }
            isRecording = true
            setState(VoiceState.RECORDING)
        } catch (e: IOException) {
            addMessage("system", "❌ Gagal merekam audio: ${e.message}")
            setState(VoiceState.IDLE)
        }
    }

    private fun stopRecordingAndProcess() {
        if (!isRecording) return
        try {
            mediaRecorder?.apply {
                stop()
                release()
            }
            mediaRecorder = null
            isRecording = false
        } catch (e: Exception) {
            setState(VoiceState.IDLE)
            return
        }

        val file = audioFile ?: run { setState(VoiceState.IDLE); return }
        if (file.length() < 1024) { // Too short
            addMessage("system", "⚠️ Rekaman terlalu pendek, coba lagi.")
            setState(VoiceState.IDLE)
            return
        }

        setState(VoiceState.PROCESSING)
        processVoiceInput(file)
    }

    // ── STT → Chat → TTS Pipeline ─────────────────────────────────
    private fun processVoiceInput(file: File) {
        lifecycleScope.launch {
            try {
                // 1. STT — send audio to /api/stt
                val transcribedText = withContext(Dispatchers.IO) {
                    val requestFile = RequestBody.create("audio/m4a".toMediaTypeOrNull(), file)
                    val audioPart   = MultipartBody.Part.createFormData("audio", file.name, requestFile)
                    val langBody    = RequestBody.create("text/plain".toMediaType(), "id")
                    val resp        = api.speechToText(audioPart, langBody)
                    if (resp.isSuccessful) resp.body()?.text else null
                }

                if (transcribedText.isNullOrBlank()) {
                    addMessage("system", "⚠️ Tidak dapat mengenali suara, coba lagi.")
                    setState(VoiceState.IDLE)
                    return@launch
                }

                addMessage("user", transcribedText)

                // 2. Chat — send text to /api/chat
                val aiReply = withContext(Dispatchers.IO) {
                    val resp = api.sendChat(ChatRequest(transcribedText))
                    if (resp.isSuccessful) resp.body()?.reply else null
                }

                if (aiReply.isNullOrBlank()) {
                    addMessage("system", "⚠️ AI tidak merespons.")
                    setState(VoiceState.IDLE)
                    return@launch
                }

                addMessage("assistant", aiReply)

                // 3. TTS — play AI reply
                playTTS(aiReply)

            } catch (e: Exception) {
                addMessage("system", "❌ Error: ${e.message}")
                setState(VoiceState.IDLE)
            } finally {
                file.delete()
            }
        }
    }

    private fun playTTS(text: String) {
        setState(VoiceState.AI_SPEAKING)
        lifecycleScope.launch {
            try {
                val audioBytes = withContext(Dispatchers.IO) {
                    val prefs = getSharedPreferences("terrabreed_prefs", MODE_PRIVATE)
                    val voice = prefs.getString("tts_voice", "id-ID-GadisNeural") ?: "id-ID-GadisNeural"
                    val resp  = api.textToSpeech(TtsRequest(text, voice))
                    if (resp.isSuccessful) resp.body()?.bytes() else null
                }

                if (audioBytes == null || audioBytes.isEmpty()) {
                    setState(VoiceState.IDLE)
                    return@launch
                }

                // Write to temp file and play
                val tmpFile = File(cacheDir, "tts_output_${System.currentTimeMillis()}.mp3")
                FileOutputStream(tmpFile).use { it.write(audioBytes) }

                withContext(Dispatchers.Main) {
                    mediaPlayer?.release()
                    mediaPlayer = MediaPlayer().apply {
                        setAudioAttributes(
                            AudioAttributes.Builder()
                                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                                .setUsage(AudioAttributes.USAGE_MEDIA)
                                .build()
                        )
                        setDataSource(tmpFile.absolutePath)
                        prepare()
                        start()
                        setOnCompletionListener {
                            tmpFile.delete()
                            setState(VoiceState.IDLE)
                        }
                        setOnErrorListener { _, _, _ ->
                            tmpFile.delete()
                            setState(VoiceState.IDLE)
                            false
                        }
                    }
                    isAISpeaking = true
                }
            } catch (e: Exception) {
                setState(VoiceState.IDLE)
            }
        }
    }

    private fun stopAISpeaking() {
        mediaPlayer?.apply {
            if (isPlaying) stop()
            release()
        }
        mediaPlayer = null
        isAISpeaking = false
    }

    private fun speakAIGreeting() {
        val greeting = getString(R.string.ai_voice_greeting)
        addMessage("assistant", greeting)
        playTTS(greeting)
    }

    // ── UI Helpers ─────────────────────────────────────────────────
    private fun addMessage(role: String, content: String) {
        chatMessages.add(Pair(role, content))
        runOnUiThread {
            val ctx = this
            val bubble = android.widget.TextView(ctx).apply {
                text = content
                textSize = 14f
                val pad = resources.getDimensionPixelSize(R.dimen.chat_bubble_padding)
                setPadding(pad, pad / 2, pad, pad / 2)
                val lp = android.widget.LinearLayout.LayoutParams(
                    android.widget.LinearLayout.LayoutParams.WRAP_CONTENT,
                    android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    val margin = resources.getDimensionPixelSize(R.dimen.chat_bubble_margin)
                    setMargins(
                        if (role == "user") margin * 4 else margin,
                        margin / 2,
                        if (role == "user") margin else margin * 4,
                        margin / 2
                    )
                    gravity = if (role == "user") android.view.Gravity.END else android.view.Gravity.START
                }
                layoutParams = lp
                gravity = if (role == "user") android.view.Gravity.END else android.view.Gravity.START
                setTextColor(ContextCompat.getColor(ctx, R.color.text_primary))
                background = ContextCompat.getDrawable(ctx,
                    if (role == "user") R.drawable.bg_chat_user
                    else R.drawable.bg_chat_ai
                )
                maxWidth = (resources.displayMetrics.widthPixels * 0.8).toInt()
            }

            val container = android.widget.LinearLayout(ctx).apply {
                orientation = android.widget.LinearLayout.HORIZONTAL
                val lp = android.widget.LinearLayout.LayoutParams(
                    android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                    android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
                )
                layoutParams = lp
                gravity = if (role == "user") android.view.Gravity.END else android.view.Gravity.START
            }
            container.addView(bubble)
            binding.chatContainer.addView(container)
            binding.scrollChat.post { binding.scrollChat.fullScroll(View.FOCUS_DOWN) }
        }
    }

    private fun vibrateShort() {
        val vibrator = getSystemService(VIBRATOR_SERVICE) as? Vibrator ?: return
        vibrator.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE))
    }

    // ── Permissions ────────────────────────────────────────────────
    private fun checkAudioPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                this, arrayOf(Manifest.permission.RECORD_AUDIO), AUDIO_PERMISSION_CODE
            )
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == AUDIO_PERMISSION_CODE && grantResults.isEmpty() ||
            grantResults[0] != PackageManager.PERMISSION_GRANTED) {
            addMessage("system", "❌ Izin mikrofon ditolak. Fitur suara tidak tersedia.")
            binding.btnPushToTalk.isEnabled = false
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaRecorder?.release()
        mediaPlayer?.release()
    }
}
