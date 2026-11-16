package app.rybbitclient.feature.overview

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.rybbitclient.core.data.AnalyticsRepository
import app.rybbitclient.core.model.MetricPage
import app.rybbitclient.core.model.Overview
import app.rybbitclient.core.model.OverviewPoint
import app.rybbitclient.core.network.ApiException
import app.rybbitclient.core.query.AnalyticsQuery
import app.rybbitclient.core.query.Filter
import app.rybbitclient.core.query.SiteQueryState
import app.rybbitclient.core.query.TimeBucket
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.time.Clock

/** Rows loaded per breakdown tab on the card itself. */
const val OVERVIEW_TAB_LIMIT = 10

/** Rows per page in the "show more" sheet. */
const val OVERVIEW_SHEET_PAGE_SIZE = 50

/** How often the live badge refreshes while the screen is visible. */
const val OVERVIEW_LIVE_POLL_MILLIS = 30_000L

/**
 * ViewModel for [OverviewScreen]. Every query change (range or filter) cancels the loads in flight and
 * reloads the KPIs, trend and the visible breakdown tabs in parallel. Each section keeps its own
 * Loading / Loaded / Failed state, so one failing request never blanks the whole screen.
 */
class OverviewViewModel(
    private val siteId: String,
    private val analytics: AnalyticsRepository,
    private val siteQueryState: SiteQueryState,
    private val clock: Clock = Clock.systemDefaultZone(),
    private val livePollMillis: Long = OVERVIEW_LIVE_POLL_MILLIS,
) : ViewModel() {

    private val mutableState = MutableStateFlow(OverviewUiState(query = siteQueryState.query.value))
    val uiState: StateFlow<OverviewUiState> = mutableState.asStateFlow()

    private val reloadRequests = MutableStateFlow(0)
    private val tabJobs = mutableMapOf<String, Job>()
    private var sheetJob: Job? = null
    private var livePollJob: Job? = null

    init {
        viewModelScope.launch {
            combine(siteQueryState.query, reloadRequests) { query, _ -> query }
                .collectLatest { query -> reloadAll(query) }
        }
    }

    /** Pull-to-refresh: reloads the current query and keeps the content on screen meanwhile. */
    fun refresh() {
        mutableState.update { it.copy(isRefreshing = true) }
        reloadRequests.update { it + 1 }
    }

    /** Starts the live badge loop. Idempotent. Call from the screen's start effect. */
    fun startLivePolling() {
        if (livePollJob?.isActive == true) return
        livePollJob = viewModelScope.launch {
            while (isActive) {
                refreshLive()
                delay(livePollMillis)
            }
        }
    }

    /** Stops the live badge loop while the screen is not visible. */
    fun stopLivePolling() {
        livePollJob?.cancel()
        livePollJob = null
    }

    fun selectKpi(metric: KpiMetric) {
        mutableState.update { it.copy(selectedKpi = metric) }
    }

    fun selectTab(cardId: String, index: Int) {
        val card = BREAKDOWN_CARDS.firstOrNull { it.id == cardId } ?: return
        val parameter = card.parameterAt(index)
        mutableState.update { it.copy(selectedTabs = it.selectedTabs + (cardId to index)) }
        if (mutableState.value.tabs[parameter] is Section.Loaded) return
        launchTabLoad(siteQueryState.query.value, parameter)
    }

    fun selectPreset(key: String) {
        rangeForPreset(key)?.let(siteQueryState::setRange)
    }

    fun selectCustomRange(startMillis: Long, endMillis: Long) {
        siteQueryState.setRange(customRangeFromMillis(startMillis, endMillis))
    }

    /** Adds an equality filter from a tapped row. Blank values can't be filtered on and are ignored. */
    fun selectBreakdownValue(parameter: String, rawValue: String) {
        if (rawValue.isBlank()) return
        siteQueryState.addFilter(equalsFilter(parameter, rawValue))
        closeFullList()
    }

    fun removeFilter(parameter: String) {
        siteQueryState.query.value.filters
            .firstOrNull { it.parameter == parameter }
            ?.let(siteQueryState::removeFilter)
    }

    fun clearFilters() {
        siteQueryState.clearFilters()
    }

    fun openFullList(parameter: String) {
        mutableState.update { it.copy(fullList = FullListState(parameter = parameter)) }
        loadFullListPage(pageNumber = 1)
    }

    fun loadMoreFullList() {
        val list = mutableState.value.fullList ?: return
        if (list.isLoadingMore || !list.hasMore) return
        loadFullListPage(pageNumber = list.page + 1)
    }

    fun closeFullList() {
        sheetJob?.cancel()
        mutableState.update { it.copy(fullList = null) }
    }

    private suspend fun reloadAll(query: AnalyticsQuery) {
        cancelTabLoads()
        val keepContent = mutableState.value.isRefreshing
        mutableState.update { it.resetFor(query, keepContent) }
        coroutineScope {
            launch { loadKpis(query) }
            launch { loadTrend(query) }
            BREAKDOWN_CARDS.forEach { card ->
                val index = mutableState.value.selectedTabs[card.id] ?: 0
                launch { loadTab(query, card.parameterAt(index)) }
            }
        }
        mutableState.update { it.copy(isRefreshing = false) }
    }

    private fun OverviewUiState.resetFor(query: AnalyticsQuery, keepContent: Boolean): OverviewUiState = copy(
        query = query,
        kpis = if (keepContent) kpis else Section.Loading,
        trend = if (keepContent) trend else Section.Loading,
        tabs = if (keepContent) tabs else emptyMap(),
        fullList = null,
        isRefreshing = keepContent,
    )

    private fun cancelTabLoads() {
        tabJobs.values.forEach { it.cancel() }
        tabJobs.clear()
        sheetJob?.cancel()
    }

    private suspend fun loadKpis(query: AnalyticsQuery) {
        val section: Section<KpiData> = try {
            coroutineScope {
                val current = async { analytics.overview(siteId, query) }
                val previous = async { previousOverviewOrNull(query) }
                Section.Loaded(KpiData(current.await(), previous.await()))
            }
        } catch (error: ApiException) {
            Section.Failed(error)
        }
        mutableState.update { it.copy(kpis = section) }
    }

    private suspend fun previousOverviewOrNull(query: AnalyticsQuery): Overview? {
        val previousQuery = previousQueryOf(query) ?: return null
        return try {
            analytics.overview(siteId, previousQuery)
        } catch (error: ApiException) {
            null
        }
    }

    private suspend fun loadTrend(query: AnalyticsQuery) {
        val bucket = query.defaultBucket(clock)
        val section: Section<TrendData> = try {
            coroutineScope {
                val current = async { analytics.overviewTimeSeries(siteId, query, bucket) }
                val previous = async { previousTrendOrEmpty(query, bucket) }
                Section.Loaded(TrendData(bucket, current.await(), previous.await()))
            }
        } catch (error: ApiException) {
            Section.Failed(error)
        }
        mutableState.update { it.copy(trend = section) }
    }

    private suspend fun previousTrendOrEmpty(query: AnalyticsQuery, bucket: TimeBucket): List<OverviewPoint> {
        val previousQuery = previousQueryOf(query) ?: return emptyList()
        return try {
            analytics.overviewTimeSeries(siteId, previousQuery, bucket)
        } catch (error: ApiException) {
            emptyList()
        }
    }

    private fun previousQueryOf(query: AnalyticsQuery): AnalyticsQuery? =
        previousRangeOf(query.range, clock, query.zone)?.let { query.copy(range = it) }

    private fun launchTabLoad(query: AnalyticsQuery, parameter: String) {
        tabJobs[parameter]?.cancel()
        tabJobs[parameter] = viewModelScope.launch { loadTab(query, parameter) }
    }

    private suspend fun loadTab(query: AnalyticsQuery, parameter: String) {
        val section: Section<MetricTab> = try {
            val page = analytics.metric(siteId, query, parameter, limit = OVERVIEW_TAB_LIMIT, page = 1)
            Section.Loaded(MetricTab(rows = page.data, totalCount = page.totalCount))
        } catch (error: ApiException) {
            Section.Failed(error)
        }
        mutableState.update { it.copy(tabs = it.tabs + (parameter to section)) }
    }

    private fun loadFullListPage(pageNumber: Int) {
        val parameter = mutableState.value.fullList?.parameter ?: return
        sheetJob?.cancel()
        mutableState.update { state -> state.copy(fullList = state.fullList?.copy(isLoadingMore = true, error = null)) }
        sheetJob = viewModelScope.launch {
            val query = siteQueryState.query.value
            try {
                val page = analytics.metric(siteId, query, parameter, limit = OVERVIEW_SHEET_PAGE_SIZE, page = pageNumber)
                applyFullListPage(page, pageNumber)
            } catch (error: ApiException) {
                mutableState.update { state ->
                    state.copy(fullList = state.fullList?.copy(isLoadingMore = false, error = error))
                }
            }
        }
    }

    private fun applyFullListPage(page: MetricPage, pageNumber: Int) {
        mutableState.update { state ->
            state.copy(fullList = state.fullList?.let { appendPage(it, page, pageNumber, OVERVIEW_SHEET_PAGE_SIZE) })
        }
    }

    private suspend fun refreshLive() {
        try {
            val count = analytics.liveUserCount(siteId)
            mutableState.update { it.copy(live = Section.Loaded(count)) }
        } catch (error: ApiException) {
            // Keep the last good count on a transient failure; only show a dash when there is none yet.
            mutableState.update { state ->
                if (state.live is Section.Loaded) state else state.copy(live = Section.Failed(error))
            }
        }
    }
}
