@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package app.rybbitclient.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import app.rybbitclient.core.ui.theme.RybbitTheme
import app.rybbitclient.core.ui.theme.Spacing
import app.rybbitclient.core.ui.theme.rybbitColors
import app.rybbitclient.core.ui.theme.tabularBody

private const val ROW_MIN_HEIGHT_DP = 48
private const val BAR_CORNER_DP = 8
private const val RANK_WIDTH_DP = 20
private const val LEADING_WIDTH_DP = 28
private const val DEFAULT_VISIBLE_ROWS = 5
private const val SHOW_MORE_LABEL = "Show more"
private const val SHOW_LESS_LABEL = "Show less"
private const val EMPTY_LABEL = "No data for this period"

/** One ranked breakdown row. [value] drives the bar length relative to the largest item. */
data class BarItem(
    val key: String,
    val label: String,
    val value: Double,
    val valueText: String,
    val percentText: String? = null,
)

/**
 * Ranked row with a translucent proportional bar behind its label. [fraction] is 0..1 of the
 * full row width. Pass [leading] for a flag, icon or rank badge; [onClick] makes the row tappable.
 */
@Composable
fun BarListRow(
    label: String,
    valueText: String,
    fraction: Float,
    modifier: Modifier = Modifier,
    rank: Int? = null,
    percentText: String? = null,
    onClick: (() -> Unit)? = null,
    leading: (@Composable () -> Unit)? = null,
) {
    val barColor = MaterialTheme.rybbitColors.barTrack
    val shape = RoundedCornerShape(BAR_CORNER_DP.dp)
    val description = listOfNotNull(label, valueText, percentText).joinToString(separator = ", ")
    val rowModifier = modifier
        .fillMaxWidth()
        .heightIn(min = ROW_MIN_HEIGHT_DP.dp)
        .clip(shape)
        .let { base -> if (onClick != null) base.hapticClickable(onClick = onClick) else base }
        .semantics(mergeDescendants = true) { contentDescription = description }

    Box(modifier = rowModifier) {
        Box(modifier = Modifier.matchParentSize()) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(fraction.coerceIn(0f, 1f))
                    .background(barColor, shape),
            )
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = ROW_MIN_HEIGHT_DP.dp)
                .padding(horizontal = Spacing.sm, vertical = Spacing.xs),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (rank != null) {
                Text(
                    text = rank.toString(),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.width(RANK_WIDTH_DP.dp),
                    textAlign = TextAlign.Center,
                )
            }
            if (leading != null) {
                Box(modifier = Modifier.size(LEADING_WIDTH_DP.dp), contentAlignment = Alignment.Center) {
                    leading()
                }
                Spacer(Modifier.width(Spacing.sm))
            }
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            Spacer(Modifier.width(Spacing.sm))
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = valueText,
                    style = MaterialTheme.typography.tabularBody,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                )
                if (percentText != null) {
                    Text(
                        text = percentText,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                    )
                }
            }
        }
    }
}

/**
 * Ranked list of [items] (sorted by the caller). Shows [limit] rows, then a "Show more" footer:
 * it calls [onShowMore] when supplied (e.g. navigate to a full list), otherwise expands in place.
 */
@Composable
fun BarListBody(
    items: List<BarItem>,
    modifier: Modifier = Modifier,
    limit: Int = DEFAULT_VISIBLE_ROWS,
    onShowMore: (() -> Unit)? = null,
    onItemClick: ((String) -> Unit)? = null,
    itemLeading: (@Composable (BarItem) -> Unit)? = null,
    emptyMessage: String = EMPTY_LABEL,
) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    if (items.isEmpty()) {
        Text(
            text = emptyMessage,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = modifier.fillMaxWidth().padding(vertical = Spacing.lg),
            textAlign = TextAlign.Center,
        )
        return
    }
    val maxValue = items.maxOf { it.value }.takeIf { it > 0.0 } ?: 1.0
    val canExpand = items.size > limit
    val visible = if (expanded || !canExpand) items else items.take(limit)

    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(Spacing.xxs)) {
        visible.forEachIndexed { index, item ->
            val leadingContent: @Composable () -> Unit = { itemLeading?.invoke(item) }
            BarListRow(
                label = item.label,
                valueText = item.valueText,
                percentText = item.percentText,
                fraction = (item.value / maxValue).toFloat(),
                rank = index + 1,
                onClick = onItemClick?.let { callback -> { callback(item.key) } },
                leading = if (itemLeading != null) leadingContent else null,
            )
        }
        if (canExpand) {
            ShowMoreFooter(
                expanded = expanded,
                onShowMore = onShowMore,
                onToggle = { expanded = !expanded },
            )
        }
    }
}

@Composable
private fun ShowMoreFooter(
    expanded: Boolean,
    onShowMore: (() -> Unit)?,
    onToggle: () -> Unit,
) {
    val label = if (expanded) SHOW_LESS_LABEL else SHOW_MORE_LABEL
    val action: () -> Unit = if (onShowMore != null && !expanded) onShowMore else onToggle
    TextButton(
        onClick = action,
        modifier = Modifier.fillMaxWidth().minimumInteractiveComponentSize(),
    ) {
        Text(text = label, style = MaterialTheme.typography.labelLarge)
    }
}

/** A titled [SectionCard] wrapping a ranked [BarListBody]. */
@Composable
fun BarListCard(
    title: String,
    items: List<BarItem>,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    limit: Int = DEFAULT_VISIBLE_ROWS,
    onShowMore: (() -> Unit)? = null,
    onItemClick: ((String) -> Unit)? = null,
    itemLeading: (@Composable (BarItem) -> Unit)? = null,
) {
    SectionCard(title = title, subtitle = subtitle, modifier = modifier) {
        BarListBody(
            items = items,
            limit = limit,
            onShowMore = onShowMore,
            onItemClick = onItemClick,
            itemLeading = itemLeading,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun BarListCardPreview() {
    RybbitTheme {
        BarListCard(
            title = "Top pages",
            subtitle = "Pathname",
            items = listOf(
                BarItem("/", "/", 4_820.0, "4.8k", "38.1%"),
                BarItem("/pricing", "/pricing", 2_310.0, "2.3k", "18.2%"),
                BarItem("/docs", "/docs", 1_120.0, "1.1k", "8.9%"),
                BarItem("/blog", "/blog", 640.0, "640", "5.1%"),
                BarItem("/login", "/login", 310.0, "310", "2.4%"),
                BarItem("/signup", "/signup", 220.0, "220", "1.7%"),
            ),
            onShowMore = {},
            modifier = Modifier.padding(Spacing.lg),
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun BarListEmptyPreview() {
    RybbitTheme {
        BarListCard(title = "Referrers", items = emptyList(), modifier = Modifier.padding(Spacing.lg))
    }
}
