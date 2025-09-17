package com.example.gothere.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val Teal = Color(0xFF4FD1C5)
private val DarkBg = Color(0xFF0F172A)
private val DarkSurface = Color(0xFF111827)
private val LightBg = Color(0xFFF8FAFC)

private val DarkColors = darkColorScheme(
    primary = Teal,
    onPrimary = Color(0xFF00201D),
    secondary = Teal,
    background = DarkBg,
    onBackground = Color(0xFFE5E7EB),
    surface = DarkSurface,
    onSurface = Color(0xFFE5E7EB),
    surfaceVariant = Color(0xFF1F2937),
    onSurfaceVariant = Color(0xFFE5E7EB)
)

private val LightColors = lightColorScheme(
    primary = Teal,
    onPrimary = Color.Black,
    background = LightBg,
    onBackground = Color(0xFF0F172A),
    surface = Color.White,
    onSurface = Color(0xFF0F172A)
)

@Composable
fun GoThereTheme(darkTheme: Boolean = true, content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = Typography(),
        content = content
    )
}
