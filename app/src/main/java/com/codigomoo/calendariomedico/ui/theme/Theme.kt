package com.codigomoo.calendariomedico.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val LightColors = lightColorScheme(
    primary = Teal40,
    onPrimary = Neutral99,
    primaryContainer = Teal80,
    onPrimaryContainer = Teal10,
    secondary = SlateBlue40,
    onSecondary = Neutral99,
    secondaryContainer = SlateBlue90,
    onSecondaryContainer = SlateBlue20,
    background = Neutral90,
    onBackground = SlateBlue20,
    surface = Neutral95,
    onSurface = SlateBlue20,
    surfaceVariant = SlateBlue90,
    onSurfaceVariant = SlateBlue40,
    error = Red40,
    onError = Neutral99,
    errorContainer = Red90,
    onErrorContainer = Red10,
)

private val DarkColors = darkColorScheme(
    primary = Teal80,
    onPrimary = Teal20,
    primaryContainer = Teal30,
    onPrimaryContainer = Teal90,
    secondary = SlateBlue80,
    onSecondary = SlateBlue20,
    secondaryContainer = SlateBlue40,
    onSecondaryContainer = SlateBlue90,
    background = Neutral10,
    onBackground = SlateBlue90,
    surface = Neutral20,
    onSurface = SlateBlue90,
    surfaceVariant = SlateBlue40,
    onSurfaceVariant = SlateBlue80,
    error = Red80,
    onError = Red10,
    errorContainer = Red40,
    onErrorContainer = Red90,
)

@Composable
fun CalendarioMedicoTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColors
        else -> LightColors
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
