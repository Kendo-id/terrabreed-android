package com.terrabreed.app.fragments

import android.graphics.Color
import android.os.Bundle
import android.view.*
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.github.mikephil.charting.components.LimitLine
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.*
import com.github.mikephil.charting.formatter.ValueFormatter
import com.terrabreed.app.R
import com.terrabreed.app.api.ApiResult
import com.terrabreed.app.api.SensorHistoryPoint
import com.terrabreed.app.databinding.FragmentTemperatureBinding
import com.terrabreed.app.viewmodels.SensorViewModel
import java.text.SimpleDateFormat
import java.util.*

class TemperatureFragment : Fragment() {

    private var _binding: FragmentTemperatureBinding? = null
    private val binding get() = _binding!!
    private val viewModel: SensorViewModel by activityViewModels()
    private var selectedMinutes = 60

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentTemperatureBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupChart()
        setupTimeRangeChips()
        observeViewModel()
        viewModel.fetchTempHistory(selectedMinutes)
        viewModel.fetchStats()

        binding.swipeRefresh.setOnRefreshListener {
            viewModel.fetchTempHistory(selectedMinutes)
            viewModel.fetchStats()
        }
        binding.swipeRefresh.setColorSchemeColors(
            ContextCompat.getColor(requireContext(), R.color.accent_primary)
        )
    }

    private fun setupChart() {
        binding.tempChart.apply {
            description.isEnabled = false
            setTouchEnabled(true)
            isDragEnabled = true
            setScaleEnabled(true)
            setPinchZoom(true)
            setDrawGridBackground(false)
            setBackgroundColor(Color.TRANSPARENT)
            legend.apply {
                textColor = ContextCompat.getColor(requireContext(), R.color.text_secondary)
                textSize = 11f
            }
            xAxis.apply {
                position = XAxis.XAxisPosition.BOTTOM
                textColor = ContextCompat.getColor(requireContext(), R.color.chart_text)
                textSize = 10f
                gridColor = ContextCompat.getColor(requireContext(), R.color.chart_grid)
                axisLineColor = ContextCompat.getColor(requireContext(), R.color.border)
                setDrawGridLines(true)
                granularity = 1f
                valueFormatter = object : ValueFormatter() {
                    private val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
                    override fun getFormattedValue(value: Float): String {
                        return sdf.format(Date(value.toLong() * 1000))
                    }
                }
            }
            axisLeft.apply {
                textColor = ContextCompat.getColor(requireContext(), R.color.chart_text)
                gridColor = ContextCompat.getColor(requireContext(), R.color.chart_grid)
                axisLineColor = ContextCompat.getColor(requireContext(), R.color.border)
                textSize = 10f
                setLabelCount(6, true)
            }
            axisRight.isEnabled = false
            animateX(500)
        }
    }

    private fun setupTimeRangeChips() {
        val chipMap = mapOf(
            binding.chip1h  to 60,
            binding.chip3h  to 180,
            binding.chip6h  to 360,
            binding.chip24h to 1440
        )
        chipMap.forEach { (chip, minutes) ->
            chip.setOnClickListener {
                chipMap.keys.forEach { it.isChecked = false }
                chip.isChecked = true
                selectedMinutes = minutes
                viewModel.fetchTempHistory(minutes)
            }
        }
        binding.chip1h.isChecked = true
    }

    private fun observeViewModel() {
        viewModel.sensorLatest.observe(viewLifecycleOwner) { result ->
            if (result is ApiResult.Success) {
                val sensor = result.data.sensor
                val temp   = sensor.temp
                if (temp != null) {
                    binding.tvCurrentTemp.text = "%.1f".format(temp)
                    val targetTemp = sensor.targetTemp ?: 37.5f
                    val diff = temp - targetTemp
                    val statusColor = when {
                        kotlin.math.abs(diff) < 0.3f -> R.color.status_ok
                        kotlin.math.abs(diff) < 1.0f -> R.color.status_warning
                        else                         -> R.color.status_error
                    }
                    binding.tvTempStatus.setTextColor(
                        ContextCompat.getColor(requireContext(), statusColor)
                    )
                    binding.tvTempStatus.text = when {
                        kotlin.math.abs(diff) < 0.3f -> "✓ Normal"
                        diff > 0 -> "▲ Terlalu Tinggi"
                        else     -> "▼ Terlalu Rendah"
                    }
                    binding.tvTargetTemp.text = "Target: %.1f°C".format(targetTemp)
                    binding.tvDs1.text   = "DS1: %.1f°C".format(sensor.tempDs1 ?: 0f)
                    binding.tvDs2.text   = "DS2: %.1f°C".format(sensor.tempDs2 ?: 0f)
                    binding.tvSht.text   = "SHT: %.1f°C".format(sensor.tempSht ?: 0f)
                    binding.tvHeaterStatus.text = if (result.data.status.heater == true) "🔥 Pemanas ON" else "Pemanas OFF"
                }
            }
        }

        viewModel.stats.observe(viewLifecycleOwner) { result ->
            if (result is ApiResult.Success) {
                val s = result.data
                binding.tvAvgTemp.text = "Rata-rata: %.1f°C".format(s.avgTemp ?: 0f)
                binding.tvMinTemp.text = "Min: %.1f°C".format(s.minTemp ?: 0f)
                binding.tvMaxTemp.text = "Maks: %.1f°C".format(s.maxTemp ?: 0f)
            }
        }

        viewModel.tempHistory.observe(viewLifecycleOwner) { result ->
            binding.swipeRefresh.isRefreshing = false
            when (result) {
                is ApiResult.Loading -> binding.swipeRefresh.isRefreshing = true
                is ApiResult.Success -> updateChart(result.data)
                is ApiResult.Error   -> {}
                else -> {}
            }
        }
    }

    private fun updateChart(data: List<SensorHistoryPoint>) {
        if (data.isEmpty()) {
            binding.tvChartEmpty.visibility = View.VISIBLE
            binding.tempChart.visibility = View.GONE
            return
        }
        binding.tvChartEmpty.visibility = View.GONE
        binding.tempChart.visibility = View.VISIBLE

        val coralColor = ContextCompat.getColor(requireContext(), R.color.chart_temp_line)
        val chartFillColor = ContextCompat.getColor(requireContext(), R.color.chart_temp_fill)
        val targetColor = ContextCompat.getColor(requireContext(), R.color.chart_target)

        val tempEntries   = data.mapNotNull { p -> p.temp?.let   { Entry(p.ts.toFloat(), it) } }
        val targetEntries = data.mapNotNull { p -> p.targetTemp?.let { Entry(p.ts.toFloat(), it) } }

        val tempDataSet = LineDataSet(tempEntries, "Suhu (°C)").apply {
            color = coralColor
            lineWidth = 2.5f
            setCircleColor(coralColor)
            circleRadius = 2f
            setDrawCircleHole(false)
            valueTextSize = 0f
            mode = LineDataSet.Mode.CUBIC_BEZIER
            setDrawFilled(true)
            fillColor = chartFillColor
            fillAlpha = 80
        }

        val targetDataSet = LineDataSet(targetEntries, "Target").apply {
            color = targetColor
            lineWidth = 1.5f
            enableDashedLine(10f, 5f, 0f)
            setDrawCircles(false)
            valueTextSize = 0f
            setDrawFilled(false)
        }

        binding.tempChart.data = LineData(tempDataSet, targetDataSet)
        binding.tempChart.invalidate()

        // Add limit line for target
        data.lastOrNull()?.targetTemp?.let { target ->
            binding.tempChart.axisLeft.removeAllLimitLines()
            val ll = LimitLine(target, "Target ${target}°C").apply {
                lineWidth = 1f
                enableDashedLine(10f, 5f, 0f)
                lineColor = targetColor
                textColor = targetColor
                textSize = 9f
            }
            binding.tempChart.axisLeft.addLimitLine(ll)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
