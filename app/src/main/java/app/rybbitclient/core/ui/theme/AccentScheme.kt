package app.rybbitclient.core.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import app.rybbitclient.core.storage.AccentColor
import kotlin.math.abs

private const val FULL_TURN = 360f
private const val SECONDARY_HUE_SHIFT = 15f
private const val TERTIARY_HUE_SHIFT = 60f

/** HSL (hue in degrees, saturation and lightness 0..1) to an opaque [Color]. Pure Kotlin so it runs in JVM tests. */
internal fun hsl(hue: Float, saturation: Float, lightness: Float): Color {
    val normalizedHue = ((hue % FULL_TURN) + FULL_TURN) % FULL_TURN
    val chroma = (1f - abs(2f * lightness - 1f)) * saturation
    val sector = normalizedHue / 60f
    val x = chroma * (1f - abs(sector % 2f - 1f))
    val (r, g, b) = when {
        sector < 1f -> Triple(chroma, x, 0f)
        sector < 2f -> Triple(x, chroma, 0f)
        sector < 3f -> Triple(0f, chroma, x)
        sector < 4f -> Triple(0f, x, chroma)
        sector < 5f -> Triple(x, 0f, chroma)
        else -> Triple(chroma, 0f, x)
    }
    val offset = lightness - chroma / 2f
    return Color(r + offset, g + offset, b + offset)
}

/**
 * Surface and outline neutrals tinted toward [hue]. Material's defaults leave the `surfaceContainer*` roles at the
 * baseline purple tint, which clashes with any non-purple accent, so every scheme gets this stack explicitly.
 */
private data class SurfaceStack(
    val background: Color,
    val surface: Color,
    val surfaceDim: Color,
    val surfaceBright: Color,
    val containerLowest: Color,
    val containerLow: Color,
    val container: Color,
    val containerHigh: Color,
    val containerHighest: Color,
)

private fun surfaceStack(hue: Float, dark: Boolean): SurfaceStack = if (dark) {
    SurfaceStack(
        background = hsl(hue, 0.15f, 0.07f),
        surface = hsl(hue, 0.15f, 0.07f),
        surfaceDim = hsl(hue, 0.15f, 0.07f),
        surfaceBright = hsl(hue, 0.12f, 0.22f),
        containerLowest = hsl(hue, 0.15f, 0.05f),
        containerLow = hsl(hue, 0.15f, 0.095f),
        container = hsl(hue, 0.14f, 0.115f),
        containerHigh = hsl(hue, 0.13f, 0.14f),
        containerHighest = hsl(hue, 0.12f, 0.17f),
    )
} else {
    SurfaceStack(
        background = hsl(hue, 0.25f, 0.975f),
        surface = hsl(hue, 0.25f, 0.98f),
        surfaceDim = hsl(hue, 0.15f, 0.87f),
        surfaceBright = hsl(hue, 0.25f, 0.98f),
        containerLowest = Color.White,
        containerLow = hsl(hue, 0.22f, 0.955f),
        container = hsl(hue, 0.2f, 0.94f),
        containerHigh = hsl(hue, 0.18f, 0.925f),
        containerHighest = hsl(hue, 0.16f, 0.91f),
    )
}

/** Applies the tinted surface stack for [hue] to an existing scheme (used for the hand-tuned brand scheme). */
fun ColorScheme.withTintedSurfaces(hue: Float, dark: Boolean): ColorScheme {
    val stack = surfaceStack(hue, dark)
    return copy(
        background = stack.background,
        surface = stack.surface,
        surfaceDim = stack.surfaceDim,
        surfaceBright = stack.surfaceBright,
        surfaceContainerLowest = stack.containerLowest,
        surfaceContainerLow = stack.containerLow,
        surfaceContainer = stack.container,
        surfaceContainerHigh = stack.containerHigh,
        surfaceContainerHighest = stack.containerHighest,
    )
}

/**
 * Near-black backgrounds for OLED screens. The lowest surface roles go to true black; raised containers climb
 * in small steps so cards stay distinguishable from the background.
 */
fun ColorScheme.withPureBlack(): ColorScheme = copy(
    background = Color.Black,
    surface = Color.Black,
    surfaceDim = Color.Black,
    surfaceContainerLowest = Color.Black,
    surfaceContainerLow = hsl(0f, 0f, 0.07f),
    surfaceContainer = hsl(0f, 0f, 0.09f),
    surfaceContainerHigh = hsl(0f, 0f, 0.12f),
    surfaceContainerHighest = hsl(0f, 0f, 0.15f),
)

/** A complete light or dark scheme built around [hue]: primary, secondary and tertiary families plus neutrals. */
fun accentColorScheme(hue: Float, dark: Boolean): ColorScheme {
    val secondaryHue = hue + SECONDARY_HUE_SHIFT
    val tertiaryHue = hue + TERTIARY_HUE_SHIFT
    val stack = surfaceStack(hue, dark)
    return if (dark) {
        darkColorScheme(
            primary = hsl(hue, 0.65f, 0.75f),
            onPrimary = hsl(hue, 0.8f, 0.12f),
            primaryContainer = hsl(hue, 0.55f, 0.25f),
            onPrimaryContainer = hsl(hue, 0.75f, 0.88f),
            secondary = hsl(secondaryHue, 0.45f, 0.75f),
            onSecondary = hsl(secondaryHue, 0.6f, 0.12f),
            secondaryContainer = hsl(secondaryHue, 0.3f, 0.26f),
            onSecondaryContainer = hsl(secondaryHue, 0.5f, 0.88f),
            tertiary = hsl(tertiaryHue, 0.5f, 0.75f),
            onTertiary = hsl(tertiaryHue, 0.6f, 0.12f),
            tertiaryContainer = hsl(tertiaryHue, 0.35f, 0.27f),
            onTertiaryContainer = hsl(tertiaryHue, 0.55f, 0.88f),
            background = stack.background,
            onBackground = hsl(hue, 0.1f, 0.9f),
            surface = stack.surface,
            onSurface = hsl(hue, 0.1f, 0.9f),
            surfaceVariant = hsl(hue, 0.1f, 0.22f),
            onSurfaceVariant = hsl(hue, 0.1f, 0.75f),
            outline = hsl(hue, 0.06f, 0.55f),
            outlineVariant = hsl(hue, 0.08f, 0.28f),
            inverseSurface = hsl(hue, 0.1f, 0.9f),
            inverseOnSurface = hsl(hue, 0.2f, 0.15f),
            inversePrimary = hsl(hue, 0.6f, 0.3f),
            surfaceTint = hsl(hue, 0.65f, 0.75f),
            surfaceDim = stack.surfaceDim,
            surfaceBright = stack.surfaceBright,
            surfaceContainerLowest = stack.containerLowest,
            surfaceContainerLow = stack.containerLow,
            surfaceContainer = stack.container,
            surfaceContainerHigh = stack.containerHigh,
            surfaceContainerHighest = stack.containerHighest,
        )
    } else {
        lightColorScheme(
            primary = hsl(hue, 0.6f, 0.3f),
            onPrimary = Color.White,
            primaryContainer = hsl(hue, 0.7f, 0.88f),
            onPrimaryContainer = hsl(hue, 0.8f, 0.1f),
            secondary = hsl(secondaryHue, 0.35f, 0.32f),
            onSecondary = Color.White,
            secondaryContainer = hsl(secondaryHue, 0.45f, 0.89f),
            onSecondaryContainer = hsl(secondaryHue, 0.55f, 0.1f),
            tertiary = hsl(tertiaryHue, 0.4f, 0.33f),
            onTertiary = Color.White,
            tertiaryContainer = hsl(tertiaryHue, 0.5f, 0.89f),
            onTertiaryContainer = hsl(tertiaryHue, 0.6f, 0.1f),
            background = stack.background,
            onBackground = hsl(hue, 0.2f, 0.1f),
            surface = stack.surface,
            onSurface = hsl(hue, 0.2f, 0.1f),
            surfaceVariant = hsl(hue, 0.18f, 0.9f),
            onSurfaceVariant = hsl(hue, 0.12f, 0.3f),
            outline = hsl(hue, 0.08f, 0.45f),
            outlineVariant = hsl(hue, 0.12f, 0.8f),
            inverseSurface = hsl(hue, 0.15f, 0.2f),
            inverseOnSurface = hsl(hue, 0.2f, 0.94f),
            inversePrimary = hsl(hue, 0.7f, 0.75f),
            surfaceTint = hsl(hue, 0.6f, 0.3f),
            surfaceDim = stack.surfaceDim,
            surfaceBright = stack.surfaceBright,
            surfaceContainerLowest = stack.containerLowest,
            surfaceContainerLow = stack.containerLow,
            surfaceContainer = stack.container,
            surfaceContainerHigh = stack.containerHigh,
            surfaceContainerHighest = stack.containerHighest,
        )
    }
}

/** The swatch colour shown in the accent picker: the primary of that accent's scheme for the given brightness. */
fun AccentColor.previewColor(dark: Boolean): Color = when {
    this == AccentColor.Emerald && dark -> RybbitDarkScheme.primary
    this == AccentColor.Emerald -> RybbitLightScheme.primary
    else -> accentColorScheme(hue = hue, dark = dark).primary
}
