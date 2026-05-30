package com.terrabreed.app.fragments

import android.app.AlertDialog
import android.os.Bundle
import android.view.*
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Spinner
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.terrabreed.app.R
import com.terrabreed.app.api.ApiResult
import com.terrabreed.app.databinding.FragmentIncubationBinding
import com.terrabreed.app.viewmodels.SensorViewModel
import java.text.SimpleDateFormat
import java.util.*
import androidx.lifecycle.lifecycleScope
import com.terrabreed.app.api.ApiClient
import com.terrabreed.app.api.FinishIncubationRequest
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class IncubationFragment : Fragment() {

    private var _binding: FragmentIncubationBinding? = null
    private val binding get() = _binding!!
    private val viewModel: SensorViewModel by activityViewModels()

    private val speciesList = mapOf(
        "ayam"    to Triple("Ayam Kampung",  21, 37.5f),
        "bebek"   to Triple("Bebek",          28, 37.8f),
        "kalkun"  to Triple("Kalkun",         28, 37.5f),
        "puyuh"   to Triple("Puyuh",          17, 37.5f),
        "angsa"   to Triple("Angsa",          30, 37.6f),
        "buaya"   to Triple("Buaya",          90, 32.0f),
        "penyu"   to Triple("Penyu",          60, 29.0f),
        "iguana"  to Triple("Iguana",         65, 30.0f),
        "ular"    to Triple("Ular",           60, 30.0f),
        "merpati" to Triple("Merpati",        18, 37.5f),
        "merak"   to Triple("Merak",          28, 37.5f),
        "emu"     to Triple("Emu",            50, 36.5f)
    )

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentIncubationBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        observeViewModel()
        viewModel.fetchIncubation()

        binding.swipeRefresh.setOnRefreshListener {
            viewModel.fetchIncubation()
        }
        binding.swipeRefresh.setColorSchemeColors(
            ContextCompat.getColor(requireContext(), R.color.accent_primary)
        )

        binding.btnStartSession.setOnClickListener { showStartSessionDialog() }
        binding.btnFinishSession.setOnClickListener { showFinishSessionDialog() }
    }

    private fun observeViewModel() {
        viewModel.incubation.observe(viewLifecycleOwner) { result ->
            binding.swipeRefresh.isRefreshing = false
            when (result) {
                is ApiResult.Loading -> binding.swipeRefresh.isRefreshing = true
                is ApiResult.Success -> {
                    val s = result.data
                    if (s.active == true) {
                        showActiveSession(s)
                    } else {
                        showNoSession()
                    }
                }
                is ApiResult.Error -> showNoSession()
                else -> {}
            }
        }
    }

    private fun showActiveSession(s: com.terrabreed.app.api.IncubationSession) {
        binding.groupActiveSession.visibility = View.VISIBLE
        binding.groupNoSession.visibility     = View.GONE
        binding.btnFinishSession.visibility   = View.VISIBLE
        binding.btnStartSession.visibility    = View.GONE

        val spInfo = speciesList[s.species?.lowercase()]
        val speciesLabel = spInfo?.first ?: (s.species?.replaceFirstChar { it.titlecase() } ?: "-")

        binding.tvSpecies.text    = speciesLabel
        binding.tvTotalEggs.text  = "${s.totalEggs ?: 0} butir"
        binding.tvDayCount.text   = "Hari ke-${s.elapsedDays ?: 0} / ${s.totalDays ?: 0}"
        binding.tvRemainingDays.text = "${(s.totalDays ?: 0) - (s.elapsedDays ?: 0)} hari lagi"
        binding.tvNotes.text      = if (!s.notes.isNullOrBlank()) s.notes else "-"

        // Started date
        val sdf = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())
        binding.tvStartedAt.text  = sdf.format(Date((s.startedAt) * 1000))

        // Estimated hatch
        val hatchTs = s.startedAt + ((s.totalDays ?: 0) * 86400L)
        binding.tvEstimatedHatch.text = sdf.format(Date(hatchTs * 1000))

        // Progress bar
        val totalDays   = (s.totalDays ?: 1).coerceAtLeast(1)
        val elapsedDays = (s.elapsedDays ?: 0).coerceIn(0, totalDays)
        val progress    = (elapsedDays * 100 / totalDays)
        binding.progressIncubation.progress = progress
        binding.tvProgress.text = "$progress%"

        // Color progress by stage
        val progressColor = when {
            progress < 50  -> R.color.status_ok
            progress < 80  -> R.color.status_warning
            else           -> R.color.accent_coral
        }
        binding.progressIncubation.setIndicatorColor(
            ContextCompat.getColor(requireContext(), progressColor)
        )

        // Species icon & param
        binding.tvSpeciesParams.text =
            "Suhu: ${spInfo?.third ?: "--"}°C  |  Lama: ${s.totalDays ?: "--"} hari"
    }

    private fun showNoSession() {
        binding.groupActiveSession.visibility = View.GONE
        binding.groupNoSession.visibility     = View.VISIBLE
        binding.btnStartSession.visibility    = View.VISIBLE
        binding.btnFinishSession.visibility   = View.GONE
    }

    private fun showStartSessionDialog() {
        val ctx = requireContext()
        val layout = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 32, 48, 16)
        }
        val spinnerSpecies = Spinner(ctx)
        val speciesKeys = speciesList.keys.toList()
        val speciesNames = speciesList.values.map { it.first }
        val adapter = android.widget.ArrayAdapter(ctx, android.R.layout.simple_spinner_item, speciesNames)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerSpecies.adapter = adapter

        val etEggs = EditText(ctx).apply {
            hint = "Jumlah telur"
            inputType = android.text.InputType.TYPE_CLASS_NUMBER
        }
        val etNotes = EditText(ctx).apply {
            hint = "Catatan (opsional)"
        }

        layout.addView(android.widget.TextView(ctx).apply { text = "Pilih Spesies:" })
        layout.addView(spinnerSpecies)
        layout.addView(android.widget.TextView(ctx).apply { text = "Jumlah Telur:" })
        layout.addView(etEggs)
        layout.addView(android.widget.TextView(ctx).apply { text = "Catatan:" })
        layout.addView(etNotes)

        AlertDialog.Builder(ctx)
            .setTitle("Mulai Sesi Inkubasi")
            .setView(layout)
            .setPositiveButton("Mulai") { _, _ ->
                val key        = speciesKeys[spinnerSpecies.selectedItemPosition]
                val info       = speciesList[key]!!
                val totalDays  = info.second
                val totalEggs  = etEggs.text.toString().toIntOrNull() ?: 0
                val notes      = etNotes.text.toString()
                viewModel.startIncubation(key, totalDays, totalEggs, notes)
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    private fun showFinishSessionDialog() {
        val ctx = requireContext()
        val layout = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 32, 48, 16)
        }
        val etHatched   = EditText(ctx).apply { hint = "Jumlah menetas"; inputType = android.text.InputType.TYPE_CLASS_NUMBER }
        val etInfertile = EditText(ctx).apply { hint = "Jumlah infertil"; inputType = android.text.InputType.TYPE_CLASS_NUMBER }
        val etNotes     = EditText(ctx).apply { hint = "Catatan hasil" }

        layout.addView(android.widget.TextView(ctx).apply { text = "Telur Menetas:" })
        layout.addView(etHatched)
        layout.addView(android.widget.TextView(ctx).apply { text = "Telur Infertil:" })
        layout.addView(etInfertile)
        layout.addView(android.widget.TextView(ctx).apply { text = "Catatan:" })
        layout.addView(etNotes)

        AlertDialog.Builder(ctx)
            .setTitle("Selesaikan Sesi Inkubasi")
            .setView(layout)
            .setPositiveButton("Selesai") { _, _ ->
                val hatched   = etHatched.text.toString().toIntOrNull() ?: 0
                val infertile = etInfertile.text.toString().toIntOrNull() ?: 0
                val notes     = etNotes.text.toString()
                // Finish via API
                val currentId = (viewModel.incubation.value as? ApiResult.Success)?.data?.id ?: return@setPositiveButton
                lifecycleScope.launch {
                    withContext(Dispatchers.IO) {
                        try {
                            ApiClient.getApi(requireContext()).finishIncubation(
                                FinishIncubationRequest(currentId, hatched, infertile, notes)
                            )
                        } catch (e: Exception) { /* ignore */ }
                    }
                    viewModel.fetchIncubation()
                }
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
