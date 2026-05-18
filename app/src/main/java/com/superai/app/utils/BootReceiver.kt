package com.superai.app.utils

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import com.superai.app.ui.overlay.FloatingHudService
import timber.log.Timber

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        if (action !in listOf(
                Intent.ACTION_BOOT_COMPLETED,
                "android.intent.action.QUICKBOOT_POWERON",
                Intent.ACTION_MY_PACKAGE_REPLACED
            )
        ) return

        Timber.d("BootReceiver fired: $action")

        val prefs: SharedPreferences =
            context.getSharedPreferences("superai_prefs", Context.MODE_PRIVATE)

        if (prefs.getBoolean("hud_auto_restart", false)) {
            Timber.d("Auto-restarting FloatingHudService after boot")
            runCatching {
                context.startForegroundService(
                    Intent(context, FloatingHudService::class.java)
                )
            }.onFailure { e -> Timber.e(e, "Failed to start HUD on boot") }
        }
    }
}
