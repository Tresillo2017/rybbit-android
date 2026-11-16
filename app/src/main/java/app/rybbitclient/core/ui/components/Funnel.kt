package app.rybbitclient.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowDownward
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import app.rybbitclient.core.ui.format.EMPTY_VALUE
import app.rybbitclient.core.ui.format.formatFullNumber
import app.rybbitclient.core.ui.format.formatPercent
import app.rybbitclient.core.ui.theme.RybbitTheme
import app.rybbitclient.core.ui.theme.Spacing
import app.rybbitclient.core.ui.theme.rybbitColors
import app.rybbitclient.core.ui.theme.tabularBody

private const val FUNNEL_BAR_HEIGHT_DP = 30
private const val FUNNEL_BAR_CORNER_DP = 8
private const val FUNNEL_ICON_DP = 14
private const val FUNNEL_MIN_BAR_FRACTION = 0.02f

/**
 * One funnel step. [count] drives bar length relative to the first step. [conversionPercent] is
 * the share of the first step that reached this one; [dropoffPercent] is the share lost since the
 * previous step. All percentages use percent units (0..100), as Rybbit returns them.
 */
data class FunnelStepItem(
    val label: String,
    val count: Double,
    val conversionPercent: Double? = null,
    val dropoffPercent: Double? = null,
)

/**
 * Horizontal funnel: one labelled bar per step with count, conversion from the top, and the
 * drop-off from the previous step. Bars scale to the first step, so a shrinking bar reads at a glance.
 */
@Composable
fun FunnelChart(
    steps: List<FunnelStepItem>,
    modifier: Modifier = Modifier,
    summary: String? = null,
) {
    if (steps.isEmpty()) {
        Text(
            text = "No funnel data for this period",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = modifier.fillMaxWidth().padding(Spacing.lg),
        )
        return
    }
    val top = steps.first().count.takeIf { it > 0.0 } ?: 1.0
    Column(
        modifier = modifier
            .fillMaxWidth()
            .semantics { contentDescription = summary ?: funnelSummary(steps) },
        verticalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        steps.forEachIndexed { index, step ->
            if (index > 0) {
                DropoffRow(step.dropoffPercent)
            }
            FunnelStepRow(
                step = step,
                fraction = (step.count / top).toFloat().coerceIn(FUNNEL_MIN_BAR_FRACTION, 1f),
            )
        }
    }
}

@Composable
private fun FunnelStepRow(step: FunnelStepItem, fraction: Float) {
    val palette = MaterialTheme.rybbitColors
    val shape = RoundedCornerShape(FUNNEL_BAR_CORNER_DP.dp)
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.xxs)) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = step.label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            Spacer(Modifier.width(Spacing.sm))
            Text(
                text = formatFullNumber(step.count),
                style = MaterialTheme.typography.tabularBody,
                color = MaterialTheme.colorScheme.onSurface,
            )
            if (step.conversionPercent != null) {
                Text(
                    text = " · ${formatPercent(step.conversionPercent)}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                )
            }
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(FUNNEL_BAR_HEIGHT_DP.dp)
                .background(palette.barTrack, shape),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(fraction)
                    .background(palette.chartSeries.first(), shape),
            )
        }
    }
}

@Composable
private fun DropoffRow(dropoffPercent: Double?) {
    val text = if (dropoffPercent == null) EMPTY_VALUE else "${formatPercent(dropoffPercent)} drop-off"
    Row(
        modifier = Modifier.fillMaxWidth().padding(start = Spacing.xs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Outlined.ArrowDownward,
            contentDescription = null,
            tint = MaterialTheme.rybbitColors.negative,
            modifier = Modifier.size(FUNNEL_ICON_DP.dp),
        )
        Spacer(Modifier.width(Spacing.xs))
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.rybbitColors.negative,
        )
    }
}

private fun funnelSummary(steps: List<FunnelStepItem>): String {
    val first = steps.first()
    val last = steps.last()
    val overall = last.conversionPercent?.let { " Overall conversion ${formatPercent(it)}." } ?: ""
    return "Funnel with ${steps.size} steps. ${first.label} ${formatFullNumber(first.count)} visitors, " +
        "${last.label} ${formatFullNumber(last.count)}.$overall"
}

@Preview(showBackground = true)
@Composable
private fun FunnelChartPreview() {
    RybbitTheme {
        FunnelChart(
            steps = listOf(
                FunnelStepItem("Home", 8.0, 100.0, 0.0),
                FunnelStepItem("Pricing", 7.0, 87.5, 12.5),
                FunnelStepItem("Signup", 2.0, 25.0, 71.43),
            ),
            modifier = Modifier.padding(Spacing.lg),
        )
    }
}
