package app.rybbitclient.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import app.rybbitclient.core.ui.format.EMPTY_VALUE
import app.rybbitclient.core.ui.format.formatPercent
import app.rybbitclient.core.ui.theme.RybbitTheme
import app.rybbitclient.core.ui.theme.Spacing
import app.rybbitclient.core.ui.theme.rybbitColors
import app.rybbitclient.core.ui.theme.tabularBody

private const val COHORT_LABEL_WIDTH_DP = 96
private const val COHORT_SIZE_WIDTH_DP = 64
private const val PERIOD_CELL_WIDTH_DP = 56
private const val CELL_HEIGHT_DP = 36
private const val CELL_CORNER_DP = 6
private const val PERCENT_SCALE = 100.0
private const val DARK_TEXT_LUMINANCE_LIMIT = 0.45f
private val DarkCellText = Color(0xFF0F1A16)

/**
 * One retention cohort. [percentages] are percent units (0..100) by period; `null` means the period
 * has not happened yet and renders as an empty cell.
 */
data class RetentionCohort(
    val label: String,
    val size: Int,
    val percentages: List<Double?>,
)

/**
 * Cohort retention heatmap: rows are cohorts, columns are periods. Cells ramp from the theme's
 * low to high heat color by retention. The table scrolls horizontally on narrow screens.
 * [periodLabel] names each column header (defaults to `P0`, `P1`, ...).
 */
@Composable
fun RetentionGrid(
    cohorts: List<RetentionCohort>,
    modifier: Modifier = Modifier,
    periodLabel: (Int) -> String = { "P$it" },
    cohortHeader: String = "Cohort",
    sizeHeader: String = "Users",
) {
    if (cohorts.isEmpty()) {
        Text(
            text = "No retention data for this period",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = modifier.fillMaxWidth().padding(Spacing.lg),
        )
        return
    }
    val periods = cohorts.maxOf { it.percentages.size }
    Column(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(Spacing.xxs),
    ) {
        RetentionHeaderRow(periods = periods, periodLabel = periodLabel, cohortHeader = cohortHeader, sizeHeader = sizeHeader)
        cohorts.forEach { cohort ->
            RetentionCohortRow(cohort = cohort, periods = periods, periodLabel = periodLabel)
        }
    }
}

@Composable
private fun RetentionHeaderRow(
    periods: Int,
    periodLabel: (Int) -> String,
    cohortHeader: String,
    sizeHeader: String,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        HeaderCell(text = cohortHeader, width = COHORT_LABEL_WIDTH_DP, alignment = TextAlign.Start)
        HeaderCell(text = sizeHeader, width = COHORT_SIZE_WIDTH_DP, alignment = TextAlign.End)
        (0 until periods).forEach { period ->
            HeaderCell(text = periodLabel(period), width = PERIOD_CELL_WIDTH_DP, alignment = TextAlign.Center)
        }
    }
}

@Composable
private fun HeaderCell(text: String, width: Int, alignment: TextAlign) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = alignment,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier
            .width(width.dp)
            .padding(horizontal = Spacing.xxs, vertical = Spacing.xs),
    )
}

@Composable
private fun RetentionCohortRow(
    cohort: RetentionCohort,
    periods: Int,
    periodLabel: (Int) -> String,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = cohort.label,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.width(COHORT_LABEL_WIDTH_DP.dp).padding(horizontal = Spacing.xxs),
        )
        Text(
            text = cohort.size.toString(),
            style = MaterialTheme.typography.tabularBody,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.End,
            modifier = Modifier.width(COHORT_SIZE_WIDTH_DP.dp).padding(horizontal = Spacing.xxs),
        )
        (0 until periods).forEach { period ->
            RetentionCell(
                cohortLabel = cohort.label,
                periodName = periodLabel(period),
                percent = cohort.percentages.getOrNull(period),
            )
        }
    }
}

@Composable
private fun RetentionCell(cohortLabel: String, periodName: String, percent: Double?) {
    val palette = MaterialTheme.rybbitColors
    val intensity = ((percent ?: 0.0) / PERCENT_SCALE).coerceIn(0.0, 1.0).toFloat()
    val background = if (percent == null) {
        MaterialTheme.colorScheme.surfaceVariant
    } else {
        lerp(palette.heatLow, palette.heatHigh, intensity)
    }
    val textColor = textColorFor(background)
    val label = if (percent == null) EMPTY_VALUE else formatPercent(percent, fractionDigits = 0)
    val description = if (percent == null) {
        "$cohortLabel, $periodName, not yet measured"
    } else {
        "$cohortLabel, $periodName, ${formatPercent(percent)} retained"
    }
    Box(
        modifier = Modifier
            .width(PERIOD_CELL_WIDTH_DP.dp)
            .height(CELL_HEIGHT_DP.dp)
            .padding(horizontal = 2.dp)
            .background(background, RoundedCornerShape(CELL_CORNER_DP.dp))
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = textColor,
            maxLines = 1,
        )
    }
}

/** Light text on dark heat cells, dark text on light ones, so every cell meets contrast needs. */
private fun textColorFor(background: Color): Color =
    if (background.luminance() < DARK_TEXT_LUMINANCE_LIMIT) Color.White else DarkCellText

@Preview(showBackground = true)
@Composable
private fun RetentionGridPreview() {
    RybbitTheme {
        RetentionGrid(
            cohorts = listOf(
                RetentionCohort("Nov 1", 420, listOf(100.0, 42.0, 31.5, 25.0, 22.0)),
                RetentionCohort("Nov 8", 390, listOf(100.0, 38.4, 27.2, 21.0)),
                RetentionCohort("Nov 15", 265, listOf(100.0, 35.1, 24.0)),
                RetentionCohort("Nov 22", 310, listOf(100.0)),
            ),
            periodLabel = { "W$it" },
            modifier = Modifier.padding(Spacing.lg),
        )
    }
}
