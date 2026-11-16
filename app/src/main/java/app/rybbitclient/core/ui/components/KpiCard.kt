package app.rybbitclient.core.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.TrendingDown
import androidx.compose.material.icons.automirrored.outlined.TrendingFlat
import androidx.compose.material.icons.automirrored.outlined.TrendingUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import app.rybbitclient.core.ui.haptics.rememberHaptics
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.animateColorAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import app.rybbitclient.core.ui.format.formatDeltaPercent
import app.rybbitclient.core.ui.theme.Spacing
import app.rybbitclient.core.ui.theme.metricDelta
import app.rybbitclient.core.ui.theme.metricValue
import app.rybbitclient.core.ui.theme.rybbitColors
import app.rybbitclient.core.ui.theme.RybbitTheme
import kotlin.math.abs

private const val FLAT_DELTA_THRESHOLD_PERCENT = 0.05
private const val TREND_ICON_SIZE_DP = 16
private const val KPI_MIN_HEIGHT_DP = 104
private const val SELECTED_BORDER_DP = 2
private const val RESTING_CORNER_DP = 20
private const val SELECTED_CORNER_DP = 32
private const val DELTA_CONTEXT_LABEL = "vs previous period"

/** Visible short form; the full [DELTA_CONTEXT_LABEL] stays in the accessibility description. */
private const val DELTA_CONTEXT_SHORT = "vs prev."

/** Direction of a change, independent of whether that direction is good or bad. */
enum class DeltaTrend { Up, Down, Flat }

/** Classifies a percent change; changes within +/-0.05% read as flat. */
fun deltaTrend(deltaPercent: Double): DeltaTrend = when {
    !deltaPercent.isFinite() || abs(deltaPercent) < FLAT_DELTA_THRESHOLD_PERCENT -> DeltaTrend.Flat
    deltaPercent > 0 -> DeltaTrend.Up
    else -> DeltaTrend.Down
}

/**
 * Metric tile: label, big tabular value, and an optional signed delta vs the previous period.
 *
 * - [isInverse] flips the color meaning (lower is better, e.g. bounce rate). The arrow still shows
 *   the true direction, so color is never the only signal.
 * - [selected] outlines the card and tints it; pair with [onClick] to make it a filter/tab control.
 */
@Composable
fun KpiCard(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    deltaPercent: Double? = null,
    isInverse: Boolean = false,
    selected: Boolean = false,
    onClick: (() -> Unit)? = null,
    compact: Boolean = false,
) {
    val colors = MaterialTheme.colorScheme
    val motion = MaterialTheme.motionScheme
    // Selection morphs the tile: it rounds up, tints and gains an outline. Colour is an effects
    // spring (no overshoot); the corner is a spatial spring so it settles with a small bounce.
    val containerColor by animateColorAsState(
        targetValue = if (selected) colors.primaryContainer.copy(alpha = 0.55f) else colors.surfaceContainerLow,
        animationSpec = motion.defaultEffectsSpec(),
        label = "kpiContainer",
    )
    val borderColor by animateColorAsState(
        targetValue = if (selected) colors.primary else Color.Transparent,
        animationSpec = motion.defaultEffectsSpec(),
        label = "kpiBorder",
    )
    val corner by animateDpAsState(
        targetValue = if (selected) SELECTED_CORNER_DP.dp else RESTING_CORNER_DP.dp,
        animationSpec = motion.fastSpatialSpec(),
        label = "kpiCorner",
    )
    val shape = RoundedCornerShape(corner.coerceAtLeast(0.dp))
    val border = BorderStroke(SELECTED_BORDER_DP.dp, borderColor)
    val description = buildKpiDescription(label, value, deltaPercent)
    val interactionSource = remember { MutableInteractionSource() }
    val haptics = rememberHaptics()
    val cardModifier = modifier
        .defaultKpiHeight()
        .pressScale(interactionSource)
        .semantics(mergeDescendants = true) {
            contentDescription = description
            if (selected) stateDescription = "Selected"
        }
    val cardColors = CardDefaults.cardColors(containerColor = containerColor)
    val content: @Composable () -> Unit = {
        KpiContent(label = label, value = value, deltaPercent = deltaPercent, isInverse = isInverse, compact = compact)
    }
    if (onClick != null) {
        Card(
            onClick = {
                haptics.selection()
                onClick()
            },
            modifier = cardModifier,
            shape = shape,
            colors = cardColors,
            border = border,
            interactionSource = interactionSource,
        ) { content() }
    } else {
        Card(modifier = cardModifier, shape = shape, colors = cardColors, border = border) { content() }
    }
}

@Composable
private fun KpiContent(
    label: String,
    value: String,
    deltaPercent: Double?,
    isInverse: Boolean,
    compact: Boolean,
) {
    // Narrow tiles (3+ per row) get tighter padding, a two-line label and a smaller number so values like
    // "100.0%" and labels like "Share of errors" fit instead of being ellipsized.
    val padding = if (compact) Spacing.md else Spacing.lg
    val valueStyle = if (compact) {
        MaterialTheme.typography.metricValue.copy(fontSize = MaterialTheme.typography.titleLarge.fontSize)
    } else {
        MaterialTheme.typography.metricValue
    }
    Column(modifier = Modifier.fillMaxWidth().padding(padding)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = if (compact) 2 else 1,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.height(Spacing.xs))
        Text(
            text = value,
            style = valueStyle,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        if (deltaPercent != null) {
            Spacer(Modifier.height(Spacing.xs))
            DeltaRow(deltaPercent = deltaPercent, isInverse = isInverse)
        }
    }
}

@Composable
private fun DeltaRow(deltaPercent: Double, isInverse: Boolean) {
    val trend = deltaTrend(deltaPercent)
    val tint = deltaTint(trend, isInverse)
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = when (trend) {
                DeltaTrend.Up -> Icons.AutoMirrored.Outlined.TrendingUp
                DeltaTrend.Down -> Icons.AutoMirrored.Outlined.TrendingDown
                DeltaTrend.Flat -> Icons.AutoMirrored.Outlined.TrendingFlat
            },
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(TREND_ICON_SIZE_DP.dp),
        )
        Spacer(Modifier.width(Spacing.xxs))
        Text(
            text = formatDeltaPercent(deltaPercent),
            style = MaterialTheme.typography.metricDelta,
            color = tint,
            maxLines = 1,
        )
        Spacer(Modifier.width(Spacing.xs))
        Text(
            text = DELTA_CONTEXT_SHORT,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun deltaTint(trend: DeltaTrend, isInverse: Boolean): Color {
    val palette = MaterialTheme.rybbitColors
    return when (trend) {
        DeltaTrend.Flat -> palette.neutral
        DeltaTrend.Up -> if (isInverse) palette.negative else palette.positive
        DeltaTrend.Down -> if (isInverse) palette.positive else palette.negative
    }
}

private fun Modifier.defaultKpiHeight(): Modifier = this.heightIn(min = KPI_MIN_HEIGHT_DP.dp)

private fun buildKpiDescription(label: String, value: String, deltaPercent: Double?): String =
    if (deltaPercent == null) {
        "$label, $value"
    } else {
        "$label, $value, ${formatDeltaPercent(deltaPercent)} $DELTA_CONTEXT_LABEL"
    }

/** One tile in a [KpiGrid]. Immutable; [key] identifies the tile for selection and clicks. */
data class KpiItem(
    val key: String,
    val label: String,
    val value: String,
    val deltaPercent: Double? = null,
    val isInverse: Boolean = false,
)

/**
 * Two-column (by default) grid of [KpiCard]s laid out as rows so it nests inside a scrolling column.
 * Cards in the same row share a height.
 */
@Composable
fun KpiGrid(
    items: List<KpiItem>,
    modifier: Modifier = Modifier,
    selectedKey: String? = null,
    columns: Int = 2,
    onItemClick: ((String) -> Unit)? = null,
) {
    val columnCount = columns.coerceAtLeast(1)
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        items.chunked(columnCount).forEach { rowItems ->
            Row(
                modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min),
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
            ) {
                rowItems.forEach { item ->
                    KpiCard(
                        label = item.label,
                        value = item.value,
                        deltaPercent = item.deltaPercent,
                        isInverse = item.isInverse,
                        selected = item.key == selectedKey,
                        onClick = onItemClick?.let { callback -> { callback(item.key) } },
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                        compact = columnCount >= COMPACT_COLUMN_THRESHOLD,
                    )
                }
                repeat(columnCount - rowItems.size) {
                    Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun KpiCardPreview() {
    RybbitTheme {
        Row(modifier = Modifier.padding(Spacing.lg)) {
            KpiCard(label = "Visitors", value = "12.4k", deltaPercent = 12.34, modifier = Modifier.weight(1f))
            Spacer(Modifier.width(Spacing.sm))
            KpiCard(
                label = "Bounce rate",
                value = "41.2%",
                deltaPercent = -4.0,
                isInverse = true,
                selected = true,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun KpiGridPreview() {
    RybbitTheme {
        KpiGrid(
            items = listOf(
                KpiItem("visitors", "Visitors", "12.4k", 12.3),
                KpiItem("sessions", "Sessions", "18.9k", -2.1),
                KpiItem("bounce", "Bounce rate", "41.2%", -4.0, isInverse = true),
                KpiItem("duration", "Avg. duration", "2m 05s", 0.0),
                KpiItem("pageviews", "Pageviews", "64.2k"),
            ),
            selectedKey = "sessions",
            modifier = Modifier.padding(Spacing.lg),
        )
    }
}

/** Grids with this many columns or more render compact tiles. */
private const val COMPACT_COLUMN_THRESHOLD = 3
