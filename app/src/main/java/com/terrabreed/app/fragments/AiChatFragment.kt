package com.terrabreed.app.fragments

import android.os.Bundle
import android.view.*
import android.view.inputmethod.EditorInfo
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.terrabreed.app.R
import com.terrabreed.app.api.ApiClient
import com.terrabreed.app.api.ChatRequest
import com.terrabreed.app.databinding.FragmentAiChatBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

class AiChatFragment : Fragment() {
    private var _binding: FragmentAiChatBinding? = null
    private val binding get() = _binding!!
    private val api get() = ApiClient.getApi(requireContext())

    data class ChatMessage(val role: String, val content: String, val ts: Long = System.currentTimeMillis())
    private val messages = mutableListOf<ChatMessage>()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentAiChatBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        loadChatHistory()

        binding.btnSend.setOnClickListener { sendMessage() }
        binding.etMessage.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEND) { sendMessage(); true } else false
        }
        binding.btnClear.setOnClickListener { clearChat() }

        // Quick prompts
        binding.chipStatus.setOnClickListener {
            binding.etMessage.setText("Bagaimana kondisi mesin tetas saat ini?")
            sendMessage()
        }
        binding.chipTemp.setOnClickListener {
            binding.etMessage.setText("Apakah suhu saat ini sudah sesuai?")
            sendMessage()
        }
        binding.chipAdvice.setOnClickListener {
            binding.etMessage.setText("Berikan saran perawatan telur hari ini")
            sendMessage()
        }
    }

    private fun loadChatHistory() {
        lifecycleScope.launch {
            try {
                val resp = withContext(Dispatchers.IO) { api.getChatHistory(30) }
                if (resp.isSuccessful) {
                    val history = resp.body() ?: return@launch
                    messages.clear()
                    // History dari server urut terbaru dulu, balik
                    history.reversed().forEach {
                        messages.add(ChatMessage(it.role, it.content, it.ts * 1000))
                    }
                    withContext(Dispatchers.Main) {
                        rebuildChatUI()
                    }
                }
            } catch (_: Exception) {
                // Show welcome message if no history
                withContext(Dispatchers.Main) {
                    if (messages.isEmpty()) {
                        addMessageToUI(ChatMessage("assistant",
                            "Halo! Saya TERRA 🐣, asisten AI mesin tetas TerraBreed.\nAda yang bisa saya bantu?"))
                    }
                }
            }
        }
    }

    private fun sendMessage() {
        val text = binding.etMessage.text.toString().trim()
        if (text.isBlank()) return
        binding.etMessage.setText("")

        val userMsg = ChatMessage("user", text)
        messages.add(userMsg)
        addMessageToUI(userMsg)

        // Show typing indicator
        setTyping(true)
        setSendEnabled(false)

        lifecycleScope.launch {
            try {
                val resp = withContext(Dispatchers.IO) {
                    api.sendChat(ChatRequest(text))
                }
                withContext(Dispatchers.Main) {
                    setTyping(false)
                    setSendEnabled(true)
                    if (resp.isSuccessful && resp.body() != null) {
                        val reply = resp.body()!!.reply
                        val aiMsg = ChatMessage("assistant", reply)
                        messages.add(aiMsg)
                        addMessageToUI(aiMsg)

                        // Show commands executed if any
                        val cmds = resp.body()!!.commandsExecuted
                        if (!cmds.isNullOrEmpty()) {
                            val cmdText = cmds.filter { it.ok }
                                .joinToString(", ") { "✅ ${it.command}=${it.value}" }
                            if (cmdText.isNotBlank()) {
                                addMessageToUI(ChatMessage("system", cmdText))
                            }
                        }
                    } else {
                        addMessageToUI(ChatMessage("system", "❌ Server error: ${resp.code()}"))
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    setTyping(false)
                    setSendEnabled(true)
                    addMessageToUI(ChatMessage("system", "❌ Tidak dapat terhubung ke server.\n${e.message}"))
                }
            }
        }
    }

    private fun clearChat() {
        lifecycleScope.launch {
            try { withContext(Dispatchers.IO) { api.clearChat() } } catch (_: Exception) {}
            withContext(Dispatchers.Main) {
                messages.clear()
                binding.chatContainer.removeAllViews()
                addMessageToUI(ChatMessage("assistant", "Chat direset. Ada yang bisa saya bantu?"))
            }
        }
    }

    private fun rebuildChatUI() {
        binding.chatContainer.removeAllViews()
        messages.forEach { addMessageToUI(it, scroll = false) }
        binding.scrollChat.post { binding.scrollChat.fullScroll(View.FOCUS_DOWN) }
    }

    private fun addMessageToUI(msg: ChatMessage, scroll: Boolean = true) {
        val ctx = requireContext()
        val isUser = msg.role == "user"
        val isSystem = msg.role == "system"

        val row = android.widget.LinearLayout(ctx).apply {
            orientation = android.widget.LinearLayout.HORIZONTAL
            val lp = android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { setMargins(0, 4, 0, 4) }
            layoutParams = lp
            gravity = when {
                isUser -> android.view.Gravity.END
                else -> android.view.Gravity.START
            }
        }

        // Avatar for AI
        if (!isUser && !isSystem) {
            val avatar = android.widget.TextView(ctx).apply {
                text = "🐣"
                textSize = 20f
                val lp = android.widget.LinearLayout.LayoutParams(40.dp, 40.dp).apply {
                    setMargins(0, 0, 8, 0)
                    gravity = android.view.Gravity.BOTTOM
                }
                layoutParams = lp
                gravity = android.view.Gravity.CENTER
            }
            row.addView(avatar)
        }

        val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
        val timeStr = sdf.format(Date(msg.ts))

        val bubble = android.widget.LinearLayout(ctx).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            val maxW = (resources.displayMetrics.widthPixels * 0.78).toInt()
            val lp = android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT,
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                if (isUser) marginStart = (resources.displayMetrics.widthPixels * 0.15).toInt()
                else marginEnd = (resources.displayMetrics.widthPixels * 0.15).toInt()
            }
            layoutParams = lp
            background = ContextCompat.getDrawable(ctx, when {
                isUser -> R.drawable.bg_chat_user
                isSystem -> R.drawable.bg_card
                else -> R.drawable.bg_chat_ai
            })
            val pad = 12.dp
            setPadding(pad, 8.dp, pad, 8.dp)
        }

        val tvContent = android.widget.TextView(ctx).apply {
            text = msg.content
            textSize = 14f
            setTextColor(ContextCompat.getColor(ctx, if (isSystem) R.color.text_secondary else R.color.text_primary))
            setLineSpacing(2f, 1f)
        }
        bubble.addView(tvContent)

        val tvTime = android.widget.TextView(ctx).apply {
            text = timeStr
            textSize = 10f
            setTextColor(ContextCompat.getColor(ctx, R.color.text_disabled))
            val lp = android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT,
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { gravity = if (isUser) android.view.Gravity.END else android.view.Gravity.START }
            layoutParams = lp
        }
        bubble.addView(tvTime)

        row.addView(bubble)
        binding.chatContainer.addView(row)

        if (scroll) binding.scrollChat.post { binding.scrollChat.fullScroll(View.FOCUS_DOWN) }
    }

    private val Int.dp get() = (this * resources.displayMetrics.density).toInt()

    private fun setTyping(show: Boolean) {
        binding.layoutTyping.visibility = if (show) View.VISIBLE else View.GONE
        if (show) binding.scrollChat.post { binding.scrollChat.fullScroll(View.FOCUS_DOWN) }
    }

    private fun setSendEnabled(enabled: Boolean) {
        binding.btnSend.isEnabled = enabled
        binding.etMessage.isEnabled = enabled
    }

    override fun onDestroyView() { super.onDestroyView(); _binding = null }
}
