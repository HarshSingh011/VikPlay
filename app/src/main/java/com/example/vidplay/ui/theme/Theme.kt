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
import androidx.compose.ui.platform.LocalContext

// Discord-like Dark Theme with Blue/Green accents
private val DiscordDarkColorScheme = darkColorScheme(
    primary = BluePrimary,           // #0088FF - Primary blue
    onPrimary = BlackBg,             // Text on primary
    primaryContainer = DarkerSurface, // Containers
    onPrimaryContainer = BlueAccent,  // Text in containers
    secondary = GreenAccent,          // #00FF88 - Green accent
    onSecondary = BlackBg,            // Text on secondary
    secondaryContainer = DarkerSurface,
    onSecondaryContainer = GreenAccent,
    tertiary = BlueAccent,            // #00D4FF - Cyan blue
    onTertiary = BlackBg,
    tertiaryContainer = DarkerSurface,
    onTertiaryContainer = BlueAccent,
    error = Color(0xFFFF6B6B),
    onError = BlackBg,
    errorContainer = Color(0xFF8B0000),
    onErrorContainer = Color(0xFFFF6B6B),
    background = BlackBg,             // #0F0F0F - Pure black background
    onBackground = TextPrimary,       // #E0E0E0 - Light text
    surface = DarkSurface,            // #1A1A1A - Dark surface
    onSurface = TextPrimary,          // Light text on surface
    surfaceVariant = DarkerSurface,   // #2D2D2D - Darker variant
    onSurfaceVariant = TextSecondary, // Medium gray text
    outline = BorderColor             // #404040 - Dark borders
)

private val DarkColorScheme = darkColorScheme(
    primary = Purple80,
    secondary = PurpleGrey80,
    tertiary = Pink80
)

private val LightColorScheme = lightColorScheme(
    primary = Purple40,
    secondary = PurpleGrey40,
    tertiary = Pink40
)

@Composable
fun VidPlayTheme(
    darkTheme: Boolean = true,  // Always use dark theme
    dynamicColor: Boolean = false, // Disable dynamic colors
    content: @Composable () -> Unit
) {
    // Always use Discord Dark Theme - ignore system theme preference
    val colorScheme = DiscordDarkColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}