package app.rybbitclient.core.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

/** Brand seed color (emerald). Hand-tuned schemes below are derived from this family. */
val RybbitBrandSeed = Color(0xFF10B981)

private val LightPrimary = Color(0xFF047857)
private val LightOnPrimary = Color(0xFFFFFFFF)
private val LightPrimaryContainer = Color(0xFFA7F3D0)
private val LightOnPrimaryContainer = Color(0xFF022C22)
private val LightSecondary = Color(0xFF0F766E)
private val LightOnSecondary = Color(0xFFFFFFFF)
private val LightSecondaryContainer = Color(0xFFCCFBF1)
private val LightOnSecondaryContainer = Color(0xFF042F2E)
private val LightTertiary = Color(0xFF0E7490)
private val LightOnTertiary = Color(0xFFFFFFFF)
private val LightTertiaryContainer = Color(0xFFCFFAFE)
private val LightOnTertiaryContainer = Color(0xFF083344)
private val LightError = Color(0xFFB91C1C)
private val LightOnError = Color(0xFFFFFFFF)
private val LightErrorContainer = Color(0xFFFEE2E2)
private val LightOnErrorContainer = Color(0xFF7F1D1D)
private val LightBackground = Color(0xFFF7FAF8)
private val LightOnBackground = Color(0xFF0F1A16)
private val LightSurface = Color(0xFFFFFFFF)
private val LightOnSurface = Color(0xFF0F1A16)
private val LightSurfaceVariant = Color(0xFFE3EDE8)
private val LightOnSurfaceVariant = Color(0xFF3F4D47)
private val LightOutline = Color(0xFF6E7D76)
private val LightOutlineVariant = Color(0xFFC3D1CB)
private val LightInverseSurface = Color(0xFF243130)
private val LightInverseOnSurface = Color(0xFFE6F2EE)
private val LightInversePrimary = Color(0xFF6EE7B7)

private val DarkPrimary = Color(0xFF34D399)
private val DarkOnPrimary = Color(0xFF00291E)
private val DarkPrimaryContainer = Color(0xFF065F46)
private val DarkOnPrimaryContainer = Color(0xFFA7F3D0)
private val DarkSecondary = Color(0xFF5EEAD4)
private val DarkOnSecondary = Color(0xFF042F2E)
private val DarkSecondaryContainer = Color(0xFF115E59)
private val DarkOnSecondaryContainer = Color(0xFFCCFBF1)
private val DarkTertiary = Color(0xFF67E8F9)
private val DarkOnTertiary = Color(0xFF083344)
private val DarkTertiaryContainer = Color(0xFF155E75)
private val DarkOnTertiaryContainer = Color(0xFFCFFAFE)
private val DarkError = Color(0xFFF87171)
private val DarkOnError = Color(0xFF450A0A)
private val DarkErrorContainer = Color(0xFF7F1D1D)
private val DarkOnErrorContainer = Color(0xFFFEE2E2)
private val DarkBackground = Color(0xFF0C1412)
private val DarkOnBackground = Color(0xFFE3EFEA)
private val DarkSurface = Color(0xFF111C19)
private val DarkOnSurface = Color(0xFFE3EFEA)
private val DarkSurfaceVariant = Color(0xFF2A3834)
private val DarkOnSurfaceVariant = Color(0xFFBCCAC3)
private val DarkOutline = Color(0xFF869590)
private val DarkOutlineVariant = Color(0xFF3B4A45)
private val DarkInverseSurface = Color(0xFFE3EFEA)
private val DarkInverseOnSurface = Color(0xFF243130)
private val DarkInversePrimary = Color(0xFF047857)

/** Fallback light scheme used when dynamic color is unavailable or disabled. */
val RybbitLightScheme: ColorScheme = lightColorScheme(
    primary = LightPrimary,
    onPrimary = LightOnPrimary,
    primaryContainer = LightPrimaryContainer,
    onPrimaryContainer = LightOnPrimaryContainer,
    secondary = LightSecondary,
    onSecondary = LightOnSecondary,
    secondaryContainer = LightSecondaryContainer,
    onSecondaryContainer = LightOnSecondaryContainer,
    tertiary = LightTertiary,
    onTertiary = LightOnTertiary,
    tertiaryContainer = LightTertiaryContainer,
    onTertiaryContainer = LightOnTertiaryContainer,
    error = LightError,
    onError = LightOnError,
    errorContainer = LightErrorContainer,
    onErrorContainer = LightOnErrorContainer,
    background = LightBackground,
    onBackground = LightOnBackground,
    surface = LightSurface,
    onSurface = LightOnSurface,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = LightOnSurfaceVariant,
    outline = LightOutline,
    outlineVariant = LightOutlineVariant,
    inverseSurface = LightInverseSurface,
    inverseOnSurface = LightInverseOnSurface,
    inversePrimary = LightInversePrimary,
)

/** Fallback dark scheme used when dynamic color is unavailable or disabled. */
val RybbitDarkScheme: ColorScheme = darkColorScheme(
    primary = DarkPrimary,
    onPrimary = DarkOnPrimary,
    primaryContainer = DarkPrimaryContainer,
    onPrimaryContainer = DarkOnPrimaryContainer,
    secondary = DarkSecondary,
    onSecondary = DarkOnSecondary,
    secondaryContainer = DarkSecondaryContainer,
    onSecondaryContainer = DarkOnSecondaryContainer,
    tertiary = DarkTertiary,
    onTertiary = DarkOnTertiary,
    tertiaryContainer = DarkTertiaryContainer,
    onTertiaryContainer = DarkOnTertiaryContainer,
    error = DarkError,
    onError = DarkOnError,
    errorContainer = DarkErrorContainer,
    onErrorContainer = DarkOnErrorContainer,
    background = DarkBackground,
    onBackground = DarkOnBackground,
    surface = DarkSurface,
    onSurface = DarkOnSurface,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkOnSurfaceVariant,
    outline = DarkOutline,
    outlineVariant = DarkOutlineVariant,
    inverseSurface = DarkInverseSurface,
    inverseOnSurface = DarkInverseOnSurface,
    inversePrimary = DarkInversePrimary,
)
