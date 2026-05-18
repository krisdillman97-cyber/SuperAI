package com.superai.app.ui.theme

import android.content.Context
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

val Context.themeDataStore by preferencesDataStore(name = "superai_theme")

data class ThemeConfig(
    val isDarkMode: Boolean = true,
    val primaryColorArgb: Long = 0xFF6200EEL,
    val secondaryColorArgb: Long = 0xFF03DAC6L,
    val cornerRadius: Float = 12f,
    val fontScale: Float = 1.0f
)

private object ThemeKeys {
    val DARK_MODE       = booleanPreferencesKey("dark_mode")
    val PRIMARY_COLOR   = longPreferencesKey("primary_color")
    val SECONDARY_COLOR = longPreferencesKey("secondary_color")
    val CORNER_RADIUS   = floatPreferencesKey("corner_radius")
    val FONT_SCALE      = floatPreferencesKey("font_scale")
}

@Singleton
class ThemeRepository @Inject constructor(@ApplicationContext private val context: Context) {

    val themeConfig: Flow<ThemeConfig> = context.themeDataStore.data.map { p ->
        ThemeConfig(
            isDarkMode         = p[ThemeKeys.DARK_MODE]       ?: true,
            primaryColorArgb   = p[ThemeKeys.PRIMARY_COLOR]   ?: 0xFF6200EEL,
            secondaryColorArgb = p[ThemeKeys.SECONDARY_COLOR] ?: 0xFF03DAC6L,
            cornerRadius       = p[ThemeKeys.CORNER_RADIUS]   ?: 12f,
            fontScale          = p[ThemeKeys.FONT_SCALE]      ?: 1.0f
        )
    }

    suspend fun saveConfig(config: ThemeConfig) {
        context.themeDataStore.edit { p ->
            p[ThemeKeys.DARK_MODE]       = config.isDarkMode
            p[ThemeKeys.PRIMARY_COLOR]   = config.primaryColorArgb
            p[ThemeKeys.SECONDARY_COLOR] = config.secondaryColorArgb
            p[ThemeKeys.CORNER_RADIUS]   = config.cornerRadius
            p[ThemeKeys.FONT_SCALE]      = config.fontScale
        }
    }
}

@HiltViewModel
class ThemeViewModel @Inject constructor(
    private val repo: ThemeRepository
) : ViewModel() {
    val themeConfig: StateFlow<ThemeConfig> = repo.themeConfig
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ThemeConfig())

    fun updateConfig(c: ThemeConfig) = viewModelScope.launch { repo.saveConfig(c) }
    fun toggleDark() = viewModelScope.launch { repo.saveConfig(themeConfig.value.copy(isDarkMode = !themeConfig.value.isDarkMode)) }
    fun setPrimaryColor(argb: Long) = viewModelScope.launch { repo.saveConfig(themeConfig.value.copy(primaryColorArgb = argb)) }
    fun setCornerRadius(r: Float) = viewModelScope.launch { repo.saveConfig(themeConfig.value.copy(cornerRadius = r)) }
}

@Composable
fun SuperAIThemeEngine(config: ThemeConfig, content: @Composable () -> Unit) {
    val primary   = Color(config.primaryColorArgb)
    val secondary = Color(config.secondaryColorArgb)

    val colorScheme = if (config.isDarkMode) {
        darkColorScheme(
            primary        = primary,
            secondary      = secondary,
            tertiary       = primary.copy(alpha = 0.7f),
            background     = Color(0xFF0D0D0D),
            surface        = Color(0xFF1A1A2E),
            surfaceVariant = Color(0xFF16213E),
            onPrimary      = Color.White,
            onBackground   = Color.White,
            onSurface      = Color.White
        )
    } else {
        lightColorScheme(primary = primary, secondary = secondary)
    }

    MaterialTheme(colorScheme = colorScheme, content = content)
}

data class ColorPreset(val name: String, val primary: Long, val secondary: Long)

val COLOR_PRESETS = listOf(
    ColorPreset("Cyber Purple", 0xFF6200EEL, 0xFF03DAC6L),
    ColorPreset("Neon Green",   0xFF00E676L, 0xFF1DE9B6L),
    ColorPreset("Steel Blue",   0xFF1565C0L, 0xFF42A5F5L),
    ColorPreset("Crimson",      0xFFD32F2FL, 0xFFFF7043L),
    ColorPreset("Solar Gold",   0xFFF9A825L, 0xFFFFCA28L),
    ColorPreset("Matrix",       0xFF00C853L, 0xFF69F0AEL),
    ColorPreset("Plasma Pink",  0xFFE91E63L, 0xFFFF4081L),
    ColorPreset("Deep Space",   0xFF263238L, 0xFF546E7AL)
)
