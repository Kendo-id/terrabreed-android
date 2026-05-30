package com.terrabreed.app.utils

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.terrabreed.app.widgets.WidgetUpdateService

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            context.startService(Intent(context, WidgetUpdateService::class.java))
        }
    }
}
