package app.rybbitclient.feature.overview

import app.rybbitclient.core.model.MetricRow
import app.rybbitclient.core.model.Overview
import app.rybbitclient.core.model.OverviewPoint
import app.rybbitclient.core.network.ApiException
import app.rybbitclient.core.query.AnalyticsQuery
import app.rybbitclient.core.query.TimeBucket

/** One independently loaded piece of the Overview. A failure stays inside its own section. */
sealed interface Section<out T> {
    data object Loading : Section<Nothing>
    data class Loaded<out T>(val value: T) : Section<T>
    data class Failed(val error: ApiException) : Section<Nothing>
}

/** The current period and, when the range has one, the equal-length period before it. */
data class KpiData(val current: Overview, val previous: Overview?)

/** Time series for the trend chart. [previous] is empty when there is no comparison window. */
data class TrendData(
    val bucket: TimeBucket,
    val current: List<OverviewPoint>,
    val previous: List<OverviewPoint>,
)

/** Loaded rows of one breakdown tab (first page, limited). */
data class MetricTab(val rows: List<MetricRow>, val totalCount: Int?)

/** State of the "show more" bottom sheet for one breakdown parameter. */
data class FullListState(
    val parameter: String,
    val rows: List<MetricRow> = emptyList(),
    val totalCount: Int? = null,
    val page: Int = 0,
    val hasMore: Boolean = true,
    val isLoadingMore: Boolean = false,
    val error: ApiException? = null,
)

/** Everything [OverviewScreen] renders. Immutable: the ViewModel replaces it with `copy`. */
data class OverviewUiState(
    val query: AnalyticsQuery,
    val kpis: Section<KpiData> = Section.Loading,
    val trend: Section<TrendData> = Section.Loading,
    val selectedKpi: KpiMetric = KpiMetric.Visitors,
    val live: Section<Int> = Section.Loading,
    /** Selected tab index per breakdown card, keyed by [BreakdownCardSpec.id]. */
    val selectedTabs: Map<String, Int> = emptyMap(),
    /** Loaded tab data keyed by metric parameter. A missing key means the tab is still loading. */
    val tabs: Map<String, Section<MetricTab>> = emptyMap(),
    val fullList: FullListState? = null,
    val isRefreshing: Boolean = false,
)
