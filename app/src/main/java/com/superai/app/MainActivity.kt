package com.superai.app

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.hilt.navigation.compose.hiltViewModel
import com.superai.app.ui.SuperAINavHost
import com.superai.app.ui.theme.SuperAIThemeEngine
import com.superai.app.ui.theme.ThemeViewModel
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val notifPermLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> Timber.d("POST_NOTIFICATIONS granted=$granted") }

    private val multiPermLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results -> results.forEach { (p, g) -> Timber.d("Permission $p granted=$g") } }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        requestRuntimePermissions()

        setContent {
            val themeViewModel: ThemeViewModel = hiltViewModel()
            val themeConfig by themeViewModel.themeConfig.collectAsState()
            SuperAIThemeEngine(config = themeConfig) {
                SuperAINavHost()
            }
        }
    }

    private fun requestRuntimePermissions() {
        val needed = mutableListOf<String>()

        // Android 13+ notification permission
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (!hasPermission(Manifest.permission.POST_NOTIFICATIONS))
                needed += Manifest.permission.POST_NOTIFICATIONS
            if (!hasPermission(Manifest.permission.READ_MEDIA_IMAGES))
                needed += Manifest.permission.READ_MEDIA_IMAGES
            if (!hasPermission(Manifest.permission.READ_MEDIA_VIDEO))
                needed += Manifest.permission.READ_MEDIA_VIDEO
        }

        // Android 14+ partial media
        if (Build.VERSION.SDK_INT >= 34) {
            if (!hasPermission(Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED))
                needed += Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED
        }

        // Bluetooth (API 31+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (!hasPermission(Manifest.permission.BLUETOOTH_CONNECT))
                needed += Manifest.permission.BLUETOOTH_CONNECT
        }

        if (needed.isNotEmpty()) {
            multiPermLauncher.launch(needed.toTypedArray())
        }
    }

    private fun hasPermission(p: String) =
        ContextCompat.checkSelfPermission(this, p) == PackageManager.PERMISSION_GRANTED
}
