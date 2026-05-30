package com.terrabreed.app.widgets

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.terrabreed.app.MainActivity
import com.terrabreed.app.R
import com.terrabreed.app.activities.AIVoiceCallActivity
import com.terrabreed.app.api.ApiClient
import kotlinx.coroutines.*
import java.text.SimpleDateFormat
import java.util.*

// ══════════════════════════════════════════════════════════
//  Widget 1 — Temperature
// ══════════════════════════════════════════════════════════
class TemperatureWidget : AppWidgetProvider() {
    override fun onUpdate(ctx: Context, mgr: AppWidgetManager, ids: IntArray) {
        ids.forEach { updateTemperatureWidget(ctx, mgr, it) }
    }
}

fun updateTemperatureWidget(ctx: Context, mgr: AppWidgetManager, widgetId: Int) {
    val views = RemoteViews(ctx.packageName, R.layout.widget_temperature)
    val openIntent = PendingIntent.getActivity(
        ctx, 0, Intent(ctx, MainActivity::class.java),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )
    views.setOnClickPendingIntent(R.id.widget_root, openIntent)

    CoroutineScope(Dispatchers.IO).launch {
        try {
            val resp = ApiClient.getApi(ctx).getSensorLatest()
            if (resp.isSuccessful) {
                val sensor = resp.body()?.sensor
                val temp   = sensor?.temp
                val status = resp.body()?.status
                withContext(Dispatchers.Main) {
                    if (temp != null) {
                        views.setTextViewText(R.id.tv_widget_temp_value, "%.1f°C".format(temp))
                        val targetTemp = sensor.targetTemp ?: 37.5f
                        val diff       = temp - targetTemp
                        val statusText = when {
                            kotlin.math.abs(diff) < 0.3f -> "✓ Normal"
                            diff > 0  -> "▲ Tinggi"
                            else      -> "▼ Rendah"
                        }
                        views.setTextViewText(R.id.tv_widget_temp_status, statusText)
                        views.setTextViewText(R.id.tv_widget_target_temp, "Target: %.1f°C".format(targetTemp))
                        views.setTextViewText(R.id.tv_widget_heater,
                            if (status?.heater == true) "🔥 ON" else "OFF")
                    } else {
                        views.setTextViewText(R.id.tv_widget_temp_value, "--°C")
                        views.setTextViewText(R.id.tv_widget_temp_status, "Offline")
                    }
                    views.setTextViewText(R.id.tv_widget_update_time, timeNow())
                    mgr.updateAppWidget(widgetId, views)
                }
            }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                views.setTextViewText(R.id.tv_widget_temp_value, "--°C")
                views.setTextViewText(R.id.tv_widget_temp_status, "Offline")
                views.setTextViewText(R.id.tv_widget_update_time, timeNow())
                mgr.updateAppWidget(widgetId, views)
            }
        }
    }
}

// ══════════════════════════════════════════════════════════
//  Widget 2 — Humidity
// ══════════════════════════════════════════════════════════
class HumidityWidget : AppWidgetProvider() {
    override fun onUpdate(ctx: Context, mgr: AppWidgetManager, ids: IntArray) {
        ids.forEach { updateHumidityWidget(ctx, mgr, it) }
    }
}

fun updateHumidityWidget(ctx: Context, mgr: AppWidgetManager, widgetId: Int) {
    val views = RemoteViews(ctx.packageName, R.layout.widget_humidity)
    val openIntent = PendingIntent.getActivity(
        ctx, 1, Intent(ctx, MainActivity::class.java),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )
    views.setOnClickPendingIntent(R.id.widget_root, openIntent)

    CoroutineScope(Dispatchers.IO).launch {
        try {
            val resp = ApiClient.getApi(ctx).getSensorLatest()
            if (resp.isSuccessful) {
                val sensor = resp.body()?.sensor
                val humid  = sensor?.humidity
                val status = resp.body()?.status
                withContext(Dispatchers.Main) {
                    if (humid != null) {
                        views.setTextViewText(R.id.tv_widget_humid_value, "%.1f%%".format(humid))
                        val targetHumid = sensor.targetHumid ?: 60f
                        val diff = humid - targetHumid
                        val statusText = when {
                            kotlin.math.abs(diff) < 2f -> "✓ Normal"
                            diff > 0 -> "▲ Tinggi"
                            else     -> "▼ Rendah"
                        }
                        views.setTextViewText(R.id.tv_widget_humid_status, statusText)
                        views.setTextViewText(R.id.tv_widget_target_humid, "Target: %.0f%%".format(targetHumid))
                        views.setTextViewText(R.id.tv_widget_humidifier,
                            if (status?.humidifier == true) "💧 ON" else "OFF")
                    } else {
                        views.setTextViewText(R.id.tv_widget_humid_value, "--%")
                        views.setTextViewText(R.id.tv_widget_humid_status, "Offline")
                    }
                    views.setTextViewText(R.id.tv_widget_update_time, timeNow())
                    mgr.updateAppWidget(widgetId, views)
                }
            }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                views.setTextViewText(R.id.tv_widget_humid_value, "--%")
                views.setTextViewText(R.id.tv_widget_humid_status, "Offline")
                views.setTextViewText(R.id.tv_widget_update_time, timeNow())
                mgr.updateAppWidget(widgetId, views)
            }
        }
    }
}

// ══════════════════════════════════════════════════════════
//  Widget 3 — Incubation Status
// ══════════════════════════════════════════════════════════
class IncubationWidget : AppWidgetProvider() {
    override fun onUpdate(ctx: Context, mgr: AppWidgetManager, ids: IntArray) {
        ids.forEach { updateIncubationWidget(ctx, mgr, it) }
    }
}

fun updateIncubationWidget(ctx: Context, mgr: AppWidgetManager, widgetId: Int) {
    val views = RemoteViews(ctx.packageName, R.layout.widget_incubation)
    val openIntent = PendingIntent.getActivity(
        ctx, 2, Intent(ctx, MainActivity::class.java),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )
    views.setOnClickPendingIntent(R.id.widget_root, openIntent)

    CoroutineScope(Dispatchers.IO).launch {
        try {
            val resp = ApiClient.getApi(ctx).getIncubationCurrent()
            withContext(Dispatchers.Main) {
                if (resp.isSuccessful && resp.body()?.active == true) {
                    val s = resp.body()!!
                    val species = s.species?.replaceFirstChar { it.titlecase() } ?: "-"
                    val elapsed  = s.elapsedDays ?: 0
                    val total    = s.totalDays ?: 1
                    val progress = (elapsed * 100 / total.coerceAtLeast(1))
                    val remaining = (total - elapsed).coerceAtLeast(0)

                    views.setTextViewText(R.id.tv_widget_species, "🥚 $species")
                    views.setTextViewText(R.id.tv_widget_day_count, "Hari ke-$elapsed / $total")
                    views.setTextViewText(R.id.tv_widget_remaining, "$remaining hari lagi")
                    views.setTextViewText(R.id.tv_widget_progress, "$progress%")
                    views.setProgressBar(R.id.pb_widget_incubation, 100, progress, false)
                    views.setTextViewText(R.id.tv_widget_eggs,
                        "${s.totalEggs ?: 0} butir telur")
                } else {
                    views.setTextViewText(R.id.tv_widget_species, "Tidak ada sesi aktif")
                    views.setTextViewText(R.id.tv_widget_day_count, "--")
                    views.setTextViewText(R.id.tv_widget_remaining, "--")
                    views.setTextViewText(R.id.tv_widget_progress, "--")
                    views.setProgressBar(R.id.pb_widget_incubation, 100, 0, false)
                    views.setTextViewText(R.id.tv_widget_eggs, "")
                }
                views.setTextViewText(R.id.tv_widget_update_time, timeNow())
                mgr.updateAppWidget(widgetId, views)
            }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                views.setTextViewText(R.id.tv_widget_species, "Offline")
                views.setTextViewText(R.id.tv_widget_update_time, timeNow())
                mgr.updateAppWidget(widgetId, views)
            }
        }
    }
}

// ══════════════════════════════════════════════════════════
//  Widget 4 — AI Voice Call
// ══════════════════════════════════════════════════════════
class AIVoiceWidget : AppWidgetProvider() {
    override fun onUpdate(ctx: Context, mgr: AppWidgetManager, ids: IntArray) {
        ids.forEach { updateAIVoiceWidget(ctx, mgr, it) }
    }
    override fun onReceive(ctx: Context, intent: Intent) {
        super.onReceive(ctx, intent)
        if (intent.action == "com.terrabreed.app.ACTION_VOICE_CALL") {
            ctx.startActivity(Intent(ctx, AIVoiceCallActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            })
        }
    }
}

fun updateAIVoiceWidget(ctx: Context, mgr: AppWidgetManager, widgetId: Int) {
    val views = RemoteViews(ctx.packageName, R.layout.widget_ai_voice)

    val voiceIntent = PendingIntent.getBroadcast(
        ctx, widgetId,
        Intent(ctx, AIVoiceWidget::class.java).apply {
            action = "com.terrabreed.app.ACTION_VOICE_CALL"
        },
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )
    views.setOnClickPendingIntent(R.id.btn_widget_voice, voiceIntent)

    val openIntent = PendingIntent.getActivity(
        ctx, 3, Intent(ctx, MainActivity::class.java),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )
    views.setOnClickPendingIntent(R.id.widget_root, openIntent)
    views.setTextViewText(R.id.tv_widget_update_time, timeNow())
    mgr.updateAppWidget(widgetId, views)
}

// ══════════════════════════════════════════════════════════
//  Widget Update Service (WorkManager periodic)
// ══════════════════════════════════════════════════════════
class WidgetUpdateService : android.app.Service() {
    override fun onBind(intent: Intent?) = null
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val ctx = applicationContext
        val mgr = AppWidgetManager.getInstance(ctx)
        val tempIds  = mgr.getAppWidgetIds(android.content.ComponentName(ctx, TemperatureWidget::class.java))
        val humidIds = mgr.getAppWidgetIds(android.content.ComponentName(ctx, HumidityWidget::class.java))
        val incu8Ids = mgr.getAppWidgetIds(android.content.ComponentName(ctx, IncubationWidget::class.java))
        val voiceIds = mgr.getAppWidgetIds(android.content.ComponentName(ctx, AIVoiceWidget::class.java))

        tempIds.forEach  { updateTemperatureWidget(ctx, mgr, it) }
        humidIds.forEach { updateHumidityWidget(ctx, mgr, it) }
        incu8Ids.forEach { updateIncubationWidget(ctx, mgr, it) }
        voiceIds.forEach { updateAIVoiceWidget(ctx, mgr, it) }

        stopSelf()
        return START_NOT_STICKY
    }
}

// Helper
private fun timeNow(): String =
    SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
