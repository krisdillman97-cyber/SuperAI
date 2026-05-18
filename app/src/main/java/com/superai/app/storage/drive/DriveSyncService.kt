package com.superai.app.storage.drive

import android.app.Service
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.superai.app.R
import com.superai.app.SuperAIApplication
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
class DriveSyncService : Service() {

    @Inject lateinit var driveRepo: DriveRepository

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> { stopSelf(); return START_NOT_STICKY }
        }
        startForeground(
            NOTIF_ID,
            NotificationCompat.Builder(this, SuperAIApplication.CHANNEL_DRIVE)
                .setContentTitle("SuperAI Drive Sync")
                .setContentText("Syncing agent data…")
                .setSmallIcon(R.mipmap.ic_launcher)
                .setOngoing(true)
                .build()
        )
        scope.launch {
            try {
                Timber.d("Drive sync started")
                // Add sync logic here — e.g. upload agent profiles snapshot
                delay(500)
                Timber.d("Drive sync complete")
            } catch (e: Exception) {
                Timber.e(e, "Drive sync error")
            } finally {
                stopSelf()
            }
        }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
    }

    companion object {
        const val ACTION_STOP = "com.superai.app.STOP_DRIVE_SYNC"
        private const val NOTIF_ID = 3
    }
}
