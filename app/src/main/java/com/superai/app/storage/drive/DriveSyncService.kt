package com.superai.app.storage.drive

import android.app.Service
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.superai.app.R
import com.superai.app.SuperAIApplication
import com.superai.app.agent.core.AgentRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import com.google.gson.Gson
import timber.log.Timber
import java.io.File
import javax.inject.Inject

@AndroidEntryPoint
class DriveSyncService : Service() {

    @Inject lateinit var driveRepo: DriveRepository
    @Inject lateinit var agentRepo: AgentRepository

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
                if (!driveRepo.isSignedIn) {
                    Timber.w("Drive sync skipped — not signed in")
                    stopSelf()
                    return@launch
                }

                // Collect current profiles and serialise to JSON snapshot
                val profiles = agentRepo.getAllProfiles().first()
                if (profiles.isEmpty()) {
                    Timber.d("No agent profiles to sync")
                    stopSelf()
                    return@launch
                }

                val snapshot = profiles.map { p ->
                    mapOf(
                        "id"           to p.id,
                        "name"         to p.name,
                        "instructions" to p.systemInstructions,
                        "isActive"     to p.isActive.toString(),
                        "directives"   to p.totalDirectivesProcessed.toString(),
                        "updatedAt"    to p.updatedAt.toString()
                    )
                }
                val snapshotJson = Gson().toJson(snapshot)

                // Write snapshot to a temp file then upload
                val tmpFile = File(cacheDir, "superai_profiles_snapshot.json")
                tmpFile.writeText(snapshotJson)

                val fileId = driveRepo.uploadFile(tmpFile, "application/json")
                if (fileId != null) {
                    Timber.d("Drive sync complete — uploaded snapshot id=$fileId")
                } else {
                    Timber.w("Drive sync: upload returned null (may already exist or not signed in)")
                }

                tmpFile.delete()
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
