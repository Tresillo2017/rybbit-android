package app.rybbitclient.core.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalContext

/**
 * Entry point for the Rybbit design system.
 *
 * - [dynamicColor] uses the wallpaper-derived Material You scheme on Android 12+ (default on).
 * - Otherwise falls back to the hand-tuned emerald/teal [RybbitLightScheme] / [RybbitDarkScheme].
 * - Semantic analytics colors are provided through [LocalRybbitColors] and read via
 *   `MaterialTheme.rybbitColors`. They follow the fallback palette matching the active luminance.
 */
@Composable
fun RybbitTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit,
) {
    val colorScheme = rybbitColorScheme(darkTheme = darkTheme, dynamicColor = dynamicColor)
    val semanticColors = rybbitColorsFor(isDark = darkTheme)

    CompositionLocalProvider(LocalRybbitColors provides semanticColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = rybbitTypography(),
            shapes = RybbitShapes,
            content = content,
        )
    }
}

@Composable
private fun rybbitColorScheme(darkTheme: Boolean, dynamicColor: Boolean): ColorScheme {
    val context = LocalContext.current
    val supportsDynamic = dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    return when {
        supportsDynamic && darkTheme -> dynamicDarkColorScheme(context)
        supportsDynamic -> dynamicLightColorScheme(context)
        darkTheme -> RybbitDarkScheme
        else -> RybbitLightScheme
    }
}
