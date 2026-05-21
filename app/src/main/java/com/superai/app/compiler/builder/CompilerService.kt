package com.superai.app.compiler.builder

import android.app.Service
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.superai.app.R
import com.superai.app.SuperAIApplication
import com.superai.app.compiler.script.BuildConfig
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class CompilerService : Service() {

    @Inject
    lateinit var orchestrator: CompilerOrchestrator

    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val projectName = intent?.getStringExtra(EXTRA_PROJECT_NAME) ?: "SuperAIProject"
        val packageName = intent?.getStringExtra(EXTRA_PACKAGE_NAME) ?: "com.superai.generated"
        startForeground(NOTIF_ID,
            NotificationCompat.Builder(this, SuperAIApplication.CHANNEL_COMPILER)
                .setContentTitle("SuperAI Compiler")
                .setContentText("Building $projectName…")
                .setSmallIcon(R.mipmap.ic_launcher)
                .setOngoing(true)
                .build()
        )
        scope.launch {
            orchestrator.build(BuildConfig(projectName = projectName, packageName = packageName))
            stopSelf()
        }
        return START_NOT_STICKY
    }

    override fun onDestroy() { super.onDestroy(); scope.cancel() }

    companion object {
        const val EXTRA_PROJECT_NAME = "extra_project_name"
        const val EXTRA_PACKAGE_NAME = "extra_package_name"
        private const val NOTIF_ID = 2
    }
}
