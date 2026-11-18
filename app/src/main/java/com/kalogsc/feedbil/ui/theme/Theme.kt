package com.kalogsc.feedbil.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val OledDarkColorScheme = darkColorScheme(
    primary = OledPrimary,
    onPrimary = OledOnPrimary,
    secondary = OledSecondary,
    onSecondary = OledOnSecondary,
    tertiary = OledTertiary,
    onTertiary = OledOnTertiary,
    background = OledBackground,
    onBackground = OledOnBackground,
    surface = OledSurface,
    onSurface = OledOnSurface,
    surfaceVariant = OledSurfaceVariant,
    onSurfaceVariant = OledOnSurfaceVariant,
    outline = OledOutline
)

@Composable
fun FeedbilTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = OledDarkColorScheme,
        typography = Typography,
        content = content
    )
}
