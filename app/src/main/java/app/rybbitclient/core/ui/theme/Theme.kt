package app.rybbitclient.core.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalContext
import app.rybbitclient.core.storage.AccentColor

/**
 * Entry point for the Rybbit design system, built on Material 3 Expressive: the expressive motion
 * scheme (springs with overshoot for position/size, critically damped for colour/opacity), the
 * expressive shape scale and emphasised type.
 *
 * - [dynamicColor] uses the wallpaper-derived Material You scheme on Android 12+ (default on).
 * - Otherwise the [accent] decides: Emerald is the hand-tuned brand scheme, the others are generated around a hue.
 * - [pureBlack] drops dark-theme backgrounds to true black for OLED screens.
 * - Semantic analytics colors are provided through [LocalRybbitColors] and read via
 *   `MaterialTheme.rybbitColors`. They follow the fallback palette matching the active luminance.
 */
@Composable
fun RybbitTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    accent: AccentColor = AccentColor.Emerald,
    pureBlack: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colorScheme = rybbitColorScheme(darkTheme, dynamicColor, accent, pureBlack)
    val semanticColors = rybbitColorsFor(isDark = darkTheme)

    CompositionLocalProvider(LocalRybbitColors provides semanticColors) {
        MaterialExpressiveTheme(
            colorScheme = colorScheme,
            motionScheme = MotionScheme.expressive(),
            typography = rybbitTypography(),
            shapes = RybbitShapes,
            content = content,
        )
    }
}

@Composable
private fun rybbitColorScheme(
    darkTheme: Boolean,
    dynamicColor: Boolean,
    accent: AccentColor,
    pureBlack: Boolean,
): ColorScheme {
    val context = LocalContext.current
    val supportsDynamic = dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    val base = when {
        supportsDynamic && darkTheme -> dynamicDarkColorScheme(context)
        supportsDynamic -> dynamicLightColorScheme(context)
        accent == AccentColor.Emerald && darkTheme -> RybbitDarkScheme
        accent == AccentColor.Emerald -> RybbitLightScheme
        else -> accentColorScheme(hue = accent.hue, dark = darkTheme)
    }
    return if (pureBlack && darkTheme) base.withPureBlack() else base
}
