package com.dsmod.probe.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/** YunSeek accent: one blue, used for the status card, switches, selection and links only. */
val YunBlue = Color(0xFF3478F6)
private val YunBlueDark = Color(0xFF4C8DFF)

/*
 * Clean white + single blue palette (no wallpaper/Monet colours, no gradients).
 * Page background is a very light grey so white group cards read as cards without borders.
 */
private val LightColors = lightColorScheme(
    primary = YunBlue,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE8F0FE),
    onPrimaryContainer = Color(0xFF0B3A8C),
    secondary = Color(0xFF6B7078),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFF1F2F4),
    onSecondaryContainer = Color(0xFF1A1C1F),
    tertiary = YunBlue,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFE8F0FE),
    onTertiaryContainer = Color(0xFF0B3A8C),
    error = Color(0xFFD93025),
    errorContainer = Color(0xFFFCE8E6),
    onErrorContainer = Color(0xFF5F0B05),
    background = Color(0xFFF5F6F8),
    onBackground = Color(0xFF111214),
    surface = Color(0xFFF5F6F8),
    onSurface = Color(0xFF111214),
    surfaceVariant = Color(0xFFF1F2F4),
    onSurfaceVariant = Color(0xFF7A7F87),
    outline = Color(0xFFD5D8DD),
    outlineVariant = Color(0xFFE6E8EB),
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color.White,
    surfaceContainer = Color.White,
    surfaceContainerHigh = Color(0xFFF1F2F4),
    surfaceContainerHighest = Color(0xFFE9EBEE),
)

private val DarkColors = darkColorScheme(
    primary = YunBlueDark,
    onPrimary = Color.White,
    primaryContainer = Color(0xFF1B2B45),
    onPrimaryContainer = Color(0xFFD6E4FF),
    secondary = Color(0xFF9AA0A8),
    onSecondary = Color(0xFF111214),
    secondaryContainer = Color(0xFF26282C),
    onSecondaryContainer = Color(0xFFE6E7EA),
    tertiary = YunBlueDark,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFF1B2B45),
    onTertiaryContainer = Color(0xFFD6E4FF),
    error = Color(0xFFFF6B60),
    errorContainer = Color(0xFF3A1C1A),
    onErrorContainer = Color(0xFFFFDAD6),
    background = Color(0xFF0E0F11),
    onBackground = Color(0xFFECEDEF),
    surface = Color(0xFF0E0F11),
    onSurface = Color(0xFFECEDEF),
    surfaceVariant = Color(0xFF1C1D20),
    onSurfaceVariant = Color(0xFF8E939B),
    outline = Color(0xFF3A3D42),
    outlineVariant = Color(0xFF2A2C30),
    surfaceContainerLowest = Color(0xFF1C1D20),
    surfaceContainerLow = Color(0xFF1C1D20),
    surfaceContainer = Color(0xFF1C1D20),
    surfaceContainerHigh = Color(0xFF26282C),
    surfaceContainerHighest = Color(0xFF303236),
)

/** YunSeek launcher theme: fixed white/blue palette, plain Material 3 (no expressive motion). */
@Composable
fun YunSeekTheme(content: @Composable () -> Unit) {
    val dark = isSystemInDarkTheme()
    MaterialTheme(
        colorScheme = if (dark) DarkColors else LightColors,
        content = content,
    )
}

/** Kept for source compatibility with older call sites. */
@Composable
fun DeekseepTheme(content: @Composable () -> Unit) = YunSeekTheme(content)
