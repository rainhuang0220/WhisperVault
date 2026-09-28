package com.whispervault.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val BrandPrimary = Color(0xFF0B3D2E)
private val BrandSecondary = Color(0xFF1A6B4F)
private val BrandSurface = Color(0xFFF4F7F5)
private val BrandOnSurface = Color(0xFF12201A)

private val LightColors = lightColorScheme(
    primary = BrandPrimary,
    onPrimary = Color.White,
    secondary = BrandSecondary,
    onSecondary = Color.White,
    background = BrandSurface,
    onBackground = BrandOnSurface,
    surface = BrandSurface,
    onSurface = BrandOnSurface,
)

@Composable
fun WhisperVaultTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightColors,
        content = content,
    )
}
