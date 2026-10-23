package com.example.ui.theme

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
    primary = TabloraSky,
    onPrimary = Color.White,
    primaryContainer = TabloraNavy,
    onPrimaryContainer = TabloraSkyLight,
    secondary = TabloraTeal,
    onSecondary = Color.White,
    background = TabloraDarkBackground,
    onBackground = Color(0xFFF1F5F9),
    surface = TabloraDarkSurface,
    onSurface = Color(0xFFF1F5F9),
    surfaceVariant = TabloraDarkSurfaceVariant,
    onSurfaceVariant = Color(0xFF94A3B8),
    outline = TabloraBorderDark
)

private val LightColorScheme = lightColorScheme(
    primary = TabloraSky,
    onPrimary = Color.White,
    primaryContainer = TabloraSkyLight,
    onPrimaryContainer = TabloraNavy,
    secondary = TabloraTeal,
    onSecondary = Color.White,
    background = TabloraBackground,
    onBackground = TabloraNavy,
    surface = TabloraSurface,
    onSurface = TabloraNavy,
    surfaceVariant = TabloraSurfaceVariant,
    onSurfaceVariant = TabloraSlateLight,
    outline = TabloraBorder
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Keep consistent B2B SaaS branding
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
