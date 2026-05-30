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
import com.terrabreed.app.databinding.FragmentHumidityBinding
import com.terrabreed.app.viewmodels.SensorViewModel
import java.text.SimpleDateFormat
import java.util.*

class HumidityFragment : Fragment() {

    private var _binding: FragmentHumidityBinding? = null
    private val binding get() = _binding!!
    private val viewModel: SensorViewModel by activityViewModels()
    private var selectedMinutes = 60

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentHumidityBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupChart()
        setupTimeRangeChips()
        observeViewModel()
        viewModel.fetchHumidHistory(selectedMinutes)

        binding.swipeRefresh.setOnRefreshListener {
            viewModel.fetchHumidHistory(selectedMinutes)
        }
        binding.swipeRefresh.setColorSchemeColors(
            ContextCompat.getColor(requireContext(), R.color.chart_humid_line)
        )
    }

    private fun setupChart() {
        binding.humidChart.apply {
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
                    override fun getFormattedValue(value: Float): String =
                        sdf.format(Date(value.toLong() * 1000))
                }
            }
            axisLeft.apply {
                textColor = ContextCompat.getColor(requireContext(), R.color.chart_text)
                gridColor = ContextCompat.getColor(requireContext(), R.color.chart_grid)
                axisLineColor = ContextCompat.getColor(requireContext(), R.color.border)
                textSize = 10f
                axisMinimum = 0f
                axisMaximum = 100f
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
                viewModel.fetchHumidHistory(minutes)
            }
        }
        binding.chip1h.isChecked = true
    }

    private fun observeViewModel() {
        viewModel.sensorLatest.observe(viewLifecycleOwner) { result ->
            if (result is ApiResult.Success) {
                val sensor = result.data.sensor
                val humid  = sensor.humidity
                if (humid != null) {
                    binding.tvCurrentHumid.text = "%.1f".format(humid)
                    val targetHumid = sensor.targetHumid ?: 60f
                    val diff = humid - targetHumid
                    val statusColor = when {
                        kotlin.math.abs(diff) < 2f  -> R.color.status_ok
                        kotlin.math.abs(diff) < 7f  -> R.color.status_warning
                        else                        -> R.color.status_error
                    }
                    binding.tvHumidStatus.setTextColor(
                        ContextCompat.getColor(requireContext(), statusColor)
                    )
                    binding.tvHumidStatus.text = when {
                        kotlin.math.abs(diff) < 2f -> "✓ Normal"
                        diff > 0  -> "▲ Terlalu Tinggi"
                        else      -> "▼ Terlalu Rendah"
                    }
                    binding.tvTargetHumid.text = "Target: %.0f%%".format(targetHumid)
                    binding.tvHumidifierStatus.text =
                        if (result.data.status.humidifier == true) "💧 Humidifier ON" else "Humidifier OFF"
                }
            }
        }

        viewModel.stats.observe(viewLifecycleOwner) { result ->
            if (result is ApiResult.Success) {
                val s = result.data
                binding.tvAvgHumid.text = "Rata-rata: %.1f%%".format(s.avgHumid ?: 0f)
                binding.tvMinHumid.text = "Min: %.1f%%".format(s.minHumid ?: 0f)
                binding.tvMaxHumid.text = "Maks: %.1f%%".format(s.maxHumid ?: 0f)
            }
        }

        viewModel.humidHistory.observe(viewLifecycleOwner) { result ->
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
            binding.humidChart.visibility = View.GONE
            return
        }
        binding.tvChartEmpty.visibility = View.GONE
        binding.humidChart.visibility = View.VISIBLE

        val blueColor   = ContextCompat.getColor(requireContext(), R.color.chart_humid_line)
        val humidFillColor = ContextCompat.getColor(requireContext(), R.color.chart_humid_fill)
        val targetColor = ContextCompat.getColor(requireContext(), R.color.chart_target)

        val humidEntries  = data.mapNotNull { p -> p.humidity?.let  { Entry(p.ts.toFloat(), it) } }
        val targetEntries = data.mapNotNull { p -> p.targetHumid?.let { Entry(p.ts.toFloat(), it) } }

        val humidDataSet = LineDataSet(humidEntries, "Kelembapan (%)").apply {
            color = blueColor
            lineWidth = 2.5f
            setCircleColor(blueColor)
            circleRadius = 2f
            setDrawCircleHole(false)
            valueTextSize = 0f
            mode = LineDataSet.Mode.CUBIC_BEZIER
            setDrawFilled(true)
            fillColor = humidFillColor
            fillAlpha = 80
        }

        val targetDataSet = LineDataSet(targetEntries, "Target").apply {
            color = targetColor
            lineWidth = 1.5f
            enableDashedLine(10f, 5f, 0f)
            setDrawCircles(false)
            valueTextSize = 0f
        }

        binding.humidChart.data = LineData(humidDataSet, targetDataSet)
        binding.humidChart.invalidate()

        data.lastOrNull()?.targetHumid?.let { target ->
            binding.humidChart.axisLeft.removeAllLimitLines()
            val ll = LimitLine(target, "Target ${target.toInt()}%").apply {
                lineWidth = 1f
                enableDashedLine(10f, 5f, 0f)
                lineColor = targetColor
                textColor = targetColor
                textSize = 9f
            }
            binding.humidChart.axisLeft.addLimitLine(ll)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
