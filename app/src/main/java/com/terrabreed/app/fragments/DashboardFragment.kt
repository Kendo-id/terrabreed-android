package com.terrabreed.app.fragments

import android.os.Bundle
import android.view.*
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.terrabreed.app.R
import com.terrabreed.app.api.ApiResult
import com.terrabreed.app.databinding.FragmentDashboardBinding
import com.terrabreed.app.viewmodels.SensorViewModel

class DashboardFragment : Fragment() {
    private var _binding: FragmentDashboardBinding? = null
    private val binding get() = _binding!!
    private val viewModel: SensorViewModel by activityViewModels()

    private var heaterOn = false
    private var humidifierOn = false
    private var fanOn = false
    private var autoMode = false

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentDashboardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        observeViewModel()
        viewModel.startPolling()
        viewModel.fetchStats()
        viewModel.fetchAlarms()

        binding.swipeRefresh.setOnRefreshListener { viewModel.fetchLatest() }
        binding.swipeRefresh.setColorSchemeColors(
            ContextCompat.getColor(requireContext(), R.color.accent_primary))

        // Chips as toggle buttons
        binding.chipHeater.setOnClickListener {
            if (autoMode) { showAutoModeHint(); return@setOnClickListener }
            heaterOn = !heaterOn
            viewModel.sendCommand("heater", heaterOn)
        }
        binding.chipHumidifier.setOnClickListener {
            if (autoMode) { showAutoModeHint(); return@setOnClickListener }
            humidifierOn = !humidifierOn
            viewModel.sendCommand("humidifier", humidifierOn)
        }
        binding.chipFan.setOnClickListener {
            if (autoMode) { showAutoModeHint(); return@setOnClickListener }
            fanOn = !fanOn
            viewModel.sendCommand("fan", fanOn)
        }
        binding.chipAutoMode.setOnClickListener {
            autoMode = !autoMode
            viewModel.sendCommand("auto_mode", autoMode)
        }

        // Command result feedback
        viewModel.commandResult.observe(viewLifecycleOwner) { result ->
            if (result is ApiResult.Success && result.data.ok) {
                viewModel.fetchLatest()
            }
        }
    }

    private fun observeViewModel() {
        viewModel.sensorLatest.observe(viewLifecycleOwner) { result ->
            binding.swipeRefresh.isRefreshing = false
            if (result is ApiResult.Success) {
                val s  = result.data.sensor
                val st = result.data.status

                binding.tvTemp.text        = "%.1f°C".format(s.temp ?: 0f)
                binding.tvHumid.text       = "%.1f%%".format(s.humidity ?: 0f)
                binding.tvTargetTemp.text  = "Target: %.1f°C".format(s.targetTemp ?: 37.5f)
                binding.tvTargetHumid.text = "Target: %.0f%%".format(s.targetHumid ?: 60f)

                // Update state vars
                heaterOn     = st.heater == true
                humidifierOn = st.humidifier == true
                fanOn        = st.fan == true
                autoMode     = st.autoMode == true

                // Update chip states
                updateChip(binding.chipHeater,     heaterOn,     "🔥 Pemanas")
                updateChip(binding.chipHumidifier, humidifierOn, "💧 Humidifier")
                updateChip(binding.chipFan,        fanOn,        "💨 Kipas")
                updateChip(binding.chipAutoMode,   autoMode,     "⚙️ Auto")

                binding.tvTrayPos.text    = st.trayPosition ?: if (st.trayTilted == true) "Kiri (-45°)" else "Kanan (+45°)"
                binding.tvMotorState.text = st.motorState ?: "stop"

                // Warning color for temp
                val tempColor = when {
                    s.temp == null -> R.color.chart_temp_line
                    s.targetTemp != null && Math.abs(s.temp - s.targetTemp) >= 2f -> R.color.status_error
                    s.targetTemp != null && Math.abs(s.temp - s.targetTemp) >= 0.5f -> R.color.status_warning
                    else -> R.color.chart_temp_line
                }
                binding.tvTemp.setTextColor(ContextCompat.getColor(requireContext(), tempColor))
            }
        }
        viewModel.incubation.observe(viewLifecycleOwner) { result ->
            if (result is ApiResult.Success && result.data.active == true) {
                val s  = result.data
                val sp = s.species?.replaceFirstChar { it.titlecase() } ?: "-"
                binding.tvSessionLabel.text = "🥚 $sp — Hari ke-${s.elapsedDays}/${s.totalDays}"
                binding.cardSession.visibility = View.VISIBLE
            } else {
                binding.cardSession.visibility = View.GONE
            }
        }
    }

    private fun updateChip(chip: com.google.android.material.chip.Chip, isOn: Boolean, label: String) {
        chip.isChecked = isOn
        chip.chipBackgroundColor = ContextCompat.getColorStateList(requireContext(),
            if (isOn) R.color.status_ok_bg else R.color.bg_elevated)
        chip.setTextColor(ContextCompat.getColor(requireContext(),
            if (isOn) R.color.status_ok else R.color.text_secondary))
    }

    private fun showAutoModeHint() {
        Toast.makeText(requireContext(),
            "Mode otomatis aktif. Nonaktifkan Auto untuk kontrol manual.", Toast.LENGTH_SHORT).show()
    }

    override fun onDestroyView() { super.onDestroyView(); _binding = null }
}
