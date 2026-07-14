package com.example.presentation.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val AppColorScheme = darkColorScheme(
    primary = AccentPrimary,
    onPrimary = TextOnAccent,
    secondary = BgSecondary,
    onSecondary = TextPrimary,
    background = BgPrimary,
    onBackground = TextPrimary,
    surface = BgSecondary,
    onSurface = TextPrimary,
    surfaceVariant = BgTertiary,
    onSurfaceVariant = TextSecondary,
    error = ErrorColor
)

@Composable
fun AppTheme(
    darkTheme: Boolean = true, // Always dark theme for Sophisticated Dark
    dynamicColor: Boolean = false, // Disable dynamic color to maintain theme
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = AppColorScheme,
        typography = Typography,
        content = content
    )
}
