package com.terrabreed.app.viewmodels

import android.app.Application
import androidx.lifecycle.*
import com.terrabreed.app.api.*
import kotlinx.coroutines.*

class SensorViewModel(application: Application) : AndroidViewModel(application) {

    private val api get() = ApiClient.getApi(getApplication())

    // ── LiveData ──────────────────────────────────────────────
    private val _sensorLatest = MutableLiveData<ApiResult<SensorLatestResponse>>()
    val sensorLatest: LiveData<ApiResult<SensorLatestResponse>> = _sensorLatest

    private val _tempHistory = MutableLiveData<ApiResult<List<SensorHistoryPoint>>>()
    val tempHistory: LiveData<ApiResult<List<SensorHistoryPoint>>> = _tempHistory

    private val _humidHistory = MutableLiveData<ApiResult<List<SensorHistoryPoint>>>()
    val humidHistory: LiveData<ApiResult<List<SensorHistoryPoint>>> = _humidHistory

    private val _stats = MutableLiveData<ApiResult<SensorStats>>()
    val stats: LiveData<ApiResult<SensorStats>> = _stats

    private val _incubation = MutableLiveData<ApiResult<IncubationSession>>()
    val incubation: LiveData<ApiResult<IncubationSession>> = _incubation

    private val _alarms = MutableLiveData<ApiResult<List<AlarmLog>>>()
    val alarms: LiveData<ApiResult<List<AlarmLog>>> = _alarms

    private val _commandResult = MutableLiveData<ApiResult<CommandResponse>>()
    val commandResult: LiveData<ApiResult<CommandResponse>> = _commandResult

    // Polling job
    private var pollingJob: Job? = null
    var pollingIntervalMs: Long = 5000L

    // History window (minutes)
    var historyMinutes: Int = 60

    // ── Polling ───────────────────────────────────────────────
    fun startPolling() {
        pollingJob?.cancel()
        pollingJob = viewModelScope.launch {
            while (isActive) {
                fetchLatest()
                fetchIncubation()
                delay(pollingIntervalMs)
            }
        }
    }

    fun stopPolling() { pollingJob?.cancel() }

    // ── Fetch Latest Sensor ──────────────────────────────────
    fun fetchLatest() {
        viewModelScope.launch {
            try {
                val resp = api.getSensorLatest()
                if (resp.isSuccessful && resp.body() != null) {
                    _sensorLatest.postValue(ApiResult.Success(resp.body()!!))
                } else {
                    _sensorLatest.postValue(ApiResult.Error("Server error ${resp.code()}"))
                }
            } catch (e: Exception) {
                _sensorLatest.postValue(ApiResult.Error(e.message ?: "Network error"))
            }
        }
    }

    // ── Fetch History ─────────────────────────────────────────
    fun fetchTempHistory(minutes: Int = historyMinutes) {
        _tempHistory.value = ApiResult.Loading
        viewModelScope.launch {
            try {
                val resp = api.getSensorHistory(minutes)
                if (resp.isSuccessful) {
                    _tempHistory.postValue(ApiResult.Success(resp.body() ?: emptyList()))
                } else {
                    _tempHistory.postValue(ApiResult.Error("Server error ${resp.code()}"))
                }
            } catch (e: Exception) {
                _tempHistory.postValue(ApiResult.Error(e.message ?: "Network error"))
            }
        }
    }

    fun fetchHumidHistory(minutes: Int = historyMinutes) {
        _humidHistory.value = ApiResult.Loading
        viewModelScope.launch {
            try {
                val resp = api.getSensorHistory(minutes)
                if (resp.isSuccessful) {
                    _humidHistory.postValue(ApiResult.Success(resp.body() ?: emptyList()))
                } else {
                    _humidHistory.postValue(ApiResult.Error("Server error ${resp.code()}"))
                }
            } catch (e: Exception) {
                _humidHistory.postValue(ApiResult.Error(e.message ?: "Network error"))
            }
        }
    }

    // ── Fetch Stats ───────────────────────────────────────────
    fun fetchStats() {
        viewModelScope.launch {
            try {
                val resp = api.getSensorStats()
                if (resp.isSuccessful && resp.body() != null) {
                    _stats.postValue(ApiResult.Success(resp.body()!!))
                }
            } catch (_: Exception) {}
        }
    }

    // ── Fetch Incubation ──────────────────────────────────────
    fun fetchIncubation() {
        viewModelScope.launch {
            try {
                val resp = api.getIncubationCurrent()
                if (resp.isSuccessful && resp.body() != null) {
                    _incubation.postValue(ApiResult.Success(resp.body()!!))
                } else {
                    _incubation.postValue(ApiResult.Error("Tidak ada sesi aktif"))
                }
            } catch (e: Exception) {
                _incubation.postValue(ApiResult.Error(e.message ?: "Network error"))
            }
        }
    }

    // ── Send Command ──────────────────────────────────────────
    fun sendCommand(command: String, value: Any?) {
        viewModelScope.launch {
            try {
                val resp = api.sendCommand(CommandRequest(command, value))
                if (resp.isSuccessful && resp.body() != null) {
                    _commandResult.postValue(ApiResult.Success(resp.body()!!))
                } else {
                    _commandResult.postValue(ApiResult.Error("Gagal mengirim perintah"))
                }
            } catch (e: Exception) {
                _commandResult.postValue(ApiResult.Error(e.message ?: "Network error"))
            }
        }
    }

    // ── Start Incubation ──────────────────────────────────────
    fun startIncubation(species: String, totalDays: Int, totalEggs: Int, notes: String) {
        viewModelScope.launch {
            try {
                api.startIncubation(StartIncubationRequest(species, totalDays, totalEggs, notes))
                fetchIncubation()
            } catch (_: Exception) {}
        }
    }

    // ── Fetch Alarms ──────────────────────────────────────────
    fun fetchAlarms() {
        viewModelScope.launch {
            try {
                val resp = api.getAlarms()
                if (resp.isSuccessful) {
                    _alarms.postValue(ApiResult.Success(resp.body() ?: emptyList()))
                }
            } catch (_: Exception) {}
        }
    }

    override fun onCleared() {
        super.onCleared()
        pollingJob?.cancel()
    }
}
