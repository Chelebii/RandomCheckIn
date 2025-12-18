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

private val DarkColorScheme: ColorScheme = darkColorScheme(
    primary = AccentPurple,
    onPrimary = TextPrimaryNight,
    primaryContainer = AccentPurpleContainer,
    onPrimaryContainer = TextPrimaryNight,
    secondary = AccentBlue,
    onSecondary = TextPrimaryNight,
    secondaryContainer = AccentBlueContainer,
    onSecondaryContainer = TextPrimaryNight,
    background = DeepSpace,
    onBackground = TextPrimaryNight,
    surface = CardOnyx,
    onSurface = TextPrimaryNight,
    surfaceVariant = CardSlate,
    onSurfaceVariant = TextSecondaryNight,
    outline = OutlineNight,
    outlineVariant = OutlineNight,
    inverseOnSurface = NightBlack,
    inverseSurface = TextPrimaryNight
)

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
    onBackground = Color(0xFF211F35),
    surface = LightSurface,
    onSurface = Color(0xFF211F35),
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = Color(0xFF4F4A74),
    outline = Color(0xFFCAC4D9)
)

@Composable
fun RandomCheckInTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
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