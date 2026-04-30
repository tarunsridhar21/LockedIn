package com.timetrack.app.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val LightColorScheme = lightColorScheme(
    primary = Sage,
    onPrimary = Color.White,
    primaryContainer = SagePale,
    onPrimaryContainer = SageDeep,
    secondary = Lavender,
    onSecondary = Color.White,
    secondaryContainer = LavenderPale,
    onSecondaryContainer = Color(0xFF1E1340),
    tertiary = Clay,
    onTertiary = Color.White,
    tertiaryContainer = ClayPale,
    onTertiaryContainer = ClayDeep,
    error = SoftError,
    onError = Color.White,
    background = LinenBg,
    onBackground = WarmInk,
    surface = CreamSurface,
    onSurface = WarmInk,
    surfaceVariant = CreamContainer,
    onSurfaceVariant = WarmInkMuted,
    surfaceContainer = CreamContainer,
    surfaceContainerHigh = CreamContainerHigh,
    outline = WarmOutline,
    outlineVariant = Color(0xFFEBE5DA),
)

private val DarkColorScheme = darkColorScheme(
    primary = SageLight,
    onPrimary = SageDeep,
    primaryContainer = SageContainerDark,
    onPrimaryContainer = SagePale,
    secondary = LavenderLight,
    onSecondary = Color(0xFF1E1340),
    secondaryContainer = Color(0xFF3D2F5A),
    onSecondaryContainer = LavenderPale,
    tertiary = ClayLight,
    onTertiary = ClayDeep,
    tertiaryContainer = Color(0xFF5A3828),
    onTertiaryContainer = ClayPale,
    error = Color(0xFFE88C7A),
    onError = Color(0xFF4A1512),
    background = MidnightBg,
    onBackground = WarmPaper,
    surface = MidnightSurface,
    onSurface = WarmPaper,
    surfaceVariant = MidnightContainer,
    onSurfaceVariant = WarmPaperMuted,
    surfaceContainer = MidnightContainer,
    surfaceContainerHigh = Color(0xFF3E3A32),
    outline = Color(0xFF5C564E),
    outlineVariant = Color(0xFF3E3A32),
)

@Composable
fun TimeTrackTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit,
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
        typography = AppTypography,
        content = content,
    )
}
