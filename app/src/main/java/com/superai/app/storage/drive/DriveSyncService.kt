package com.superai.app.storage.drive

import android.app.Service
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.superai.app.R
import com.superai.app.SuperAIApplication
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
class DriveSyncService : Service() {

    @Inject lateinit var driveRepo: DriveRepository

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(NOTIF_ID,
            NotificationCompat.Builder(this, SuperAIApplication.CHANNEL_DRIVE)
                .setContentTitle("SuperAI Drive Sync")
                .setContentText("Syncing agent data…")
                .setSmallIcon(R.mipmap.ic_launcher)
                .setOngoing(true)
                .build()
        )
        val action = intent?.getStringExtra(EXTRA_ACTION) ?: ACTION_SYNC
        scope.launch {
            try {
                when (action) {
                    ACTION_SYNC   -> driveRepo.syncAll()
                    ACTION_UPLOAD -> driveRepo.uploadPendingFiles()
                    ACTION_FETCH  -> driveRepo.fetchLatest()
                }
            } catch (e: Exception) {
                Timber.e(e, "Drive sync error")
            } finally {
                stopSelf()
            }
        }
        return START_NOT_STICKY
    }

    override fun onDestroy() { super.onDestroy(); scope.cancel() }

    companion object {
        const val EXTRA_ACTION = "extra_action"
        const val ACTION_SYNC   = "sync"
        const val ACTION_UPLOAD = "upload"
        const val ACTION_FETCH  = "fetch"
        private const val NOTIF_ID = 3
    }
}
