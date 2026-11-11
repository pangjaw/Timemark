package com.sintelis.timemark.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val TimemarkOrange = Color(0xFFF97316)
val TimemarkGold = Color(0xFFFFC233)
val TimemarkGreen = Color(0xFF10B981)
val TimemarkRed = Color(0xFFF43F5E)
val SurfaceDark = Color(0xFF0F172A)
val CardDark = Color(0xFF1E293B)
val CanvasDark = Color(0xFF020617)

private val AppDarkColorScheme = darkColorScheme(
    primary = TimemarkOrange,
    onPrimary = Color.White,
    secondary = TimemarkGreen,
    onSecondary = Color.White,
    background = CanvasDark,
    onBackground = Color(0xFFF8FAFC),
    surface = SurfaceDark,
    onSurface = Color(0xFFF8FAFC),
    surfaceVariant = CardDark,
    onSurfaceVariant = Color(0xFF94A3B8),
    error = TimemarkRed,
    onError = Color.White
)

@Composable
fun TimemarkTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = AppDarkColorScheme,
        content = content
    )
}
