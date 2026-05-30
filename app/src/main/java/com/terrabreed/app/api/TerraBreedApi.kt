package com.terrabreed.app.api

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.*

interface TerraBreedApi {

    // ── Sensor ──────────────────────────────────────────────
    @GET("terrabreed/api/sensor/latest")
    suspend fun getSensorLatest(): Response<SensorLatestResponse>

    @GET("terrabreed/api/sensor/history")
    suspend fun getSensorHistory(
        @Query("minutes") minutes: Int = 60
    ): Response<List<SensorHistoryPoint>>

    @GET("terrabreed/api/sensor/stats")
    suspend fun getSensorStats(): Response<SensorStats>

    // ── Incubation ───────────────────────────────────────────
    @GET("terrabreed/api/incubation/current")
    suspend fun getIncubationCurrent(): Response<IncubationSession>

    @GET("terrabreed/api/incubation/sessions")
    suspend fun getIncubationSessions(
        @Query("limit") limit: Int = 20
    ): Response<List<IncubationSession>>

    @POST("terrabreed/api/incubation/start")
    suspend fun startIncubation(
        @Body request: StartIncubationRequest
    ): Response<IncubationResponse>

    @POST("terrabreed/api/incubation/finish")
    suspend fun finishIncubation(
        @Body request: FinishIncubationRequest
    ): Response<CommandResponse>

    // ── AI Chat ──────────────────────────────────────────────
    @POST("terrabreed/api/chat")
    suspend fun sendChat(
        @Body request: ChatRequest
    ): Response<ChatResponse>

    @GET("terrabreed/api/chat/history")
    suspend fun getChatHistory(
        @Query("limit") limit: Int = 50
    ): Response<List<ChatHistoryItem>>

    @POST("terrabreed/api/chat/clear")
    suspend fun clearChat(): Response<CommandResponse>

    // ── TTS / STT ────────────────────────────────────────────
    @POST("terrabreed/api/tts")
    suspend fun textToSpeech(
        @Body request: TtsRequest
    ): Response<ResponseBody>

    @Multipart
    @POST("terrabreed/api/stt")
    suspend fun speechToText(
        @Part audio: MultipartBody.Part,
        @Part("lang") lang: okhttp3.RequestBody = okhttp3.RequestBody.create("text/plain".toMediaType(), "id")
    ): Response<SttResponse>

    // ── Commands ─────────────────────────────────────────────
    @POST("terrabreed/api/command")
    suspend fun sendCommand(
        @Body request: CommandRequest
    ): Response<CommandResponse>

    @POST("terrabreed/api/settings")
    suspend fun saveSettings(
        @Body request: SettingsRequest
    ): Response<CommandResponse>

    // ── Alarms ───────────────────────────────────────────────
    @GET("terrabreed/api/alarms")
    suspend fun getAlarms(
        @Query("limit") limit: Int = 30
    ): Response<List<AlarmLog>>

    // ── Config ───────────────────────────────────────────────
    @GET("terrabreed/api/config")
    suspend fun getConfig(): Response<ConfigResponse>
}
