package com.example.vidplay.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val AppDarkColorScheme = darkColorScheme(
    primary = Color(0xFF4F8CFF),
    onPrimary = Color.White,
    primaryContainer = Color(0xFF123A7A),
    onPrimaryContainer = Color.White,
    secondary = Color(0xFF6EA8FF),
    onSecondary = Color.White,
    tertiary = Color(0xFF1D4ED8),
    onTertiary = Color.White,
    background = Color(0xFF030712),
    onBackground = Color(0xFFF8FAFC),
    surface = Color(0xFF0B1220),
    onSurface = Color(0xFFF8FAFC),
    surfaceVariant = Color(0xFF111C32),
    onSurfaceVariant = Color(0xFFCBD5E1),
    outline = Color(0xFF31558A),
    outlineVariant = Color(0xFF94A3B8),
    error = Color(0xFFEF4444),
    onError = Color.White
)

private val DiscordColorScheme = AppDarkColorScheme

@Composable
fun VidPlayTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DiscordColorScheme,
        typography = Typography,
        shapes = Shapes,
        content = content
    )
}

@Composable
fun AuthTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DiscordColorScheme,
        typography = Typography,
        shapes = Shapes,
        content = content
    )
}