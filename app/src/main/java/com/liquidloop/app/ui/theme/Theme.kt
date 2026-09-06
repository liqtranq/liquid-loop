package com.liquidloop.app.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = LiquidCyan,
    onPrimary = LiquidBackground,
    primaryContainer = LiquidSurfaceVariant,
    onPrimaryContainer = LiquidCyan,

    secondary = LiquidPurpleLight,
    onSecondary = LiquidBackground,
    secondaryContainer = LiquidPurple,
    onSecondaryContainer = LiquidTextPrimary,

    tertiary = LiquidPink,
    background = LiquidBackground,
    onBackground = LiquidTextPrimary,

    surface = LiquidSurface,
    onSurface = LiquidTextPrimary,
    surfaceVariant = LiquidSurfaceVariant,
    onSurfaceVariant = LiquidTextSecondary,

    outline = LiquidCardBorder
)

@Composable
fun LiquidLoopTheme(
    content: @Composable () -> Unit
) {
    val colorScheme = DarkColorScheme
    val view = LocalView.current

    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window ?: return@SideEffect
            window.statusBarColor = LiquidBackground.toArgb()
            window.navigationBarColor = LiquidBackground.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
            WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = false
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}
