package com.dsmod.probe.ui

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

/* Fallback (pre-Android 12) palette: a calm blue/teal seed in the spirit of Monet output. */
private val LightColors = lightColorScheme(
    primary = Color(0xFF2F5FA8),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD7E3FF),
    onPrimaryContainer = Color(0xFF001B3F),
    secondary = Color(0xFF555F71),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFD9E3F8),
    onSecondaryContainer = Color(0xFF121C2B),
    tertiary = Color(0xFF2A6A64),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFB0EEE6),
    onTertiaryContainer = Color(0xFF00201D),
    error = Color(0xFFBA1A1A),
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
    background = Color(0xFFF9F9FF),
    surface = Color(0xFFF9F9FF),
    surfaceVariant = Color(0xFFE0E2EC),
    onSurfaceVariant = Color(0xFF44474E),
    outlineVariant = Color(0xFFC4C6D0),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF3F3FA),
    surfaceContainer = Color(0xFFEDEDF4),
    surfaceContainerHigh = Color(0xFFE7E8EE),
    surfaceContainerHighest = Color(0xFFE2E2E9),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFABC7FF),
    onPrimary = Color(0xFF002F65),
    primaryContainer = Color(0xFF11468E),
    onPrimaryContainer = Color(0xFFD7E3FF),
    secondary = Color(0xFFBDC7DC),
    onSecondary = Color(0xFF273141),
    secondaryContainer = Color(0xFF3E4758),
    onSecondaryContainer = Color(0xFFD9E3F8),
    tertiary = Color(0xFF94D1CA),
    onTertiary = Color(0xFF003733),
    tertiaryContainer = Color(0xFF07504B),
    onTertiaryContainer = Color(0xFFB0EEE6),
    error = Color(0xFFFFB4AB),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    background = Color(0xFF111318),
    surface = Color(0xFF111318),
    surfaceVariant = Color(0xFF44474E),
    onSurfaceVariant = Color(0xFFC4C6D0),
    outlineVariant = Color(0xFF44474E),
    surfaceContainerLowest = Color(0xFF0C0E13),
    surfaceContainerLow = Color(0xFF191C20),
    surfaceContainer = Color(0xFF1D2024),
    surfaceContainerHigh = Color(0xFF282A2F),
    surfaceContainerHighest = Color(0xFF33353A),
)

/**
 * YunSeek launcher theme: Material 3 with Monet dynamic colour on Android 12+ and a static
 * fallback scheme below that. Any failure while reading wallpaper colours (some OEM ROMs throw)
 * falls back to the static scheme instead of crashing the launcher.
 */
@Composable
fun YunSeekTheme(content: @Composable () -> Unit) {
    val context = LocalContext.current
    val dark = isSystemInDarkTheme()
    val colors: ColorScheme = remember(dark, context) {
        val dynamic = if (Build.VERSION.SDK_INT >= 31) {
            runCatching {
                if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
            }.getOrNull()
        } else null
        dynamic ?: if (dark) DarkColors else LightColors
    }
    MaterialExpressiveTheme(
        colorScheme = colors,
        motionScheme = MotionScheme.expressive(),
        content = content,
    )
}

/** Kept for source compatibility with older call sites. */
@Composable
fun DeekseepTheme(content: @Composable () -> Unit) = YunSeekTheme(content)
