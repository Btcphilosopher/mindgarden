package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val SoftDarkColorScheme = darkColorScheme(
    primary = ObsidianPrimary,
    onPrimary = ObsidianBackground,
    secondary = ObsidianSecondary,
    onSecondary = ObsidianTextLight,
    tertiary = ObsidianPrimary,
    background = ObsidianBackground,
    onBackground = ObsidianTextLight,
    surface = ObsidianSurface,
    onSurface = ObsidianTextLight,
    surfaceVariant = ObsidianSurfaceVariant,
    onSurfaceVariant = ObsidianTextLight,
    outline = ObsidianSecondary,
    outlineVariant = ObsidianBorder
)

private val WarmLightColorScheme = lightColorScheme(
    primary = PolishPrimary,
    onPrimary = PolishSurface,
    secondary = PolishSecondary,
    onSecondary = PolishTextDark,
    tertiary = PolishAccent,
    background = PolishBackground,
    onBackground = PolishTextDark,
    surface = PolishSurface,
    onSurface = PolishTextDark,
    surfaceVariant = PolishSurfaceVariant,
    onSurfaceVariant = PolishTextDark,
    outline = PolishSecondary,
    outlineVariant = PolishBorder
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) SoftDarkColorScheme else WarmLightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
