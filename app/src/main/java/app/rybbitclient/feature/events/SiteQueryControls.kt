package app.rybbitclient.feature.events

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import app.rybbitclient.core.query.AnalyticsQuery
import app.rybbitclient.core.query.DateRange
import app.rybbitclient.core.query.SiteQueryState
import app.rybbitclient.core.ui.components.ActiveFilter
import app.rybbitclient.core.ui.components.DateRangeBar
import app.rybbitclient.core.ui.components.FilterChipsRow
import app.rybbitclient.core.ui.theme.Spacing

private const val CUSTOM_CHIP_LABEL = "Custom"

/**
 * Date range chips and applied-filter chips, bound to the shared [SiteQueryState]. Every analytics
 * screen shows these so that changing the range or a filter reloads all of them.
 */
@Composable
fun SiteQueryControls(
    queryState: SiteQueryState,
    query: AnalyticsQuery,
    modifier: Modifier = Modifier,
) {
    val custom = query.range as? DateRange.Custom
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(Spacing.xs),
    ) {
        DateRangeBar(
            options = DateRangePresets.options,
            selectedKey = DateRangePresets.keyFor(query.range),
            onPresetSelected = { key -> DateRangePresets.rangeFor(key)?.let(queryState::setRange) },
            onCustomRangeSelected = { startMillis, endMillis ->
                queryState.setRange(customRangeFor(startMillis, endMillis))
            },
            customLabel = custom?.let(::customRangeText) ?: CUSTOM_CHIP_LABEL,
            customSelected = custom != null,
            initialStartMillis = custom?.let { millisOf(it.start) },
            initialEndMillis = custom?.let { millisOf(it.end) },
        )
        FilterChipsRow(
            filters = query.filters.map { ActiveFilter(key = it.parameter, label = filterLabel(it)) },
            onRemove = { key ->
                query.filters.firstOrNull { it.parameter == key }?.let(queryState::removeFilter)
            },
            onClearAll = queryState::clearFilters,
        )
    }
}
