package com.arif.randomcheckin.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

// Central palettes stay private so UI surfaces consume them only through MaterialTheme.
private val DarkColorScheme: ColorScheme = darkColorScheme(
    primary = AccentPurple,
    onPrimary = TextPrimaryNight,
    primaryContainer = AccentPurple,
    onPrimaryContainer = TextPrimaryNight,
    secondary = AccentBlue,
    onSecondary = TextPrimaryNight,
    secondaryContainer = AccentBlue,
    onSecondaryContainer = TextPrimaryNight,
    background = Color(0xFF050607),
    onBackground = Color(0xFFE9EDF2),
    surface = Color(0xFF0B0D10),
    onSurface = Color(0xFFE9EDF2),
    surfaceVariant = Color(0xFF10141A),
    onSurfaceVariant = Color(0xFFB4BCC7),
    outline = Color(0xFF1A202A),
    inverseOnSurface = Color(0xFF0B0D10),
    inverseSurface = Color(0xFFE9EDF2)
)

private val LightOnSurface = Color(0xFF211F35)
private val LightOnSurfaceVariant = Color(0xFF4F4A74)
private val LightOutline = Color(0xFFCAC4D9)

private val LightColorScheme: ColorScheme = lightColorScheme(
    primary = LightPrimary,
    onPrimary = LightOnPrimary,
    primaryContainer = LightPrimaryContainer,
    onPrimaryContainer = LightPrimary,
    secondary = LightSecondary,
    onSecondary = LightOnSecondary,
    secondaryContainer = LightSecondaryContainer,
    onSecondaryContainer = LightSecondary,
    background = LightBackground,
    onBackground = LightOnSurface,
    surface = LightSurface,
    onSurface = LightOnSurface,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = LightOnSurfaceVariant,
    outline = LightOutline
)

/**
 * Wraps MaterialTheme so every screen follows the same palette and typography while respecting
 * system dark mode and (optionally) Android 12+ dynamic color surfaces.
 */
@Composable
fun RandomCheckInTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val supportsDynamicColor = dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    val colorScheme = when {
        supportsDynamicColor -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}