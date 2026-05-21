package com.superai.app.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.superai.app.agent.profile.AgentProfile
import com.superai.app.ui.theme.COLOR_PRESETS
import com.superai.app.ui.theme.ThemeViewModel

@Composable
fun SettingsScreen(
    profileId: String? = null,
    onBack: () -> Unit = {}
) {
    val themeVm: ThemeViewModel = hiltViewModel()
    val themeConfig by themeVm.themeConfig.collectAsState()
    val settingsVm: SettingsViewModel = hiltViewModel()
    val profile by settingsVm.profile.collectAsState()

    LaunchedEffect(profileId) { profileId?.let { settingsVm.loadProfile(it) } }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
    ) {
        // Top bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, "Back", tint = MaterialTheme.colorScheme.onBackground)
            }
            Text("Settings", style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onBackground,
                fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 8.dp))
        }

        // ── SAFETY & FILTERS ─────────────────────────────────────────────────
        SectionCard(title = "Safety & Illicit Content Filters") {
            profile?.let { p ->
                // Safety filter toggle + level
                FilterControl(
                    label      = "Safety Filter",
                    description = "Block harmful, dangerous, or unsafe content",
                    enabled    = p.safetyEnabled,
                    level      = p.safetyLevel,
                    onToggle   = { settingsVm.updateProfile(p.copy(safetyEnabled = it)) },
                    onLevel    = { settingsVm.updateProfile(p.copy(safetyLevel = it)) }
                )
                Spacer(Modifier.height(12.dp))
                // Illicit content filter toggle + level
                FilterControl(
                    label       = "Illicit Content Filter",
                    description = "Block adult, illegal, or illicit content",
                    enabled     = p.illicitFilterEnabled,
                    level       = p.illicitFilterLevel,
                    onToggle    = { settingsVm.updateProfile(p.copy(illicitFilterEnabled = it)) },
                    onLevel     = { settingsVm.updateProfile(p.copy(illicitFilterLevel = it)) }
                )
                Spacer(Modifier.height(8.dp))
                if (!p.safetyEnabled || !p.illicitFilterEnabled) {
                    Text(
                        text = "⚠ One or more filters are OFF. Use with caution.",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            } ?: Text("No agent profile selected.", color = MaterialTheme.colorScheme.onSurface)
        }

        // ── THEME ─────────────────────────────────────────────────────────────
        SectionCard(title = "Appearance") {
            // Dark mode
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Dark Mode", color = MaterialTheme.colorScheme.onSurface)
                Switch(checked = themeConfig.isDarkMode, onCheckedChange = { themeVm.toggleDark() })
            }
            Spacer(Modifier.height(12.dp))
            // Corner radius
            Text("Corner Radius: ${themeConfig.cornerRadius.toInt()}dp",
                color = MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.bodySmall)
            Slider(
                value = themeConfig.cornerRadius, onValueChange = { themeVm.setCornerRadius(it) },
                valueRange = 0f..32f, steps = 15, modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(12.dp))
            // Color presets
            Text("Color Presets", color = MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(8.dp))
            COLOR_PRESETS.chunked(4).forEach { row ->
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    row.forEach { preset ->
                        val isSelected = themeConfig.primaryColorArgb == preset.primary
                        Button(
                            onClick = { themeVm.setPrimaryColor(preset.primary) },
                            modifier = Modifier.weight(1f).height(36.dp),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(preset.primary),
                                contentColor = Color.White
                            ),
                            border = if (isSelected) ButtonDefaults.outlinedButtonBorder else null
                        ) {
                            Text(preset.name.take(4), style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
                Spacer(Modifier.height(4.dp))
            }
        }

        Spacer(Modifier.height(32.dp))
    }
}

@Composable
fun FilterControl(
    label: String,
    description: String,
    enabled: Boolean,
    level: Float,
    onToggle: (Boolean) -> Unit,
    onLevel: (Float) -> Unit
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(label, color = MaterialTheme.colorScheme.onSurface,
                    style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                Text(description, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    style = MaterialTheme.typography.bodySmall)
            }
            Switch(checked = enabled, onCheckedChange = onToggle)
        }
        if (enabled) {
            val levelLabel = when {
                level <= 0.0f -> "OFF"
                level <= 0.25f -> "Low"
                level <= 0.5f -> "Medium"
                level <= 0.75f -> "High"
                else -> "Maximum"
            }
            Spacer(Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Level: $levelLabel",
                    color = when {
                        level <= 0.0f -> MaterialTheme.colorScheme.error
                        level <= 0.25f -> Color(0xFFFF9800)
                        else -> MaterialTheme.colorScheme.onSurface
                    },
                    style = MaterialTheme.typography.bodySmall)
                Text("${(level * 100).toInt()}%",
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                    style = MaterialTheme.typography.labelSmall)
            }
            Slider(
                value = level,
                onValueChange = onLevel,
                valueRange = 0f..1f,
                steps = 9,
                modifier = Modifier.fillMaxWidth()
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("OFF", style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.error)
                Text("MAX", style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
            }
        }
    }
}

@Composable
fun SectionCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(12.dp))
            content()
        }
    }
}
