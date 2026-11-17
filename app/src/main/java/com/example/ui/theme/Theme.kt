package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val ScriptHubDarkColorScheme = darkColorScheme(
    primary = NeonCyan,
    onPrimary = DeepSpaceBg,
    primaryContainer = NeonCyanDim,
    onPrimaryContainer = NeonCyan,
    secondary = NeonCrimson,
    onSecondary = TextPrimary,
    secondaryContainer = NeonCrimsonDim,
    onSecondaryContainer = NeonCrimson,
    tertiary = NeonViolet,
    onTertiary = TextPrimary,
    background = DeepSpaceBg,
    onBackground = TextPrimary,
    surface = SurfaceGlass,
    onSurface = TextPrimary,
    surfaceVariant = SurfaceGlassElevated,
    onSurfaceVariant = TextSecondary,
    outline = BorderGlass,
    error = NeonCrimson,
    onError = TextPrimary
)

@Composable
fun ScriptHubTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = ScriptHubDarkColorScheme,
        typography = ScriptHubTypography,
        content = content
    )
}
