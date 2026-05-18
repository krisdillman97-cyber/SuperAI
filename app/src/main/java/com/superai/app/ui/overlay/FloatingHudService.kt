package com.superai.app.ui.overlay

import android.app.*
import android.content.Intent
import android.graphics.*
import android.graphics.drawable.GradientDrawable
import android.os.*
import android.provider.Settings
import android.util.TypedValue
import android.view.*
import android.widget.*
import androidx.core.app.NotificationCompat
import com.superai.app.MainActivity
import com.superai.app.R
import com.superai.app.SuperAIApplication
import com.superai.app.agent.core.AgentRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
class FloatingHudService : Service() {

    @Inject lateinit var agentRepo: AgentRepository

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private lateinit var windowManager: WindowManager

    private var bubbleView: View? = null
    private var hudPanelView: View? = null
    private var isPanelExpanded = false

    private var initialX = 0; private var initialY = 0
    private var initialTouchX = 0f; private var initialTouchY = 0f

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) { stopSelf(); return START_NOT_STICKY }
        startForeground(NOTIFICATION_ID, buildNotification())
        if (Settings.canDrawOverlays(this)) createBubble()
        else { Timber.w("SYSTEM_ALERT_WINDOW not granted"); stopSelf() }
        return START_STICKY
    }

    // ── Bubble ────────────────────────────────────────────────────────────────
    private fun createBubble() {
        val size = dp(64)
        val params = overlayParams(size, size).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 20; y = 300
        }
        val bubble = FrameLayout(this).apply {
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(0xFF6200EE.toInt())
                setStroke(dp(2), 0xFF03DAC6.toInt())
            }
            elevation = dp(8).toFloat()
        }
        TextView(this).apply {
            text = "SA"
            textSize = 16f
            setTextColor(Color.WHITE)
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        }.also { bubble.addView(it) }

        bubble.setOnTouchListener(dragListener(params))
        bubble.setOnClickListener { togglePanel(params) }

        bubbleView = bubble
        runCatching { windowManager.addView(bubble, params) }
            .onFailure { Timber.e(it, "Failed to add bubble") }
    }

    private fun togglePanel(bubbleParams: WindowManager.LayoutParams) {
        if (isPanelExpanded) {
            hudPanelView?.let { runCatching { windowManager.removeView(it) } }
            hudPanelView = null
            isPanelExpanded = false
        } else {
            createPanel(bubbleParams)
            isPanelExpanded = true
        }
    }

    private fun createPanel(bubbleParams: WindowManager.LayoutParams) {
        val panelW = dp(280); val panelH = dp(360)
        val params = overlayParams(panelW, panelH).apply {
            gravity = Gravity.TOP or Gravity.START
            x = (bubbleParams.x - panelW / 2).coerceAtLeast(0)
            y = bubbleParams.y + dp(72)
        }
        val panel = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(12), dp(12), dp(12), dp(12))
            background = GradientDrawable().apply {
                setColor(0xF01A1A2E.toInt())
                cornerRadius = dp(16).toFloat()
                setStroke(dp(1), 0xFF6200EE.toInt())
            }
            elevation = dp(12).toFloat()
        }

        // Title
        TextView(this).apply {
            text = "SuperAI"
            setTextColor(0xFF6200EE.toInt())
            textSize = 18f
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(0, 0, 0, dp(8))
        }.also { panel.addView(it) }

        // Status line
        val statusTv = TextView(this).apply {
            text = "Initialising…"
            setTextColor(Color.WHITE)
            textSize = 12f
            setPadding(0, 0, 0, dp(8))
        }
        panel.addView(statusTv)

        // Observe active agent
        scope.launch {
            agentRepo.getActiveProfile().collect { profile ->
                statusTv.text = profile?.let { "Active: ${it.name}" } ?: "No active agent"
            }
        }

        // Action buttons
        listOf("Dashboard" to { openApp() }, "Stop HUD" to { stopSelf() }).forEach { (label, action) ->
            Button(this).apply {
                text = label
                setTextColor(Color.WHITE)
                background = GradientDrawable().apply {
                    setColor(0xFF3700B3.toInt())
                    cornerRadius = dp(8).toFloat()
                }
                setOnClickListener { action() }
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, dp(48)
                ).apply { setMargins(0, dp(4), 0, dp(4)) }
            }.also { panel.addView(it) }
        }

        hudPanelView = panel
        runCatching { windowManager.addView(panel, params) }
            .onFailure { Timber.e(it, "Failed to add panel") }
    }

    private fun openApp() {
        startActivity(Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
        })
    }

    private fun dragListener(params: WindowManager.LayoutParams) = View.OnTouchListener { v, ev ->
        when (ev.action) {
            MotionEvent.ACTION_DOWN -> {
                initialX = params.x; initialY = params.y
                initialTouchX = ev.rawX; initialTouchY = ev.rawY
                true
            }
            MotionEvent.ACTION_MOVE -> {
                params.x = initialX + (ev.rawX - initialTouchX).toInt()
                params.y = initialY + (ev.rawY - initialTouchY).toInt()
                runCatching { windowManager.updateViewLayout(v, params) }
                true
            }
            else -> false
        }
    }

    private fun buildNotification(): Notification =
        NotificationCompat.Builder(this, SuperAIApplication.CHANNEL_HUD)
            .setContentTitle("SuperAI HUD Active")
            .setContentText("Floating overlay running")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setOngoing(true)
            .setContentIntent(
                PendingIntent.getActivity(
                    this, 0,
                    Intent(this, MainActivity::class.java),
                    PendingIntent.FLAG_IMMUTABLE
                )
            )
            .addAction(0, "Stop", PendingIntent.getService(
                this, 1,
                Intent(this, FloatingHudService::class.java).apply { action = ACTION_STOP },
                PendingIntent.FLAG_IMMUTABLE
            ))
            .build()

    private fun overlayParams(w: Int, h: Int) = WindowManager.LayoutParams(
        w, h,
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        else
            @Suppress("DEPRECATION") WindowManager.LayoutParams.TYPE_PHONE,
        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
        PixelFormat.TRANSLUCENT
    )

    private fun dp(v: Int) = TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_DIP, v.toFloat(), resources.displayMetrics
    ).toInt()

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
        bubbleView?.let { runCatching { windowManager.removeView(it) } }
        hudPanelView?.let { runCatching { windowManager.removeView(it) } }
    }

    companion object {
        const val ACTION_STOP     = "com.superai.app.STOP_HUD"
        private const val NOTIFICATION_ID = 1
    }
}
