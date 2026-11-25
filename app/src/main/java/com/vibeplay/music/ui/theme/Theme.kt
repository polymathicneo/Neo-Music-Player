package com.vibeplay.music.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val VibeDarkColorScheme = darkColorScheme(
    primary = VibeGreen,
    onPrimary = VibeBackground,
    primaryContainer = VibeSurfaceElevated,
    onPrimaryContainer = VibePrimaryText,
    secondary = VibeGreenLight,
    onSecondary = VibeBackground,
    background = VibeBackground,
    onBackground = VibePrimaryText,
    surface = VibeSurface,
    onSurface = VibePrimaryText,
    surfaceVariant = VibeSurfaceElevated,
    onSurfaceVariant = VibeSecondaryText,
    outline = VibeDivider,
    error = VibeError,
    onError = VibePrimaryText
)

@Composable
fun VibePlayTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = VibeDarkColorScheme,
        typography = VibeTypography,
        content = content
    )
}
