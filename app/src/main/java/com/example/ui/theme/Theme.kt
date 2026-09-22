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
    primary = GSaveGreen,
    onPrimary = Color.White,
    secondary = GSaveBlueLight,
    onSecondary = Color.White,
    background = GSaveBackgroundDark,
    surface = GSaveSurfaceDark,
    onBackground = GSaveTextLight,
    onSurface = GSaveTextLight
)

private val LightColorScheme = lightColorScheme(
    primary = GSaveGreen,
    onPrimary = Color.White,
    secondary = GSaveBlue,
    onSecondary = Color.White,
    background = GSaveBackgroundLight,
    surface = GSaveSurfaceLight,
    onBackground = GSaveTextDark,
    onSurface = GSaveTextDark
)

@Composable
fun GSaveTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

// Alias for template compatibility
@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    GSaveTheme(darkTheme = darkTheme, dynamicColor = dynamicColor, content = content)
}
