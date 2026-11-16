package app.rybbitclient.feature.overview

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import app.rybbitclient.core.ui.components.BreakdownCard
import app.rybbitclient.core.ui.theme.Spacing

private const val BREAKDOWN_LIMIT = 5
private const val LOADING_TEXT = "Loading..."
private const val FAILED_TEXT = "Couldn't load this list. Tap the tab to try again."
private const val EMPTY_TEXT = "No data for this period"

/**
 * One breakdown card. Tabs load lazily: the selected tab's rows come from [OverviewUiState.tabs], and
 * tapping a row adds an equality filter. Each card reads its own tab state, so one failure stays local.
 */
@Composable
fun BreakdownSection(
    card: BreakdownCardSpec,
    state: OverviewUiState,
    viewModel: OverviewViewModel,
) {
    val selected = state.selectedTabs[card.id] ?: 0
    val parameter = card.parameterAt(selected)
    val tab = state.tabs[parameter]
    val loaded = (tab as? Section.Loaded)?.value
    BreakdownCard(
        title = card.title,
        tabs = card.tabs.map { it.label },
        selectedTab = selected,
        onTabSelected = { index -> viewModel.selectTab(card.id, index) },
        items = barItemsFor(parameter, loaded?.rows.orEmpty()),
        modifier = Modifier.padding(horizontal = Spacing.screenGutter),
        limit = BREAKDOWN_LIMIT,
        onShowMore = if (loaded != null && loaded.rows.isNotEmpty()) {
            { viewModel.openFullList(parameter) }
        } else {
            null
        },
        onItemClick = { raw -> viewModel.selectBreakdownValue(parameter, raw) },
        emptyMessage = emptyMessageFor(tab),
    )
}

private fun emptyMessageFor(tab: Section<MetricTab>?): String = when (tab) {
    null, Section.Loading -> LOADING_TEXT
    is Section.Failed -> FAILED_TEXT
    is Section.Loaded -> EMPTY_TEXT
}
