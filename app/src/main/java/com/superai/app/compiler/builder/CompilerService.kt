package com.superai.app.compiler.builder

import android.app.Service
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.superai.app.R
import com.superai.app.SuperAIApplication
import com.superai.app.compiler.script.BuildConfig
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
class CompilerService : Service() {

    @Inject lateinit var orchestrator: CompilerOrchestrator

    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) { stopSelf(); return START_NOT_STICKY }

        val projectName = intent?.getStringExtra(EXTRA_PROJECT_NAME) ?: "SuperAIProject"
        val packageName = intent?.getStringExtra(EXTRA_PACKAGE_NAME) ?: "com.superai.generated"

        startForeground(NOTIF_ID, buildNotification("Starting build…"))

        scope.launch {
            try {
                orchestrator.state.collect { s ->
                    updateNotification("${s.phase} (${s.progress}%)")
                }
            } catch (_: Exception) {}
        }

        scope.launch {
            val config = BuildConfig(projectName = projectName, packageName = packageName)
            val ok = orchestrator.build(config)
            Timber.d("Build finished: ok=$ok")
            stopSelf()
        }

        return START_NOT_STICKY
    }

    private fun buildNotification(text: String) =
        NotificationCompat.Builder(this, SuperAIApplication.CHANNEL_COMPILER)
            .setContentTitle("SuperAI Compiler")
            .setContentText(text)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setOngoing(true)
            .build()

    private fun updateNotification(text: String) {
        val nm = getSystemService(android.app.NotificationManager::class.java)
        nm?.notify(NOTIF_ID, buildNotification(text))
    }

    override fun onDestroy() { super.onDestroy(); scope.cancel() }

    companion object {
        const val ACTION_STOP         = "com.superai.app.STOP_COMPILER"
        const val EXTRA_PROJECT_NAME  = "project_name"
        const val EXTRA_PACKAGE_NAME  = "package_name"
        private const val NOTIF_ID    = 2
    }
}
