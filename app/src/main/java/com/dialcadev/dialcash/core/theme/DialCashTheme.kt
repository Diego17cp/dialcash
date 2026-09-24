package com.dialcadev.dialcash.core.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = Color(0xFF085691),
    onPrimary = Color(0xFFF2F2F2),
    secondary = Color(0xFFD79B5F),
    onSecondary = Color(0xFF000000),
    background = Color(0xFFFFFFFF),
    surface = Color(0xFFF2F2F2),
    onSurface = Color(0xFF000000),
    onSurfaceVariant = Color(0xFF333333)
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF63A3D7),
    onPrimary = Color(0xFFFFFFFF),
    secondary = Color(0xFFF0C78B),
    onSecondary = Color(0xFFFFFFFF),
    background = Color(0xFF121212),
    surface = Color(0xFF1E1E1E),
    onSurface = Color(0xFFFFFFFF),
    onSurfaceVariant = Color(0xFFCCCCCC)
)

@Composable
fun DialCashTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    typography: Typography = MaterialTheme.typography,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = typography,
        content = content
    )
}