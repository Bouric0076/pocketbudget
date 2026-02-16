package com.ics2300.pocketbudget.ui.theme

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

private val DarkColorScheme = darkColorScheme(
    primary = BrandLightGreen,
    secondary = BrandSecondaryGreen,
    tertiary = AnalyticsTeal,
    background = BrandDarkGreen,
    surface = BrandSecondaryGreen,
    onPrimary = BrandDarkGreen,
    onSecondary = BrandLightGreen,
    onBackground = BrandBackgroundGray,
    onSurface = BrandBackgroundGray
)

private val LightColorScheme = lightColorScheme(
    primary = BrandDarkGreen,
    secondary = BrandSecondaryGreen,
    tertiary = AnalyticsTeal,
    background = BrandBackgroundGray,
    surface = AnalyticsSurface,
    onPrimary = AnalyticsSurface,
    onSecondary = AnalyticsSurface,
    onBackground = TextPrimary,
    onSurface = TextPrimary
)

@Composable
fun PocketbudgetTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = false, // Disable dynamic color to enforce brand consistency
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
