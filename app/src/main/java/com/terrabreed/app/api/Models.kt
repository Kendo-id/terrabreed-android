package com.terrabreed.app.api

import com.google.gson.annotations.SerializedName

// ══════════════════════════════════════════════════════════
//  SENSOR DATA
// ══════════════════════════════════════════════════════════

data class SensorData(
    @SerializedName("temp")         val temp: Float?        = null,
    @SerializedName("temp_ds1")     val tempDs1: Float?     = null,
    @SerializedName("temp_ds2")     val tempDs2: Float?     = null,
    @SerializedName("temp_sht")     val tempSht: Float?     = null,
    @SerializedName("humidity")     val humidity: Float?    = null,
    @SerializedName("target_temp")  val targetTemp: Float?  = null,
    @SerializedName("target_humid") val targetHumid: Float? = null,
    @SerializedName("heater")       val heater: Boolean?    = null,
    @SerializedName("humidifier")   val humidifier: Boolean? = null,
    @SerializedName("fan")          val fan: Boolean?       = null,
    @SerializedName("auto_mode")    val autoMode: Boolean?  = null,
    @SerializedName("tray_position") val trayPosition: String? = null,
    @SerializedName("tray_tilted")  val trayTilted: Boolean? = null,
    @SerializedName("motor_state")  val motorState: String? = null
)

data class ActuatorStatus(
    @SerializedName("heater")       val heater: Boolean?    = null,
    @SerializedName("humidifier")   val humidifier: Boolean? = null,
    @SerializedName("fan")          val fan: Boolean?       = null,
    @SerializedName("auto_mode")    val autoMode: Boolean?  = null,
    @SerializedName("tray_tilted")  val trayTilted: Boolean? = null,
    @SerializedName("tray_position") val trayPosition: String? = null,
    @SerializedName("motor_state")  val motorState: String? = null,
    @SerializedName("turn_interval_min") val turnIntervalMin: Int? = null,
    @SerializedName("turn_duration_sec") val turnDurationSec: Int? = null
)

data class SensorLatestResponse(
    @SerializedName("sensor") val sensor: SensorData,
    @SerializedName("status") val status: ActuatorStatus
)

data class SensorHistoryPoint(
    @SerializedName("ts")           val ts: Long,
    @SerializedName("temp")         val temp: Float?,
    @SerializedName("humidity")     val humidity: Float?,
    @SerializedName("target_temp")  val targetTemp: Float?,
    @SerializedName("target_humid") val targetHumid: Float?,
    @SerializedName("heater")       val heater: Int?,
    @SerializedName("humidifier")   val humidifier: Int?,
    @SerializedName("fan")          val fan: Int?
)

data class SensorStats(
    @SerializedName("avg_temp")    val avgTemp: Float?,
    @SerializedName("min_temp")    val minTemp: Float?,
    @SerializedName("max_temp")    val maxTemp: Float?,
    @SerializedName("avg_humid")   val avgHumid: Float?,
    @SerializedName("min_humid")   val minHumid: Float?,
    @SerializedName("max_humid")   val maxHumid: Float?,
    @SerializedName("data_points") val dataPoints: Int?
)

// ══════════════════════════════════════════════════════════
//  INCUBATION SESSION
// ══════════════════════════════════════════════════════════

data class IncubationSession(
    @SerializedName("id")           val id: Int,
    @SerializedName("started_at")   val startedAt: Long,
    @SerializedName("ended_at")     val endedAt: Long?,
    @SerializedName("species")      val species: String?,
    @SerializedName("total_days")   val totalDays: Int?,
    @SerializedName("total_eggs")   val totalEggs: Int?,
    @SerializedName("hatched")      val hatched: Int?,
    @SerializedName("infertile")    val infertile: Int?,
    @SerializedName("notes")        val notes: String?,
    @SerializedName("elapsed_days") val elapsedDays: Int?,
    @SerializedName("active")       val active: Boolean?
)

data class StartIncubationRequest(
    @SerializedName("species")    val species: String,
    @SerializedName("total_days") val totalDays: Int,
    @SerializedName("total_eggs") val totalEggs: Int,
    @SerializedName("notes")      val notes: String = ""
)

data class FinishIncubationRequest(
    @SerializedName("id")        val id: Int,
    @SerializedName("hatched")   val hatched: Int,
    @SerializedName("infertile") val infertile: Int,
    @SerializedName("notes")     val notes: String = ""
)

data class IncubationResponse(
    @SerializedName("ok")         val ok: Boolean,
    @SerializedName("id")         val id: Int?,
    @SerializedName("started_at") val startedAt: Long?,
    @SerializedName("species")    val species: String?
)

// ══════════════════════════════════════════════════════════
//  AI CHAT
// ══════════════════════════════════════════════════════════

data class ChatRequest(
    @SerializedName("message")     val message: String,
    @SerializedName("session_ctx") val sessionCtx: String = ""
)

data class ChatCommand(
    @SerializedName("command") val command: String,
    @SerializedName("value")   val value: Any?,
    @SerializedName("ok")      val ok: Boolean
)

data class ChatResponse(
    @SerializedName("reply")              val reply: String,
    @SerializedName("commands_executed") val commandsExecuted: List<ChatCommand>?
)

data class ChatHistoryItem(
    @SerializedName("ts")      val ts: Long,
    @SerializedName("role")    val role: String,
    @SerializedName("content") val content: String
)

// ══════════════════════════════════════════════════════════
//  TTS / STT
// ══════════════════════════════════════════════════════════

data class TtsRequest(
    @SerializedName("text")  val text: String,
    @SerializedName("voice") val voice: String = "id-ID-GadisNeural"
)

data class SttResponse(
    @SerializedName("text")  val text: String?,
    @SerializedName("error") val error: String?
)

// ══════════════════════════════════════════════════════════
//  COMMAND / SETTINGS
// ══════════════════════════════════════════════════════════

data class CommandRequest(
    @SerializedName("command") val command: String,
    @SerializedName("value")   val value: Any?
)

data class CommandResponse(
    @SerializedName("ok")      val ok: Boolean,
    @SerializedName("command") val command: String?,
    @SerializedName("value")   val value: Any?
)

data class SettingsRequest(
    @SerializedName("target_temp")   val targetTemp: Float?   = null,
    @SerializedName("target_humid")  val targetHumid: Float?  = null,
    @SerializedName("turn_interval") val turnInterval: Int?   = null,
    @SerializedName("turn_duration") val turnDuration: Int?   = null,
    @SerializedName("stop_day")      val stopDay: Int?        = null
)

// ══════════════════════════════════════════════════════════
//  ALARM
// ══════════════════════════════════════════════════════════

data class AlarmLog(
    @SerializedName("id")      val id: Int,
    @SerializedName("ts")      val ts: Long,
    @SerializedName("type")    val type: String,
    @SerializedName("message") val message: String?,
    @SerializedName("value")   val value: Float?
)

// ══════════════════════════════════════════════════════════
//  CONFIG
// ══════════════════════════════════════════════════════════

data class ConfigResponse(
    @SerializedName("mqtt_host") val mqttHost: String,
    @SerializedName("mqtt_port") val mqttPort: Int,
    @SerializedName("device_id") val deviceId: String
)

// ══════════════════════════════════════════════════════════
//  UI STATE WRAPPERS
// ══════════════════════════════════════════════════════════

sealed class ApiResult<out T> {
    data class Success<T>(val data: T) : ApiResult<T>()
    data class Error(val message: String, val code: Int? = null) : ApiResult<Nothing>()
    object Loading : ApiResult<Nothing>()
}
