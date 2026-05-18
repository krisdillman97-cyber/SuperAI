package com.superai.app.ui.settings

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.superai.app.agent.core.AgentRepository
import com.superai.app.ui.theme.COLOR_PRESETS
import com.superai.app.ui.theme.ThemeViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

// ─── Safety / Content filter model ───────────────────────────────────────────

data class ContentFilterConfig(
    val safetyLevel: Float = 0.7f,           // 0.0 = OFF, 1.0 = MAXIMUM
    val filterHate: Boolean = true,
    val filterViolence: Boolean = true,
    val filterAdult: Boolean = true,
    val filterIllicit: Boolean = true,
    val filterSelfHarm: Boolean = true,
    val filterPii: Boolean = false,
    val customBlocklist: List<String> = emptyList()
)

// ─── Safety ViewModel ─────────────────────────────────────────────────────────

@HiltViewModel
class SafetyViewModel @Inject constructor(
    private val agentRepo: AgentRepository
) : ViewModel() {

    private val _config = MutableStateFlow(ContentFilterConfig())
    val config: StateFlow<ContentFilterConfig> = _config.asStateFlow()

    fun setSafetyLevel(level: Float) {
        _config.update { it.copy(safetyLevel = level) }
        // Propagate to all active state machines
        viewModelScope.launch {
            agentRepo.getAllProfiles().first().forEach { profile ->
                agentRepo.getMachine(profile.id)
                    ?.transition(
                        com.superai.app.agent.state.AgentEvent.UpdateSafety(level)
                    )
            }
        }
    }

    fun setFilter(name: String, enabled: Boolean) = _config.update {
        when (name) {
            "hate"      -> it.copy(filterHate = enabled)
            "violence"  -> it.copy(filterViolence = enabled)
            "adult"     -> it.copy(filterAdult = enabled)
            "illicit"   -> it.copy(filterIllicit = enabled)
            "self_harm" -> it.copy(filterSelfHarm = enabled)
            "pii"       -> it.copy(filterPii = enabled)
            else        -> it
        }
    }

    /** Turn off ALL filters at once */
    fun disableAllFilters() = _config.update {
        it.copy(
            safetyLevel   = 0f,
            filterHate    = false,
            filterViolence = false,
            filterAdult   = false,
            filterIllicit = false,
            filterSelfHarm = false,
            filterPii     = false
        )
    }

    /** Restore hardened defaults */
    fun enableAllFilters() = _config.update { ContentFilterConfig() }

    fun toAgentInstruction(): String {
        val c = _config.value
        if (c.safetyLevel == 0f) return "[SAFETY: OFF — all content filters disabled]"
        val active = buildList {
            if (c.filterHate)     add("hate speech")
            if (c.filterViolence) add("graphic violence")
            if (c.filterAdult)    add("explicit content")
            if (c.filterIllicit)  add("illicit activity")
            if (c.filterSelfHarm) add("self-harm")
            if (c.filterPii)      add("PII")
        }
        return "[SAFETY: ${safetyLabel(c.safetyLevel)} | block: ${active.joinToString(", ")}]"
    }

    private fun safetyLabel(v: Float) = when {
        v == 0f  -> "OFF"
        v < 0.2f -> "UNRESTRICTED"
        v < 0.4f -> "LOW"
        v < 0.6f -> "MEDIUM"
        v < 0.8f -> "HIGH"
        else     -> "MAXIMUM"
    }
}

// ─── Settings Screen ─────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    themeVm:  ThemeViewModel  = hiltViewModel(),
    safetyVm: SafetyViewModel = hiltViewModel()
) {
    val context      = LocalContext.current
    val themeConfig  by themeVm.themeConfig.collectAsState()
    val safetyConfig by safetyVm.config.collectAsState()
    val prefs        = remember { context.getSharedPreferences("superai_prefs", Context.MODE_PRIVATE) }
    var hudAutoRestart by remember { mutableStateOf(prefs.getBoolean("hud_auto_restart", false)) }
    var showDisableConfirm by remember { mutableStateOf(false) }

    // Permission states (recomposed on resume)
    val hasNotif = if (Build.VERSION.SDK_INT >= 33)
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
    else true
    val canOverlay = Settings.canDrawOverlays(context)
    val hasCamera  = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED

    val notifLauncher  = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {}
    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {}

    Scaffold(
        topBar = { TopAppBar(title = { Text("Settings", fontWeight = FontWeight.Bold) }) }
    ) { padding ->
        Column(
            Modifier.padding(padding).fillMaxSize().verticalScroll(rememberScrollState())
        ) {

            // ════════════════════════════════════════════════════════════════
            // SAFETY & CONTENT FILTERS
            // ════════════════════════════════════════════════════════════════
            SettingSection("Safety & Content Filters") {

                // Master safety level slider
                Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Filled.Shield, null,
                            tint = safetyLevelColor(safetyConfig.safetyLevel),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "Safety Level: ${safetyLevelLabel(safetyConfig.safetyLevel)}",
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(Modifier.weight(1f))
                        // Quick OFF badge
                        if (safetyConfig.safetyLevel == 0f) {
                            Surface(
                                shape  = RoundedCornerShape(50),
                                color  = Color(0xFFD32F2F).copy(0.2f)
                            ) {
                                Text("OFF", modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                    fontSize = 11.sp, color = Color(0xFFFF5252), fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    Spacer(Modifier.height(6.dp))

                    Slider(
                        value         = safetyConfig.safetyLevel,
                        onValueChange = { safetyVm.setSafetyLevel(it) },
                        valueRange    = 0f..1f,
                        steps         = 3,   // 0, 0.25, 0.5, 0.75, 1.0 snaps
                        colors        = SliderDefaults.colors(
                            thumbColor       = safetyLevelColor(safetyConfig.safetyLevel),
                            activeTrackColor = safetyLevelColor(safetyConfig.safetyLevel)
                        )
                    )

                    Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                        Text("OFF", fontSize = 10.sp, color = Color(0xFFFF5252))
                        Text("LOW",    fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurface.copy(0.4f))
                        Text("MEDIUM", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurface.copy(0.4f))
                        Text("HIGH",   fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurface.copy(0.4f))
                        Text("MAX",    fontSize = 10.sp, color = Color(0xFF4CAF50))
                    }

                    Spacer(Modifier.height(10.dp))
                    Text(
                        "Drag to 0 (leftmost) to disable safety checks entirely, " +
                            "or use the individual toggles below.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(0.45f)
                    )
                }

                HorizontalDivider(Modifier.padding(horizontal = 12.dp))

                // Individual filter toggles
                val filters = listOf(
                    Triple("hate",      "Hate Speech",        safetyConfig.filterHate),
                    Triple("violence",  "Graphic Violence",   safetyConfig.filterViolence),
                    Triple("adult",     "Explicit / Adult",   safetyConfig.filterAdult),
                    Triple("illicit",   "Illicit & Criminal", safetyConfig.filterIllicit),
                    Triple("self_harm", "Self-Harm",          safetyConfig.filterSelfHarm),
                    Triple("pii",       "PII / Personal Data",safetyConfig.filterPii),
                )

                filters.forEach { (key, label, checked) ->
                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(label, Modifier.weight(1f), fontSize = 14.sp)
                        Switch(
                            checked         = checked,
                            onCheckedChange = { safetyVm.setFilter(key, it) },
                            colors          = SwitchDefaults.colors(
                                checkedThumbColor   = MaterialTheme.colorScheme.primary,
                                uncheckedThumbColor = Color(0xFFFF5252)
                            )
                        )
                    }
                }

                HorizontalDivider(Modifier.padding(horizontal = 12.dp, vertical = 6.dp))

                // Bulk action buttons
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { showDisableConfirm = true },
                        modifier = Modifier.weight(1f),
                        colors   = ButtonDefaults.outlinedButtonColors(
                            contentColor = Color(0xFFFF5252)
                        ),
                        border = BorderStroke(1.dp, Color(0xFFFF5252).copy(0.5f))
                    ) {
                        Icon(Icons.Filled.NoEncryption, null, Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Disable All", fontSize = 12.sp)
                    }
                    Button(
                        onClick  = { safetyVm.enableAllFilters() },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Filled.Security, null, Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Reset Defaults", fontSize = 12.sp)
                    }
                }
            }

            // ════════════════════════════════════════════════════════════════
            // PERMISSIONS
            // ════════════════════════════════════════════════════════════════
            SettingSection("Permissions") {
                PermissionRow("Draw Over Other Apps", "Required for Floating HUD", canOverlay) {
                    context.startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:${context.packageName}")))
                }
                PermissionRow("Notifications", "Agent alerts (Android 13+)", hasNotif) {
                    if (Build.VERSION.SDK_INT >= 33)
                        notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
                PermissionRow("Camera", "Vision features (optional)", hasCamera) {
                    cameraLauncher.launch(Manifest.permission.CAMERA)
                }
            }

            // ════════════════════════════════════════════════════════════════
            // FLOATING HUD
            // ════════════════════════════════════════════════════════════════
            SettingSection("Floating HUD") {
                Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Auto-restart on boot", fontWeight = FontWeight.Medium)
                        Text("Relaunch HUD after device restart",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(0.5f))
                    }
                    Switch(
                        checked         = hudAutoRestart,
                        onCheckedChange = {
                            hudAutoRestart = it
                            prefs.edit().putBoolean("hud_auto_restart", it).apply()
                        }
                    )
                }
            }

            // ════════════════════════════════════════════════════════════════
            // APPEARANCE
            // ════════════════════════════════════════════════════════════════
            SettingSection("Appearance") {
                Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    Text("Dark Mode", Modifier.weight(1f), fontWeight = FontWeight.Medium)
                    Switch(checked = themeConfig.isDarkMode, onCheckedChange = { themeVm.toggleDark() })
                }
                Text("Accent Color", fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(start = 16.dp, bottom = 8.dp))
                Row(Modifier.padding(horizontal = 16.dp).horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    COLOR_PRESETS.forEach { preset ->
                        val selected = themeConfig.primaryColorArgb == preset.primary
                        Box(
                            Modifier.size(40.dp).clip(CircleShape)
                                .background(Color(preset.primary))
                                .border(
                                    if (selected) BorderStroke(3.dp, Color.White)
                                    else          BorderStroke(0.dp, Color.Transparent),
                                    CircleShape
                                )
                                .clickable { themeVm.setPrimaryColor(preset.primary) }
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))
                Column(Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                    Text("Corner Radius: ${themeConfig.cornerRadius.toInt()}dp",
                        fontWeight = FontWeight.Medium)
                    Slider(value = themeConfig.cornerRadius,
                        onValueChange = { themeVm.setCornerRadius(it) },
                        valueRange = 0f..32f)
                }
                Spacer(Modifier.height(8.dp))
            }

            // ════════════════════════════════════════════════════════════════
            // ABOUT
            // ════════════════════════════════════════════════════════════════
            SettingSection("About") {
                ListItem(
                    headlineContent   = { Text("SuperAI") },
                    supportingContent = { Text("Version 1.0.0 · May 18, 2026") },
                    leadingContent    = { Icon(Icons.Filled.Info, null) }
                )
                ListItem(
                    headlineContent   = { Text("GitHub") },
                    supportingContent = { Text("github.com/YOUR_USERNAME/SuperAI") },
                    leadingContent    = { Icon(Icons.Filled.Code, null) },
                    modifier          = Modifier.clickable {
                        context.startActivity(Intent(Intent.ACTION_VIEW,
                            Uri.parse("https://github.com/YOUR_USERNAME/SuperAI")))
                    }
                )
            }

            Spacer(Modifier.height(40.dp))
        }
    }

    // ── Disable-all confirmation dialog ───────────────────────────────────────
    if (showDisableConfirm) {
        AlertDialog(
            onDismissRequest = { showDisableConfirm = false },
            icon  = { Icon(Icons.Filled.Warning, null, tint = Color(0xFFFF5252)) },
            title = { Text("Disable All Filters?") },
            text  = {
                Text(
                    "This will set Safety Level to OFF and disable every content filter. " +
                        "The agent will process all directives without restriction.\n\n" +
                        "You can re-enable at any time via 'Reset Defaults'."
                )
            },
            confirmButton = {
                Button(
                    onClick = { safetyVm.disableAllFilters(); showDisableConfirm = false },
                    colors  = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F))
                ) { Text("Disable All") }
            },
            dismissButton = {
                TextButton(onClick = { showDisableConfirm = false }) { Text("Cancel") }
            }
        )
    }
}

// ─── Helpers ─────────────────────────────────────────────────────────────────

@Composable
private fun safetyLevelColor(v: Float) = when {
    v == 0f  -> Color(0xFFFF5252)
    v < 0.4f -> Color(0xFFFF9800)
    v < 0.7f -> Color(0xFFFFEB3B)
    else     -> Color(0xFF4CAF50)
}

private fun safetyLevelLabel(v: Float) = when {
    v == 0f  -> "OFF"
    v < 0.2f -> "Unrestricted"
    v < 0.4f -> "Low"
    v < 0.6f -> "Medium"
    v < 0.8f -> "High"
    else     -> "Maximum"
}

@Composable
private fun SettingSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column {
        Text(
            title.uppercase(),
            modifier = Modifier.padding(start = 16.dp, top = 20.dp, bottom = 6.dp),
            style    = MaterialTheme.typography.labelSmall,
            color    = MaterialTheme.colorScheme.primary
        )
        Card(
            Modifier.fillMaxWidth().padding(horizontal = 12.dp),
            shape = RoundedCornerShape(12.dp)
        ) { Column { content() } }
    }
}

@Composable
private fun PermissionRow(label: String, subtitle: String, granted: Boolean, onRequest: () -> Unit) {
    ListItem(
        headlineContent   = { Text(label, fontWeight = FontWeight.Medium) },
        supportingContent = { Text(subtitle, fontSize = 12.sp) },
        trailingContent   = {
            if (granted) Icon(Icons.Filled.CheckCircle, "Granted",
                tint = MaterialTheme.colorScheme.primary)
            else         TextButton(onClick = onRequest) { Text("Grant") }
        }
    )
}
