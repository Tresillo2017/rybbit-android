package app.rybbitclient.core.ui.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.unit.dp
import app.rybbitclient.core.ui.theme.rybbitColors

internal const val LINE_WIDTH_DP = 2.5f
internal const val PREVIOUS_LINE_WIDTH_DP = 1.5f
internal const val POINT_RADIUS_DP = 3.5f
internal const val INDICATOR_WIDTH_DP = 1f
internal const val LABEL_GAP_DP = 6f
internal const val GRID_WIDTH_DP = 1f
private const val FILL_ALPHA_TOP = 0.28f
private const val DASH_ON_PX = 10f
private const val DASH_OFF_PX = 8f

/** Colors used by the chart, resolved once per composition from the active theme. */
internal data class ChartPalette(
    val line: Color,
    val previous: Color,
    val grid: Color,
    val indicator: Color,
    val axisLabel: TextStyle,
    val pointFill: Color,
)

@Composable
internal fun chartPalette(): ChartPalette {
    val colors = MaterialTheme.colorScheme
    val rybbit = MaterialTheme.rybbitColors
    return ChartPalette(
        line = rybbit.chartSeries.first(),
        previous = rybbit.chartPrevious,
        grid = colors.outlineVariant.copy(alpha = 0.6f),
        indicator = colors.onSurfaceVariant,
        axisLabel = MaterialTheme.typography.labelSmall.copy(color = colors.onSurfaceVariant),
        pointFill = colors.surface,
    )
}

/** Geometry shared by every drawing step in one frame. */
internal data class ChartFrame(
    val area: PlotArea,
    val scale: YScale,
    val count: Int,
    val progress: Float,
)

internal fun DrawScope.drawGrid(
    frame: ChartFrame,
    palette: ChartPalette,
    valueFormatter: (Double) -> String,
    textMeasurer: TextMeasurer,
) {
    val area = frame.area
    val intervals = frame.scale.intervals
    val gap = LABEL_GAP_DP.dp.toPx()
    frame.scale.ticks().forEachIndexed { index, tick ->
        val y = area.bottom - area.height * index / intervals
        drawLine(
            color = palette.grid,
            start = Offset(area.left, y),
            end = Offset(area.right, y),
            strokeWidth = GRID_WIDTH_DP.dp.toPx(),
        )
        val label = valueFormatter(tick)
        val measured = textMeasurer.measure(text = label, style = palette.axisLabel).size
        drawText(
            textMeasurer = textMeasurer,
            text = label,
            topLeft = Offset(area.left - gap - measured.width, y - measured.height / 2f),
            style = palette.axisLabel,
        )
    }
}

internal fun DrawScope.drawXLabels(
    frame: ChartFrame,
    palette: ChartPalette,
    xLabelFormatter: (Int) -> String,
    textMeasurer: TextMeasurer,
) {
    val indices = xLabelIndices(frame.count)
    val gap = LABEL_GAP_DP.dp.toPx()
    indices.forEach { index ->
        val text = xLabelFormatter(index)
        if (text.isBlank()) return@forEach
        val measured = textMeasurer.measure(text = text, style = palette.axisLabel).size
        val anchorX = frame.area.xFor(index, frame.count)
        val left = xLabelLeft(index, indices, frame.count, anchorX, measured.width.toFloat())
        drawText(
            textMeasurer = textMeasurer,
            text = text,
            topLeft = Offset(
                x = left.coerceIn(0f, (size.width - measured.width).coerceAtLeast(0f)),
                y = frame.area.bottom + gap,
            ),
            style = palette.axisLabel,
        )
    }
}

/** First label hangs to the right of its anchor, last to the left, the rest are centred. */
private fun xLabelLeft(index: Int, indices: List<Int>, count: Int, anchorX: Float, labelWidth: Float): Float =
    when {
        count <= 1 -> anchorX - labelWidth / 2f
        index == indices.first() -> anchorX
        index == indices.last() -> anchorX - labelWidth
        else -> anchorX - labelWidth / 2f
    }

internal fun DrawScope.drawSeries(
    series: List<ChartSeries>,
    frame: ChartFrame,
    palette: ChartPalette,
    smooth: Boolean,
) {
    series.filter { it.isPrevious }.forEach { previous ->
        val points = seriesPoints(previous.values, frame.area, frame.scale, frame.count, frame.progress)
        drawCurve(points, smooth, palette.previous, PREVIOUS_LINE_WIDTH_DP, dashed = true)
    }
    val primary = series.firstOrNull { !it.isPrevious } ?: return
    val points = seriesPoints(primary.values, frame.area, frame.scale, frame.count, frame.progress)
    if (points.size >= 2) {
        drawArea(points, frame.area, smooth, palette.line)
    }
    drawCurve(points, smooth, palette.line, LINE_WIDTH_DP, dashed = false)
    drawEndpoint(points, palette)
}

internal fun DrawScope.drawIndicator(
    frame: ChartFrame,
    index: Int,
    series: List<ChartSeries>,
    palette: ChartPalette,
) {
    val area = frame.area
    val x = area.xFor(index, frame.count)
    drawLine(
        color = palette.indicator,
        start = Offset(x, area.top),
        end = Offset(x, area.bottom),
        strokeWidth = INDICATOR_WIDTH_DP.dp.toPx(),
    )
    val primary = series.firstOrNull { !it.isPrevious } ?: return
    val value = primary.values.getOrNull(index) ?: return
    drawCircle(
        color = palette.line,
        radius = POINT_RADIUS_DP.dp.toPx(),
        center = Offset(x, frame.area.yFor(value, frame.scale)),
    )
}

private fun DrawScope.drawCurve(
    points: List<Offset>,
    smooth: Boolean,
    color: Color,
    widthDp: Float,
    dashed: Boolean,
) {
    if (points.size < 2) return
    val style = Stroke(
        width = widthDp.dp.toPx(),
        cap = StrokeCap.Round,
        join = StrokeJoin.Round,
        pathEffect = if (dashed) dashEffect() else null,
    )
    drawPath(path = linePath(points, smooth), color = color, style = style)
}

private fun DrawScope.drawArea(points: List<Offset>, area: PlotArea, smooth: Boolean, color: Color) {
    val path = linePath(points, smooth)
    val last = points.last()
    val first = points.first()
    path.lineTo(last.x, area.bottom)
    path.lineTo(first.x, area.bottom)
    path.close()
    val gradient = Brush.verticalGradient(
        colors = listOf(color.copy(alpha = FILL_ALPHA_TOP), color.copy(alpha = 0f)),
        startY = area.top,
        endY = area.bottom,
    )
    drawPath(path = path, brush = gradient)
}

private fun DrawScope.drawEndpoint(points: List<Offset>, palette: ChartPalette) {
    val last = points.lastOrNull() ?: return
    val radius = POINT_RADIUS_DP.dp.toPx()
    if (points.size == 1) {
        drawCircle(color = palette.line, radius = radius, center = last)
    }
    drawCircle(color = palette.pointFill, radius = radius * 0.5f, center = last)
}

private fun dashEffect(): PathEffect = PathEffect.dashPathEffect(floatArrayOf(DASH_ON_PX, DASH_OFF_PX), 0f)

/** Open path through [points]. Smooth mode uses horizontal-midpoint cubics, which never overshoot in Y. */
internal fun linePath(points: List<Offset>, smooth: Boolean): Path {
    val path = Path()
    val first = points.firstOrNull() ?: return path
    path.moveTo(first.x, first.y)
    points.zipWithNext().forEach { (from, to) ->
        if (smooth) {
            val midX = (from.x + to.x) / 2f
            path.cubicTo(midX, from.y, midX, to.y, to.x, to.y)
        } else {
            path.lineTo(to.x, to.y)
        }
    }
    return path
}
