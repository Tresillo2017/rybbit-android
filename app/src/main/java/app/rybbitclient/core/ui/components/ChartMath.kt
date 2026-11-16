package app.rybbitclient.core.ui.components

import androidx.compose.ui.geometry.Offset
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.log10
import kotlin.math.pow
import kotlin.math.roundToInt

/** One line in a [LineChart]. [values] are aligned by index with the chart's x positions. */
data class ChartSeries(
    val label: String,
    val values: List<Double>,
    /** Dashed comparison line (e.g. previous period). Only the first non-previous series is filled. */
    val isPrevious: Boolean = false,
)

/** Y-axis scale: [intervals] equal steps of size [step], starting at zero. */
data class YScale(val step: Double, val intervals: Int) {
    val max: Double get() = step * intervals

    fun ticks(): List<Double> = (0..intervals).map { it * step }
}

/** Plot rectangle in pixels, inset from the chart's outer bounds by the axis gutters. */
data class PlotArea(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float,
) {
    val width: Float get() = right - left
    val height: Float get() = bottom - top

    fun xFor(index: Int, count: Int): Float =
        if (count <= 1) left + width / 2f else left + width * index / (count - 1)

    fun yFor(value: Double, scale: YScale): Float {
        if (scale.max <= 0.0) return bottom
        return (bottom - height * (value / scale.max)).toFloat()
    }

    /** Nearest data index for a horizontal pixel position, clamped to the valid range. */
    fun indexAt(x: Float, count: Int): Int {
        if (count <= 1 || width <= 0f) return 0
        val fraction = ((x - left) / width).coerceIn(0f, 1f)
        return (fraction * (count - 1)).roundToInt().coerceIn(0, count - 1)
    }
}

private const val NICE_TARGET_INTERVALS = 4
private const val INTERVAL_EPSILON = 1e-9
private const val MIN_SCALE_MAX = 1.0

/** Counts, percents and seconds read best with whole-unit ticks; avoids "0, 0, 1, 1" labels. */
private const val MIN_TICK_STEP = 1.0

/**
 * Picks a 1-2-2.5-5-10 step so the axis has about [targetIntervals] steps and starts at zero.
 * Steps never drop below [minStep] (default one whole unit).
 * All-zero, negative or non-finite maxima fall back to a 0..1 scale so the baseline still renders.
 */
fun niceYScale(
    maxValue: Double,
    targetIntervals: Int = NICE_TARGET_INTERVALS,
    minStep: Double = MIN_TICK_STEP,
): YScale {
    val safeMax = if (maxValue.isFinite() && maxValue > 0.0) maxValue else MIN_SCALE_MAX
    val rawStep = safeMax / targetIntervals.coerceAtLeast(1)
    val exponent = floor(log10(rawStep))
    val base = 10.0.pow(exponent)
    val fraction = rawStep / base
    val niceFraction = when {
        fraction <= 1.0 -> 1.0
        fraction <= 2.0 -> 2.0
        fraction <= 2.5 -> 2.5
        fraction <= 5.0 -> 5.0
        else -> 10.0
    }
    val step = maxOf(niceFraction * base, minStep)
    val intervals = ceil(safeMax / step - INTERVAL_EPSILON).toInt().coerceAtLeast(1)
    return YScale(step = step, intervals = intervals)
}

/** Evenly spread label indices, always including the first and last point when [count] > 1. */
fun xLabelIndices(count: Int, maxLabels: Int = 4): List<Int> {
    if (count <= 0) return emptyList()
    if (count == 1) return listOf(0)
    val labelCount = minOf(maxLabels.coerceAtLeast(2), count)
    return (0 until labelCount).map { step ->
        (step * (count - 1).toDouble() / (labelCount - 1)).roundToInt()
    }.distinct()
}

/** Points for a series, mapped into the plot area, optionally grown from the baseline by [progress]. */
fun seriesPoints(
    values: List<Double>,
    area: PlotArea,
    scale: YScale,
    count: Int,
    progress: Float,
): List<Offset> = values.mapIndexed { index, value ->
    val targetY = area.yFor(value, scale)
    val animatedY = area.bottom - (area.bottom - targetY) * progress
    Offset(area.xFor(index, count), animatedY)
}
