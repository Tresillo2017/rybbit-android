package app.rybbitclient.core.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.rybbitclient.core.ui.format.formatFullNumber
import app.rybbitclient.core.ui.theme.RybbitTheme
import app.rybbitclient.core.ui.theme.Spacing
import app.rybbitclient.core.ui.theme.rybbitColors

private const val SPARKLINE_DEFAULT_HEIGHT_DP = 36
private const val SPARKLINE_STROKE_DP = 2f
private const val SPARKLINE_END_DOT_DP = 2.5f
private const val SPARKLINE_PADDING_DP = 3f
private const val SPARKLINE_EMPTY_DESCRIPTION = "Trend unavailable"

/**
 * Tiny trend line with no axes. Scales to its own min..max so the shape is visible even for
 * large absolute values. Flat series draw at mid-height. Empty input draws nothing.
 */
@Composable
fun Sparkline(
    values: List<Double>,
    modifier: Modifier = Modifier,
    color: Color? = null,
    height: Dp = SPARKLINE_DEFAULT_HEIGHT_DP.dp,
    summary: String? = null,
) {
    val lineColor = color ?: MaterialTheme.rybbitColors.chartSeries.first()
    val description = summary ?: sparklineSummary(values)
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .semantics { contentDescription = description },
    ) {
        if (values.isEmpty()) return@Canvas
        val points = sparklinePoints(values, size.width, size.height, SPARKLINE_PADDING_DP.dp.toPx())
        if (points.size >= 2) {
            drawPath(
                path = linePath(points, smooth = true),
                color = lineColor,
                style = Stroke(
                    width = SPARKLINE_STROKE_DP.dp.toPx(),
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round,
                ),
            )
        }
        drawCircle(
            color = lineColor,
            radius = SPARKLINE_END_DOT_DP.dp.toPx(),
            center = points.last(),
        )
    }
}

/** Maps values to canvas points, inset vertically by [paddingPx]. Flat series sit mid-height. */
internal fun sparklinePoints(
    values: List<Double>,
    width: Float,
    height: Float,
    paddingPx: Float,
): List<Offset> {
    val minValue = values.min()
    val range = values.max() - minValue
    val usableHeight = (height - 2f * paddingPx).coerceAtLeast(0f)
    val count = values.size
    return values.mapIndexed { index, value ->
        val x = if (count <= 1) width / 2f else width * index / (count - 1)
        val normalized = if (range <= 0.0) 0.5 else (value - minValue) / range
        val y = paddingPx + usableHeight * (1.0 - normalized).toFloat()
        Offset(x, y)
    }
}

private fun sparklineSummary(values: List<Double>): String {
    if (values.isEmpty()) return SPARKLINE_EMPTY_DESCRIPTION
    val first = values.first()
    val last = values.last()
    val direction = when {
        last > first -> "rising"
        last < first -> "falling"
        else -> "flat"
    }
    return "Trend $direction, latest ${formatFullNumber(last)}"
}

@Preview(showBackground = true)
@Composable
private fun SparklinePreview() {
    RybbitTheme {
        Sparkline(
            values = listOf(3.0, 5.0, 4.0, 8.0, 7.0, 10.0, 9.0),
            modifier = Modifier.padding(Spacing.lg),
        )
    }
}
