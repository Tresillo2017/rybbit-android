package app.rybbitclient.feature.overview

import app.rybbitclient.core.model.MetricPage
import app.rybbitclient.core.network.Server
import app.rybbitclient.core.query.AnalyticsQuery
import app.rybbitclient.core.query.DateRange
import app.rybbitclient.core.query.SiteQueryState
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

/** ViewModel behaviour for the Overview: deltas, KPI selection, lazy tabs, paging, filters, polling. */
@OptIn(ExperimentalCoroutinesApi::class)
class OverviewViewModelTest {

    private val dispatcher = UnconfinedTestDispatcher()
    private val clock = Clock.fixed(Instant.parse("2026-11-16T12:00:00Z"), ZoneOffset.UTC)

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `KPIs carry the percent change against the previous period`() = runTest(dispatcher) {
        // Arrange
        val analytics = FakeOverviewAnalytics().apply {
            overviewHandler = { query -> if (query.range.isComparison()) overviewOf(users = 100) else overviewOf(users = 120) }
        }

        // Act
        val viewModel = viewModel(analytics, sevenDays())

        // Assert
        val kpis = (viewModel.uiState.value.kpis as Section.Loaded).value
        val visitors = kpiItems(kpis).first { it.key == KpiMetric.Visitors.name }
        assertEquals(20.0, visitors.deltaPercent!!, 1e-9)
        assertEquals(2, analytics.overviewQueries.size)
    }

    @Test
    fun `all time has no previous request and no deltas`() = runTest(dispatcher) {
        // Arrange
        val analytics = FakeOverviewAnalytics()
        val state = SiteQueryState(AnalyticsQuery(DateRange.AllTime, zone = ZoneOffset.UTC))

        // Act
        val viewModel = viewModel(analytics, state)

        // Assert
        assertEquals(1, analytics.overviewQueries.size)
        val kpis = (viewModel.uiState.value.kpis as Section.Loaded).value
        assertNull(kpis.previous)
        assertTrue(kpiItems(kpis).all { it.deltaPercent == null })
    }

    @Test
    fun `a failed previous-period request hides deltas but keeps the KPIs`() = runTest(dispatcher) {
        // Arrange
        val analytics = FakeOverviewAnalytics().apply {
            overviewHandler = { query -> if (query.range.isComparison()) throw serverError() else overviewOf(users = 50) }
        }

        // Act
        val viewModel = viewModel(analytics, sevenDays())

        // Assert
        val kpis = viewModel.uiState.value.kpis
        assertTrue(kpis is Section.Loaded)
        assertTrue(kpiItems((kpis as Section.Loaded).value).all { it.deltaPercent == null })
    }

    @Test
    fun `a failed KPI request leaves the breakdown cards working`() = runTest(dispatcher) {
        // Arrange
        val analytics = FakeOverviewAnalytics().apply {
            overviewHandler = { throw serverError() }
            metricHandler = { MetricPage(listOf(metricRow("/home", 4)), totalCount = 1) }
        }

        // Act
        val viewModel = viewModel(analytics, sevenDays())

        // Assert
        assertTrue(viewModel.uiState.value.kpis is Section.Failed)
        assertTrue(viewModel.uiState.value.tabs["pathname"] is Section.Loaded)
    }

    @Test
    fun `selecting a KPI re-derives the chart without any new request`() = runTest(dispatcher) {
        // Arrange
        val analytics = FakeOverviewAnalytics()
        val viewModel = viewModel(analytics, sevenDays())
        val requestsBefore = analytics.overviewQueries.size + analytics.seriesQueries.size + analytics.metricCalls.size

        // Act
        viewModel.selectKpi(KpiMetric.Pageviews)

        // Assert
        assertEquals(KpiMetric.Pageviews, viewModel.uiState.value.selectedKpi)
        assertEquals(requestsBefore, analytics.overviewQueries.size + analytics.seriesQueries.size + analytics.metricCalls.size)
    }

    @Test
    fun `loads the first tab of every card and each other tab only once`() = runTest(dispatcher) {
        // Arrange
        val analytics = FakeOverviewAnalytics().apply {
            metricHandler = { MetricPage(listOf(metricRow("/home", 4)), totalCount = 1) }
        }
        val viewModel = viewModel(analytics, sevenDays())
        assertEquals(
            listOf("pathname", "referrer", "country", "browser", "language"),
            analytics.metricCalls.map { it.parameter },
        )

        // Act
        viewModel.selectTab("pages", 1)
        viewModel.selectTab("pages", 1)
        viewModel.selectTab("pages", 0)

        // Assert
        assertEquals(6, analytics.metricCalls.size)
        assertEquals("page_title", analytics.metricCalls.last().parameter)
        assertTrue(analytics.metricCalls.all { it.limit == OVERVIEW_TAB_LIMIT && it.page == 1 })
        assertEquals(0, viewModel.uiState.value.selectedTabs["pages"])
    }

    @Test
    fun `tapping a row adds an equality filter that reloads every section`() = runTest(dispatcher) {
        // Arrange
        val analytics = FakeOverviewAnalytics()
        val state = SiteQueryState(sevenDaysQuery())
        val viewModel = viewModel(analytics, state)
        val overviewCallsBefore = analytics.overviewQueries.size

        // Act
        viewModel.selectBreakdownValue("browser", "Firefox")

        // Assert
        assertEquals(listOf(equalsFilter("browser", "Firefox")), state.query.value.filters)
        assertTrue(analytics.overviewQueries.size > overviewCallsBefore)
        assertEquals(listOf(equalsFilter("browser", "Firefox")), analytics.overviewQueries.last().filters)
    }

    @Test
    fun `a blank row value is not turned into a filter`() = runTest(dispatcher) {
        // Arrange
        val state = SiteQueryState(sevenDaysQuery())
        val viewModel = viewModel(FakeOverviewAnalytics(), state)

        // Act
        viewModel.selectBreakdownValue("country", "")

        // Assert
        assertTrue(state.query.value.filters.isEmpty())
    }

    @Test
    fun `removing a filter restores the unfiltered query`() = runTest(dispatcher) {
        // Arrange
        val analytics = FakeOverviewAnalytics()
        val state = SiteQueryState(sevenDaysQuery())
        val viewModel = viewModel(analytics, state)
        viewModel.selectBreakdownValue("browser", "Firefox")

        // Act
        viewModel.removeFilter("browser")

        // Assert
        assertTrue(state.query.value.filters.isEmpty())
        assertTrue(analytics.overviewQueries.last().filters.isEmpty())
    }

    @Test
    fun `show more pages through the full list until the total count is reached`() = runTest(dispatcher) {
        // Arrange
        val analytics = FakeOverviewAnalytics().apply {
            metricHandler = { call ->
                when (call.page) {
                    1 -> MetricPage(rows("/p", 50), totalCount = 60)
                    else -> MetricPage(rows("/q", 10), totalCount = 60)
                }
            }
        }
        val viewModel = viewModel(analytics, sevenDays())

        // Act
        viewModel.openFullList("pathname")
        val afterFirstPage = viewModel.uiState.value.fullList!!
        viewModel.loadMoreFullList()
        viewModel.loadMoreFullList()

        // Assert
        assertEquals(50, afterFirstPage.rows.size)
        assertTrue(afterFirstPage.hasMore)
        val list = viewModel.uiState.value.fullList!!
        assertEquals(60, list.rows.size)
        assertFalse(list.hasMore)
        val fullListCalls = analytics.metricCalls.filter { it.limit == OVERVIEW_SHEET_PAGE_SIZE }
        assertEquals(listOf(1, 2), fullListCalls.map { it.page })
    }

    @Test
    fun `a failed page keeps the loaded rows and retry requests the same page`() = runTest(dispatcher) {
        // Arrange
        var failSecondPage = true
        val analytics = FakeOverviewAnalytics().apply {
            metricHandler = { call ->
                when {
                    call.limit != OVERVIEW_SHEET_PAGE_SIZE -> MetricPage(rows("/tab", 1), totalCount = 1)
                    call.page == 1 -> MetricPage(rows("/p", 50), totalCount = 80)
                    failSecondPage -> throw serverError()
                    else -> MetricPage(rows("/q", 30), totalCount = 80)
                }
            }
        }
        val viewModel = viewModel(analytics, sevenDays())
        viewModel.openFullList("pathname")

        // Act
        viewModel.loadMoreFullList()
        val failed = viewModel.uiState.value.fullList!!
        failSecondPage = false
        viewModel.loadMoreFullList()

        // Assert
        assertTrue(failed.error != null)
        assertEquals(50, failed.rows.size)
        assertEquals(80, viewModel.uiState.value.fullList!!.rows.size)
        assertEquals(listOf(1, 2, 2), analytics.metricCalls.filter { it.limit == OVERVIEW_SHEET_PAGE_SIZE }.map { it.page })
    }

    @Test
    fun `a slow result from an earlier query never replaces the newer one`() = runTest(dispatcher) {
        // Arrange
        val slow = CompletableDeferred<MetricPage>()
        val analytics = FakeOverviewAnalytics().apply {
            metricHandler = { call ->
                if (call.query.range == DateRange.Last7Days && call.parameter == "pathname") {
                    slow.await()
                } else {
                    MetricPage(listOf(metricRow("/new", 3)), totalCount = 1)
                }
            }
        }
        val state = SiteQueryState(sevenDaysQuery())
        val viewModel = viewModel(analytics, state)

        // Act
        state.setRange(DateRange.Last30Days)
        slow.complete(MetricPage(listOf(metricRow("/stale", 99)), totalCount = 1))

        // Assert
        val tab = viewModel.uiState.value.tabs["pathname"] as Section.Loaded
        assertEquals("/new", tab.value.rows.single().value)
    }

    @Test
    fun `a range preset change reloads with the new window`() = runTest(dispatcher) {
        // Arrange
        val analytics = FakeOverviewAnalytics()
        val viewModel = viewModel(analytics, sevenDays())

        // Act
        viewModel.selectPreset("30d")

        // Assert
        assertEquals(DateRange.Last30Days, analytics.overviewQueries.last { !it.range.isComparison() }.range)
    }

    @Test
    fun `live count refreshes every 30 seconds while polling and stops when stopped`() = runTest(dispatcher) {
        // Arrange
        val analytics = FakeOverviewAnalytics()
        val viewModel = viewModel(analytics, sevenDays())

        // Act
        viewModel.startLivePolling()
        val immediate = analytics.liveCalls
        advanceTimeBy(29_999)
        val beforeInterval = analytics.liveCalls
        advanceTimeBy(1)
        runCurrent()
        viewModel.stopLivePolling()
        advanceTimeBy(120_000)
        runCurrent()

        // Assert
        assertEquals(1, immediate)
        assertEquals(1, beforeInterval)
        assertEquals(2, analytics.liveCalls)
    }

    @Test
    fun `a transient live-count failure keeps the last good count`() = runTest(dispatcher) {
        // Arrange
        var fail = false
        val analytics = FakeOverviewAnalytics().apply {
            liveHandler = { if (fail) throw serverError() else 42 }
        }
        val viewModel = viewModel(analytics, sevenDays())
        viewModel.startLivePolling()

        // Act
        fail = true
        advanceTimeBy(30_000)
        runCurrent()
        viewModel.stopLivePolling()

        // Assert
        assertEquals(Section.Loaded(42), viewModel.uiState.value.live)
    }

    @Test
    fun `refresh reloads the current query and keeps the content on screen`() = runTest(dispatcher) {
        // Arrange
        val analytics = FakeOverviewAnalytics()
        val viewModel = viewModel(analytics, sevenDays())
        val before = analytics.overviewQueries.size

        // Act
        viewModel.refresh()

        // Assert
        assertTrue(analytics.overviewQueries.size > before)
        assertFalse(viewModel.uiState.value.isRefreshing)
        assertTrue(viewModel.uiState.value.kpis is Section.Loaded)
    }

    private fun viewModel(analytics: FakeOverviewAnalytics, state: SiteQueryState) =
        OverviewViewModel(SITE_ID, analytics, state, clock = clock, livePollMillis = LIVE_POLL_MILLIS)

    private fun sevenDays() = SiteQueryState(sevenDaysQuery())

    private fun sevenDaysQuery() = AnalyticsQuery(DateRange.Last7Days, zone = ZoneOffset.UTC)

    private fun rows(prefix: String, count: Int) =
        (1..count).map { metricRow("$prefix$it", count = count - it + 1) }

    private fun serverError() = Server(status = 500, code = null, serverMessage = null, resolution = null)

    private companion object {
        const val SITE_ID = "1"
        const val LIVE_POLL_MILLIS = 30_000L
    }
}

/** A comparison-period query: a calendar window or an elapsed-minutes window ending before the current one. */
private fun DateRange.isComparison(): Boolean = this is DateRange.Custom || this is DateRange.ElapsedWindow
