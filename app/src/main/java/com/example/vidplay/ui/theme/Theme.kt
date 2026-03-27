package com.example.vidplay.ui.theme

import android.app.Activity
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

private val DarkColorScheme = darkColorScheme(
    primary = Purple80,
    secondary = PurpleGrey80,
    tertiary = Pink80
)

private val LightColorScheme = lightColorScheme(
    primary = Purple40,
    secondary = PurpleGrey40,
    tertiary = Pink40

    /* Other default colors to override
    background = Color(0xFFFFFBFE),
    surface = Color(0xFFFFFBFE),
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = Color(0xFF1C1B1F),
    onSurface = Color(0xFF1C1B1F),
    */
)

// Discord-like dark color scheme for auth screens
private val DiscordColorScheme = darkColorScheme(
    primary = DiscordBlueDark,
    onPrimary = Color.White,
    secondary = DiscordLightBlue,
    onSecondary = Color.White,
    tertiary = DiscordBlue,
    onTertiary = Color.White,
    background = DiscordBlack,
    onBackground = DiscordTextPrimary,
    surface = DiscordDarkGray,
    onSurface = DiscordTextPrimary,
    surfaceVariant = Color(0xFF383B43),
    onSurfaceVariant = DiscordTextInput,
    outline = DiscordBlue,
    outlineVariant = DiscordTextSecondary,
    error = Color(0xFFED4245),
    onError = Color.White
)

@Composable
fun VidPlayTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    // Always use Discord color scheme - ignore system settings
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