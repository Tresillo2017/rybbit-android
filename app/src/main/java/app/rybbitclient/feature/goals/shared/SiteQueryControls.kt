package app.rybbitclient.feature.goals.shared

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import app.rybbitclient.core.query.AnalyticsQuery
import app.rybbitclient.core.query.DateRange
import app.rybbitclient.core.query.SiteQueryState
import app.rybbitclient.core.ui.components.DateRangeBar
import app.rybbitclient.core.ui.components.FilterChipsRow
import app.rybbitclient.core.ui.theme.Spacing

private const val CUSTOM_CHIP_FALLBACK = "Custom"

/**
 * The date range bar and applied-filter chips bound to the shared [SiteQueryState]. Every analytics
 * screen that shows site data places this at its top, so the range and filters match across tabs.
 */
@Composable
fun SiteQueryControls(
    query: AnalyticsQuery,
    siteQuery: SiteQueryState,
    modifier: Modifier = Modifier,
) {
    val custom = query.range as? DateRange.Custom
    Column(
        modifier = modifier.padding(vertical = Spacing.sm),
        verticalArrangement = Arrangement.spacedBy(Spacing.xs),
    ) {
        DateRangeBar(
            options = SiteRangeOptions,
            selectedKey = presetKeyFor(query.range),
            onPresetSelected = { key -> presetRangeFor(key)?.let(siteQuery::setRange) },
            onCustomRangeSelected = { startMillis, endMillis ->
                siteQuery.setRange(customRangeFromMillis(startMillis, endMillis))
            },
            customLabel = customRangeLabel(query.range) ?: CUSTOM_CHIP_FALLBACK,
            customSelected = custom != null,
            initialStartMillis = custom?.let { localDateToMillis(it.start) },
            initialEndMillis = custom?.let { localDateToMillis(it.end) },
        )
        FilterChipsRow(
            filters = query.filters.map(::activeFilterOf),
            onRemove = { key ->
                query.filters.firstOrNull { it.parameter == key }?.let(siteQuery::removeFilter)
            },
            onClearAll = siteQuery::clearFilters,
        )
    }
}
