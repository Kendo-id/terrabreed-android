package com.terrabreed.app.fragments

import android.os.Bundle
import android.view.*
import android.widget.SeekBar
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.terrabreed.app.R
import com.terrabreed.app.api.ApiResult
import com.terrabreed.app.api.SettingsRequest
import com.terrabreed.app.databinding.FragmentControlBinding
import com.terrabreed.app.viewmodels.SensorViewModel
import androidx.lifecycle.lifecycleScope
import com.terrabreed.app.api.ApiClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ControlFragment : Fragment() {
    private var _binding: FragmentControlBinding? = null
    private val binding get() = _binding!!
    private val viewModel: SensorViewModel by activityViewModels()

    private var currentAutoMode = false
    private var targetTemp = 37.5f
    private var targetHumid = 60f

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentControlBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        observeViewModel()
        viewModel.fetchLatest()

        setupActuatorButtons()
        setupTargetControls()
        setupTrayControls()
        setupAutoModeToggle()

        binding.swipeRefresh.setOnRefreshListener { viewModel.fetchLatest() }
        binding.swipeRefresh.setColorSchemeColors(
            ContextCompat.getColor(requireContext(), R.color.accent_primary))
    }

    private fun observeViewModel() {
        viewModel.sensorLatest.observe(viewLifecycleOwner) { result ->
            binding.swipeRefresh.isRefreshing = false
            if (result is ApiResult.Success) {
                val st = result.data.status
                val s  = result.data.sensor

                currentAutoMode = st.autoMode == true
                updateAutoModeUI(currentAutoMode)

                // Actuator status
                updateActuatorBtn(binding.btnHeater, st.heater == true, "🔥 PEMANAS")
                updateActuatorBtn(binding.btnHumidifier, st.humidifier == true, "💧 HUMIDIFIER")
                updateActuatorBtn(binding.btnFan, st.fan == true, "💨 KIPAS")

                // Tray
                binding.tvTrayStatus.text = st.trayPosition
                    ?: if (st.trayTilted == true) "Kiri (-45°)" else "Kanan (+45°)"
                binding.tvMotorStatus.text = st.motorState ?: "stop"

                // Targets
                targetTemp  = s.targetTemp ?: 37.5f
                targetHumid = s.targetHumid ?: 60f
                binding.tvTargetTempValue.text  = "%.1f°C".format(targetTemp)
                binding.tvTargetHumidValue.text = "%.0f%%".format(targetHumid)
                binding.seekTargetTemp.progress  = ((targetTemp - 25f) * 10).toInt().coerceIn(0, 200)
                binding.seekTargetHumid.progress = targetHumid.toInt().coerceIn(20, 95)

                // Live readings
                binding.tvLiveTemp.text  = "%.1f°C".format(s.temp ?: 0f)
                binding.tvLiveHumid.text = "%.1f%%".format(s.humidity ?: 0f)
            }
        }

        viewModel.commandResult.observe(viewLifecycleOwner) { result ->
            if (result is ApiResult.Success) {
                val ok = result.data.ok
                if (ok) {
                    Toast.makeText(requireContext(), "✅ Perintah dikirim", Toast.LENGTH_SHORT).show()
                    viewModel.fetchLatest()
                } else {
                    Toast.makeText(requireContext(), "❌ Gagal", Toast.LENGTH_SHORT).show()
                }
            } else if (result is ApiResult.Error) {
                Toast.makeText(requireContext(), "❌ ${result.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setupActuatorButtons() {
        binding.btnHeater.setOnClickListener {
            val newState = !(binding.btnHeater.tag as? Boolean ?: false)
            viewModel.sendCommand("heater", newState)
        }
        binding.btnHumidifier.setOnClickListener {
            val newState = !(binding.btnHumidifier.tag as? Boolean ?: false)
            viewModel.sendCommand("humidifier", newState)
        }
        binding.btnFan.setOnClickListener {
            val newState = !(binding.btnFan.tag as? Boolean ?: false)
            viewModel.sendCommand("fan", newState)
        }
    }

    private fun setupTrayControls() {
        binding.btnTurnNow.setOnClickListener {
            viewModel.sendCommand("turn_now", true)
            Toast.makeText(requireContext(), "⚙️ Memutar rak...", Toast.LENGTH_SHORT).show()
        }
        binding.btnMotorStop.setOnClickListener {
            viewModel.sendCommand("motor_stop", true)
        }
    }

    private fun setupAutoModeToggle() {
        binding.switchAutoMode.setOnCheckedChangeListener { _, isChecked ->
            viewModel.sendCommand("auto_mode", isChecked)
            currentAutoMode = isChecked
            updateAutoModeUI(isChecked)
        }
    }

    private fun setupTargetControls() {
        // Target Temp seek bar  (range 25°C–45°C, step 0.1)
        binding.seekTargetTemp.max = 200
        binding.seekTargetTemp.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    targetTemp = 25f + (progress / 10f)
                    binding.tvTargetTempValue.text = "%.1f°C".format(targetTemp)
                }
            }
            override fun onStartTrackingTouch(sb: SeekBar) {}
            override fun onStopTrackingTouch(sb: SeekBar) { applySettings() }
        })

        // Target Humid seek bar (range 20%–95%)
        binding.seekTargetHumid.max = 75
        binding.seekTargetHumid.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    targetHumid = (20 + progress).toFloat()
                    binding.tvTargetHumidValue.text = "%.0f%%".format(targetHumid)
                }
            }
            override fun onStartTrackingTouch(sb: SeekBar) {}
            override fun onStopTrackingTouch(sb: SeekBar) { applySettings() }
        })

        binding.btnApplySettings.setOnClickListener { applySettings() }
    }

    private fun applySettings() {
        lifecycleScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    ApiClient.getApi(requireContext()).saveSettings(
                        SettingsRequest(targetTemp = targetTemp, targetHumid = targetHumid)
                    )
                }
                withContext(Dispatchers.Main) {
                    Toast.makeText(requireContext(), "✅ Target diperbarui", Toast.LENGTH_SHORT).show()
                    viewModel.fetchLatest()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(requireContext(), "❌ ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun updateActuatorBtn(btn: android.widget.Button, isOn: Boolean, label: String) {
        btn.tag = isOn
        btn.text = if (isOn) "$label\n● ON" else "$label\n○ OFF"
        btn.backgroundTintList = ContextCompat.getColorStateList(requireContext(),
            if (isOn) R.color.status_ok else R.color.bg_elevated)
        btn.setTextColor(ContextCompat.getColor(requireContext(),
            if (isOn) android.R.color.white else R.color.text_secondary))
    }

    private fun updateAutoModeUI(isAuto: Boolean) {
        binding.switchAutoMode.isChecked = isAuto
        binding.tvAutoModeLabel.text = if (isAuto) "Mode: OTOMATIS ✅" else "Mode: MANUAL ⚠️"
        binding.tvAutoModeLabel.setTextColor(ContextCompat.getColor(requireContext(),
            if (isAuto) R.color.status_ok else R.color.status_warning))
        // Disable manual buttons in auto mode
        val manualEnabled = !isAuto
        binding.btnHeater.isEnabled = manualEnabled
        binding.btnHumidifier.isEnabled = manualEnabled
        binding.btnFan.isEnabled = manualEnabled
        binding.btnHeater.alpha = if (manualEnabled) 1f else 0.4f
        binding.btnHumidifier.alpha = if (manualEnabled) 1f else 0.4f
        binding.btnFan.alpha = if (manualEnabled) 1f else 0.4f
    }

    override fun onDestroyView() { super.onDestroyView(); _binding = null }
}
