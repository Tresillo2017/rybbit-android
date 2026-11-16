package app.rybbitclient.core.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import app.rybbitclient.core.ui.theme.LocalSmoothCharts
import app.rybbitclient.core.ui.haptics.rememberHaptics
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.gestures.awaitHorizontalTouchSlopOrCancellation
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import app.rybbitclient.core.ui.format.formatCompactNumber
import app.rybbitclient.core.ui.theme.RybbitTheme
import app.rybbitclient.core.ui.theme.Spacing
import kotlin.math.roundToInt

private const val DEFAULT_CHART_HEIGHT_DP = 220
private const val YAXIS_GUTTER_DP = 48f
private const val TOP_PADDING_DP = 12f
private const val RIGHT_PADDING_DP = 12f
private const val BOTTOM_GUTTER_DP = 26f
private const val NO_DATA_TEXT = "No data for this period"

/**
 * Canvas line chart with optional dashed comparison series, a gradient area under the first
 * non-previous series, nice Y ticks, evenly spaced X labels, and touch/drag scrubbing.
 *
 * - [xLabelFormatter] supplies X-axis and tooltip titles for an index.
 * - [valueFormatter] formats Y ticks and tooltip values (defaults to compact numbers).
 * - [onPointSelected] fires with the scrubbed index, or `null` when the finger lifts.
 * - [summary] overrides the generated TalkBack summary.
 * - Data grows from the baseline whenever [series] changes.
 * - Empty input shows a "no data" message; a single point renders as a dot; all-zero renders a baseline.
 * Non-finite values are treated as zero.
 */
@Composable
fun LineChart(
    series: List<ChartSeries>,
    modifier: Modifier = Modifier,
    height: Dp = DEFAULT_CHART_HEIGHT_DP.dp,
    smooth: Boolean = LocalSmoothCharts.current,
    summary: String? = null,
    valueFormatter: (Double) -> String = { formatCompactNumber(it) },
    xLabelFormatter: (Int) -> String = { "" },
    onPointSelected: ((Int?) -> Unit)? = null,
) {
    val cleanSeries = remember(series) { sanitizeSeries(series) }
    val pointCount = cleanSeries.maxOfOrNull { it.values.size } ?: 0
    if (pointCount == 0) {
        EmptyChartPlaceholder(modifier = modifier, height = height)
    } else {
        PopulatedLineChart(
            series = cleanSeries,
            pointCount = pointCount,
            modifier = modifier,
            height = height,
            smooth = smooth,
            description = summary ?: buildChartSummary(cleanSeries, valueFormatter, xLabelFormatter),
            valueFormatter = valueFormatter,
            xLabelFormatter = xLabelFormatter,
            onPointSelected = onPointSelected,
        )
    }
}

@Composable
private fun PopulatedLineChart(
    series: List<ChartSeries>,
    pointCount: Int,
    modifier: Modifier,
    height: Dp,
    smooth: Boolean,
    description: String,
    valueFormatter: (Double) -> String,
    xLabelFormatter: (Int) -> String,
    onPointSelected: ((Int?) -> Unit)?,
) {
    val palette = chartPalette()
    val density = LocalDensity.current
    val scale = niceYScale(series.maxOf { s -> s.values.maxOrNull() ?: 0.0 })
    val textMeasurer = rememberTextMeasurer()
    val progress = remember { Animatable(0f) }
    // Effects spring (no overshoot): the line draws in smoothly and a data chart never springs past its values.
    val drawSpec = MaterialTheme.motionScheme.slowEffectsSpec<Float>()
    val haptics = rememberHaptics()
    LaunchedEffect(series) {
        progress.snapTo(0f)
        progress.animateTo(1f, animationSpec = drawSpec)
    }
    var selectedIndex by remember(series) { mutableStateOf<Int?>(null) }
    var containerSize by remember { mutableStateOf(IntSize.Zero) }
    var tooltipSize by remember { mutableStateOf(IntSize.Zero) }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .onSizeChanged { containerSize = it }
            .semantics { contentDescription = description }
            .pointerInput(pointCount, onPointSelected) {
                val indexAt: (Float) -> Int = { x ->
                    plotAreaFor(size.width.toFloat(), size.height.toFloat(), this)
                        .indexAt(x, pointCount)
                }
                scrubGestures(indexAt) { index ->
                    if (index != null && index != selectedIndex) haptics.scrubTick()
                    selectedIndex = index
                    onPointSelected?.invoke(index)
                }
            },
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val frame = ChartFrame(
                area = plotAreaFor(size.width, size.height, this),
                scale = scale,
                count = pointCount,
                progress = progress.value,
            )
            drawGrid(frame, palette, valueFormatter, textMeasurer)
            drawXLabels(frame, palette, xLabelFormatter, textMeasurer)
            drawSeries(series, frame, palette, smooth)
            selectedIndex?.let { drawIndicator(frame, it, series, palette) }
        }
        val index = selectedIndex
        if (index != null) {
            val indicatorX = plotAreaFor(
                containerSize.width.toFloat(),
                containerSize.height.toFloat(),
                density,
            ).xFor(index, pointCount)
            LineChartTooltip(
                series = series,
                index = index,
                palette = palette,
                containerWidthPx = containerSize.width,
                indicatorXPx = indicatorX,
                tooltipSize = tooltipSize,
                onTooltipSizeChanged = { tooltipSize = it },
                xLabelFormatter = xLabelFormatter,
                valueFormatter = valueFormatter,
            )
        }
    }
}

@Composable
private fun EmptyChartPlaceholder(modifier: Modifier, height: Dp) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .semantics { contentDescription = NO_DATA_TEXT },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = NO_DATA_TEXT,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/**
 * Gesture loop: a horizontal drag past touch slop scrubs to the nearest index; lifting reports `null`.
 * Vertical movement is left unconsumed until then, so an enclosing list still scrolls and refreshes when a
 * touch starts on the chart.
 */
private suspend fun PointerInputScope.scrubGestures(
    indexAt: (Float) -> Int,
    onIndex: (Int?) -> Unit,
) {
    awaitEachGesture {
        val down = awaitFirstDown(requireUnconsumed = false)
        val slop = awaitHorizontalTouchSlopOrCancellation(down.id) { change, _ -> change.consume() } ?: return@awaitEachGesture
        onIndex(indexAt(slop.position.x))
        var pressed = true
        while (pressed) {
            val change = awaitPointerEvent().changes.first()
            change.consume()
            pressed = change.pressed
            if (pressed) onIndex(indexAt(change.position.x))
        }
        onIndex(null)
    }
}

/** Plot rectangle for a canvas of the given pixel size; gutters are converted with [density]. */
internal fun plotAreaFor(width: Float, height: Float, density: Density): PlotArea {
    val left = with(density) { YAXIS_GUTTER_DP.dp.toPx() }
    val top = with(density) { TOP_PADDING_DP.dp.toPx() }
    val right = with(density) { RIGHT_PADDING_DP.dp.toPx() }
    val bottom = with(density) { BOTTOM_GUTTER_DP.dp.toPx() }
    return PlotArea(
        left = left,
        top = top,
        right = (width - right).coerceAtLeast(left),
        bottom = (height - bottom).coerceAtLeast(top),
    )
}

private fun sanitizeSeries(series: List<ChartSeries>): List<ChartSeries> =
    series.map { s ->
        s.copy(values = s.values.map { value -> if (value.isFinite()) value else 0.0 })
    }

/** TalkBack summary: primary series peak and latest value, plus the previous-period peak if present. */
internal fun buildChartSummary(
    series: List<ChartSeries>,
    valueFormatter: (Double) -> String,
    xLabelFormatter: (Int) -> String,
): String {
    val primary = series.firstOrNull { !it.isPrevious } ?: return NO_DATA_TEXT
    if (primary.values.isEmpty()) return NO_DATA_TEXT
    val peakIndex = primary.values.indices.maxByOrNull { primary.values[it] } ?: 0
    val peakLabel = xLabelFormatter(peakIndex).takeIf { it.isNotBlank() }
    val parts = mutableListOf(
        "${primary.label} line chart. Peak ${valueFormatter(primary.values[peakIndex])}" +
            (peakLabel?.let { " on $it" } ?: ""),
        "Latest ${valueFormatter(primary.values.last())}.",
    )
    series.filter { it.isPrevious && it.values.isNotEmpty() }.forEach { previous ->
        parts += "${previous.label} peak ${valueFormatter(previous.values.max())}."
    }
    return parts.joinToString(separator = " ")
}

@Preview(showBackground = true)
@Composable
private fun LineChartPreview() {
    val current = listOf(120.0, 180.0, 160.0, 240.0, 210.0, 320.0, 290.0, 360.0, 340.0, 410.0, 380.0, 460.0)
    val previous = listOf(90.0, 130.0, 150.0, 170.0, 160.0, 210.0, 230.0, 240.0, 260.0, 290.0, 300.0, 330.0)
    RybbitTheme {
        LineChart(
            series = listOf(
                ChartSeries("Sessions", current),
                ChartSeries("Previous", previous, isPrevious = true),
            ),
            xLabelFormatter = { index -> "Nov ${index + 1}" },
            modifier = Modifier.padding(Spacing.lg),
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun LineChartEmptyPreview() {
    RybbitTheme {
        LineChart(series = listOf(ChartSeries("Sessions", emptyList())))
    }
}

@Preview(showBackground = true)
@Composable
private fun LineChartZeroPreview() {
    RybbitTheme {
        LineChart(series = listOf(ChartSeries("Sessions", listOf(0.0, 0.0, 0.0, 0.0))))
    }
}
