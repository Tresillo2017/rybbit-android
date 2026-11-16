package app.rybbitclient.feature.overview

import app.rybbitclient.core.ui.components.hapticClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import app.rybbitclient.core.ui.components.ExpressiveRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.lifecycle.compose.LifecycleStartEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import app.rybbitclient.core.query.DateRange
import app.rybbitclient.core.ui.components.ActiveFilter
import app.rybbitclient.core.ui.components.DateRangeBar
import app.rybbitclient.core.ui.components.DateRangeOption
import app.rybbitclient.core.ui.components.FilterChipsRow
import app.rybbitclient.core.ui.components.LiveBadge
import app.rybbitclient.core.ui.theme.Spacing
import app.rybbitclient.di.LocalAppContainer

private val RANGE_OPTIONS = RANGE_PRESETS.map { DateRangeOption(it.key, it.label) }

/**
 * The Overview tab, the main dashboard: date range and filters, live visitors, six KPI tiles with
 * deltas, a trend chart for the selected KPI, and five breakdown cards with "show more" sheets.
 *
 * Parameters:
 * - [siteId]: Site.analyticsId for API calls.
 * - [onOpenRealtime]: Open the Realtime screen (from the live badge).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OverviewScreen(
    siteId: String,
    onOpenRealtime: () -> Unit,
) {
    val container = LocalAppContainer.current
    val viewModel: OverviewViewModel = viewModel(key = "overview-$siteId") {
        OverviewViewModel(siteId, container.analyticsRepository, container.siteQueryState)
    }
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LifecycleStartEffect(viewModel) {
        viewModel.startLivePolling()
        onStopOrDispose { viewModel.stopLivePolling() }
    }
    OverviewList(state = state, viewModel = viewModel, onOpenRealtime = onOpenRealtime)
    state.fullList?.let { list ->
        FullListSheet(
            list = list,
            title = breakdownTitle(list.parameter),
            onRowSelected = { raw -> viewModel.selectBreakdownValue(list.parameter, raw) },
            onLoadMore = viewModel::loadMoreFullList,
            onDismiss = viewModel::closeFullList,
        )
    }
}

@Composable
private fun OverviewList(
    state: OverviewUiState,
    viewModel: OverviewViewModel,
    onOpenRealtime: () -> Unit,
) {
    ExpressiveRefreshBox(
        isRefreshing = state.isRefreshing,
        onRefresh = viewModel::refresh,
        modifier = Modifier.fillMaxSize(),
    ) {
        LazyColumn(
            contentPadding = PaddingValues(bottom = Spacing.xl),
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            item(key = "range") { RangeRow(state, viewModel) }
            if (state.query.filters.isNotEmpty()) {
                item(key = "filters") { FilterRow(state, viewModel) }
            }
            item(key = "live") { LiveRow(state, onOpenRealtime) }
            item(key = "kpis") { KpiSection(state, viewModel) }
            item(key = "trend") { TrendSection(state, viewModel) }
            items(BREAKDOWN_CARDS, key = { it.id }) { card ->
                BreakdownSection(card = card, state = state, viewModel = viewModel)
            }
        }
    }
}

@Composable
private fun RangeRow(state: OverviewUiState, viewModel: OverviewViewModel) {
    val range = state.query.range
    val custom = range as? DateRange.Custom
    DateRangeBar(
        options = RANGE_OPTIONS,
        selectedKey = presetKeyFor(range),
        onPresetSelected = viewModel::selectPreset,
        onCustomRangeSelected = viewModel::selectCustomRange,
        customLabel = custom?.let { customRangeLabel(it) } ?: "Custom",
        customSelected = custom != null,
        initialStartMillis = custom?.let { millisOf(it.start) },
        initialEndMillis = custom?.let { millisOf(it.end) },
    )
}

@Composable
private fun FilterRow(state: OverviewUiState, viewModel: OverviewViewModel) {
    FilterChipsRow(
        filters = state.query.filters.map { ActiveFilter(key = it.parameter, label = filterChipLabel(it)) },
        onRemove = viewModel::removeFilter,
        onClearAll = viewModel::clearFilters,
    )
}

@Composable
private fun LiveRow(state: OverviewUiState, onOpenRealtime: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.screenGutter),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        LiveBadge(
            count = (state.live as? Section.Loaded)?.value,
            modifier = Modifier
                .minimumInteractiveComponentSize()
                .hapticClickable(role = Role.Button, onClickLabel = "Open realtime", onClick = onOpenRealtime),
        )
        Spacer(Modifier.width(Spacing.xs))
        TextButton(
            onClick = onOpenRealtime,
            modifier = Modifier.minimumInteractiveComponentSize(),
        ) {
            Text("Realtime")
        }
    }
}

private fun breakdownTitle(parameter: String): String {
    val card = BREAKDOWN_CARDS.firstOrNull { card -> card.tabs.any { it.parameter == parameter } }
    val tab = card?.tabs?.firstOrNull { it.parameter == parameter }
    return if (card == null || tab == null) parameterLabel(parameter) else "${card.title}: ${tab.label}"
}
