package com.superai.app

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber
import javax.inject.Inject

@HiltAndroidApp
class SuperAIApplication : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .setMinimumLoggingLevel(android.util.Log.DEBUG)
            .build()

    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.DEBUG) Timber.plant(Timber.DebugTree())
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = getSystemService(NotificationManager::class.java) ?: return
            nm.createNotificationChannels(
                listOf(
                    NotificationChannel(CHANNEL_HUD, "Floating HUD",
                        NotificationManager.IMPORTANCE_LOW).apply {
                        description = "SuperAI floating overlay — persistent"
                        setShowBadge(false)
                    },
                    NotificationChannel(CHANNEL_COMPILER, "Compiler Engine",
                        NotificationManager.IMPORTANCE_DEFAULT).apply {
                        description = "On-device build engine progress"
                    },
                    NotificationChannel(CHANNEL_DRIVE, "Drive Sync",
                        NotificationManager.IMPORTANCE_LOW).apply {
                        description = "Google Drive synchronisation"
                        setShowBadge(false)
                    },
                    NotificationChannel(CHANNEL_AGENT, "Agent Notifications",
                        NotificationManager.IMPORTANCE_HIGH).apply {
                        description = "Agent alerts and completions"
                    }
                )
            )
        }
    }

    companion object {
        const val CHANNEL_HUD      = "superai_hud"
        const val CHANNEL_COMPILER = "superai_compiler"
        const val CHANNEL_DRIVE    = "superai_drive"
        const val CHANNEL_AGENT    = "superai_agent"
    }
}
