package app.rybbitclient.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import app.rybbitclient.core.ui.theme.Spacing
import kotlin.math.roundToInt

private const val TOOLTIP_EDGE_GAP_DP = 8f
private const val TOOLTIP_CORNER_DP = 10
private const val TOOLTIP_DOT_DP = 8
private const val TOOLTIP_TOP_DP = 4f

/**
 * Floating readout for the scrubbed index: X title plus one row per series. Centred over the
 * indicator and clamped to the chart bounds. It is hidden from accessibility because the chart's
 * summary already describes the value.
 *
 * Must be placed inside the chart's parent Box (it is positioned by offset, not by alignment).
 */
@Composable
internal fun LineChartTooltip(
    series: List<ChartSeries>,
    index: Int,
    palette: ChartPalette,
    containerWidthPx: Int,
    indicatorXPx: Float,
    tooltipSize: IntSize,
    onTooltipSizeChanged: (IntSize) -> Unit,
    xLabelFormatter: (Int) -> String,
    valueFormatter: (Double) -> String,
) {
    Surface(
        modifier = Modifier
            .offset {
                IntOffset(
                    x = clampedTooltipX(
                        indicatorX = indicatorXPx,
                        tooltipWidth = tooltipSize.width,
                        containerWidth = containerWidthPx,
                        edgeGapPx = TOOLTIP_EDGE_GAP_DP.dp.toPx(),
                    ),
                    y = TOOLTIP_TOP_DP.dp.roundToPx(),
                )
            }
            .onSizeChanged(onTooltipSizeChanged)
            .clearAndSetSemantics { },
        shape = RoundedCornerShape(TOOLTIP_CORNER_DP.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHighest,
        shadowElevation = 3.dp,
    ) {
        TooltipBody(series, index, palette, xLabelFormatter, valueFormatter)
    }
}

@Composable
private fun TooltipBody(
    series: List<ChartSeries>,
    index: Int,
    palette: ChartPalette,
    xLabelFormatter: (Int) -> String,
    valueFormatter: (Double) -> String,
) {
    Column(modifier = Modifier.padding(horizontal = Spacing.sm, vertical = Spacing.xs)) {
        val title = xLabelFormatter(index)
        if (title.isNotBlank()) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        series.forEach { s ->
            val value = s.values.getOrNull(index) ?: return@forEach
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(TOOLTIP_DOT_DP.dp)
                        .clip(CircleShape)
                        .background(seriesColor(s, palette)),
                )
                Spacer(Modifier.width(Spacing.xs))
                Text(
                    text = "${s.label}: ${valueFormatter(value)}",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        }
    }
}

private fun seriesColor(series: ChartSeries, palette: ChartPalette): Color =
    if (series.isPrevious) palette.previous else palette.line

/** Centres the tooltip over the indicator, keeping it inside the chart with a small edge gap. */
private fun clampedTooltipX(
    indicatorX: Float,
    tooltipWidth: Int,
    containerWidth: Int,
    edgeGapPx: Float,
): Int {
    val centred = indicatorX - tooltipWidth / 2f
    val maxX = (containerWidth - tooltipWidth - edgeGapPx).coerceAtLeast(edgeGapPx)
    return centred.coerceIn(edgeGapPx, maxX).roundToInt()
}
