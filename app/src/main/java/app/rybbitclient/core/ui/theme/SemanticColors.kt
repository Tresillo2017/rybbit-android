package app.rybbitclient.core.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Analytics-specific colors that Material's scheme does not cover.
 * Every value is tuned to keep >= 4.5:1 against the matching surface in its theme.
 */
data class RybbitColors(
    /** Improvement for a metric where higher is better (e.g. sessions up). */
    val positive: Color,
    /** Regression for a metric where higher is better (e.g. sessions down). */
    val negative: Color,
    /** Flat / unchanged delta. */
    val neutral: Color,
    /** Categorical chart series, cycled by index. Always at least 6 entries. */
    val chartSeries: List<Color>,
    /** Dashed "previous period" line / comparison series. */
    val chartPrevious: Color,
    /** Translucent track behind proportional bars (bar lists, funnels). */
    val barTrack: Color,
    /** Background wash for the highlighted / selected KPI card. */
    val selectedWash: Color,
    /** Heatmap ramp endpoints used by retention grid: low and high intensity. */
    val heatLow: Color,
    val heatHigh: Color,
    /** Live indicator dot color. */
    val live: Color,
    val isDark: Boolean,
)

private val LightChartSeries = listOf(
    Color(0xFF059669), // emerald
    Color(0xFF0369A1), // sky
    Color(0xFF7C3AED), // violet
    Color(0xFFB45309), // amber
    Color(0xFFBE123C), // rose
    Color(0xFF475569), // slate
)

private val DarkChartSeries = listOf(
    Color(0xFF34D399),
    Color(0xFF38BDF8),
    Color(0xFFA78BFA),
    Color(0xFFFBBF24),
    Color(0xFFFB7185),
    Color(0xFF94A3B8),
)

val RybbitLightColors = RybbitColors(
    positive = Color(0xFF15803D),
    negative = Color(0xFFB91C1C),
    neutral = Color(0xFF64748B),
    chartSeries = LightChartSeries,
    chartPrevious = Color(0xFF6E7D76),
    barTrack = Color(0x1F059669),
    selectedWash = Color(0x14059669),
    heatLow = Color(0xFFECFDF5),
    heatHigh = Color(0xFF047857),
    live = Color(0xFF16A34A),
    isDark = false,
)

val RybbitDarkColors = RybbitColors(
    positive = Color(0xFF4ADE80),
    negative = Color(0xFFF87171),
    neutral = Color(0xFF94A3B8),
    chartSeries = DarkChartSeries,
    chartPrevious = Color(0xFF869590),
    barTrack = Color(0x3334D399),
    selectedWash = Color(0x1A34D399),
    heatLow = Color(0xFF12241E),
    heatHigh = Color(0xFF34D399),
    live = Color(0xFF4ADE80),
    isDark = true,
)

val LocalRybbitColors = staticCompositionLocalOf { RybbitLightColors }

/** Semantic analytics colors for the current [RybbitTheme]. */
val MaterialTheme.rybbitColors: RybbitColors
    @Composable
    get() = LocalRybbitColors.current

/** Picks the semantic palette matching a Material scheme's luminance. */
fun rybbitColorsFor(isDark: Boolean): RybbitColors =
    if (isDark) RybbitDarkColors else RybbitLightColors
