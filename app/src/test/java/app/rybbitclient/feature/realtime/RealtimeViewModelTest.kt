package app.rybbitclient.feature.realtime

import app.rybbitclient.core.model.Event
import app.rybbitclient.core.model.EventsResponse
import app.rybbitclient.core.model.OverviewPoint
import app.rybbitclient.core.network.Server
import app.rybbitclient.feature.overview.FakeOverviewAnalytics
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
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/** Realtime polling with virtual time: intervals, feed merging, failures, trend fallback and shutdown. */
@OptIn(ExperimentalCoroutinesApi::class)
class RealtimeViewModelTest {

    private val dispatcher = UnconfinedTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `live count polls every 10 seconds while polling`() = runTest(dispatcher) {
        // Arrange
        val analytics = FakeOverviewAnalytics()
        val viewModel = RealtimeViewModel(SITE_ID, analytics)

        // Act
        viewModel.startPolling()
        val immediate = analytics.liveCalls
        advanceTimeBy(9_999)
        runCurrent()
        val beforeInterval = analytics.liveCalls
        advanceTimeBy(1)
        runCurrent()
        viewModel.stopPolling()

        // Assert
        assertEquals(1, immediate)
        assertEquals(1, beforeInterval)
        assertEquals(2, analytics.liveCalls)
    }

    @Test
    fun `the feed keeps one row per event across overlapping polls`() = runTest(dispatcher) {
        // Arrange
        val analytics = FakeOverviewAnalytics().apply {
            eventsHandler = { EventsResponse(data = listOf(pageview("a", "2026-11-16 08:41:49"), pageview("b", "2026-11-16 08:41:48"))) }
        }
        val viewModel = RealtimeViewModel(SITE_ID, analytics)

        // Act
        viewModel.startPolling()
        advanceTimeBy(REALTIME_POLL_MILLIS)
        runCurrent()
        viewModel.stopPolling()

        // Assert
        val feed = viewModel.uiState.value.feed.orEmpty()
        assertEquals(2, feed.size)
        assertEquals(listOf("a", "b"), feed.map { it.sessionId })
    }

    @Test
    fun `a failed poll keeps the last good data and reports the error`() = runTest(dispatcher) {
        // Arrange
        var failing = false
        val analytics = FakeOverviewAnalytics().apply {
            liveHandler = { 7 }
            eventsHandler = {
                if (failing) throw serverError() else EventsResponse(data = listOf(pageview("a", "2026-11-16 08:41:49")))
            }
        }
        val viewModel = RealtimeViewModel(SITE_ID, analytics)
        viewModel.startPolling()

        // Act
        failing = true
        advanceTimeBy(REALTIME_POLL_MILLIS)
        runCurrent()
        viewModel.stopPolling()

        // Assert
        val state = viewModel.uiState.value
        assertNotNull(state.error)
        assertEquals(7, state.liveCount)
        assertEquals(1, state.feed?.size)
        assertTrue(!state.hasNoData)
    }

    @Test
    fun `a first poll that fails everywhere shows an error with no content`() = runTest(dispatcher) {
        // Arrange
        val analytics = FakeOverviewAnalytics().apply {
            liveHandler = { throw serverError() }
            eventsHandler = { throw serverError() }
        }
        val viewModel = RealtimeViewModel(SITE_ID, analytics)

        // Act
        viewModel.startPolling()
        viewModel.stopPolling()

        // Assert
        val state = viewModel.uiState.value
        assertNotNull(state.error)
        assertTrue(state.hasNoData)
        assertTrue(!state.isInitialLoading)
    }

    @Test
    fun `the sparkline shows the last 30 minutes of sessions`() = runTest(dispatcher) {
        // Arrange
        val analytics = FakeOverviewAnalytics().apply {
            seriesHandler = { _, _ -> (0 until 40).map { minute -> OverviewPoint(time = "t$minute", sessions = minute) } }
        }
        val viewModel = RealtimeViewModel(SITE_ID, analytics)

        // Act
        viewModel.startPolling()
        viewModel.stopPolling()

        // Assert
        val trend = viewModel.uiState.value.trend.orEmpty()
        assertEquals(REALTIME_TREND_POINTS, trend.size)
        assertEquals(39.0, trend.last(), 1e-9)
        assertEquals(10.0, trend.first(), 1e-9)
    }

    @Test
    fun `a failing minute series hides the sparkline without an error`() = runTest(dispatcher) {
        // Arrange
        val analytics = FakeOverviewAnalytics().apply {
            seriesHandler = { _, _ -> throw serverError() }
        }
        val viewModel = RealtimeViewModel(SITE_ID, analytics)

        // Act
        viewModel.startPolling()
        viewModel.stopPolling()

        // Assert
        val state = viewModel.uiState.value
        assertTrue(state.trendUnavailable)
        assertNull(state.trend)
        assertNull(state.error)
    }

    @Test
    fun `the trend refreshes every 60 seconds`() = runTest(dispatcher) {
        // Arrange
        val analytics = FakeOverviewAnalytics()
        val viewModel = RealtimeViewModel(SITE_ID, analytics)

        // Act
        viewModel.startPolling()
        advanceTimeBy(REALTIME_TREND_POLL_MILLIS - 1)
        runCurrent()
        val beforeInterval = analytics.seriesQueries.size
        advanceTimeBy(1)
        runCurrent()
        viewModel.stopPolling()

        // Assert
        assertEquals(1, beforeInterval)
        assertEquals(2, analytics.seriesQueries.size)
    }

    @Test
    fun `stopping the screen stops both loops`() = runTest(dispatcher) {
        // Arrange
        val analytics = FakeOverviewAnalytics()
        val viewModel = RealtimeViewModel(SITE_ID, analytics)
        viewModel.startPolling()

        // Act
        viewModel.stopPolling()
        advanceTimeBy(5 * REALTIME_TREND_POLL_MILLIS)
        runCurrent()

        // Assert
        assertEquals(1, analytics.liveCalls)
        assertEquals(1, analytics.seriesQueries.size)
    }

    @Test
    fun `pull to refresh polls once right away and clears the refreshing flag`() = runTest(dispatcher) {
        // Arrange
        val analytics = FakeOverviewAnalytics()
        val viewModel = RealtimeViewModel(SITE_ID, analytics)

        // Act
        viewModel.refresh()

        // Assert
        assertEquals(1, analytics.liveCalls)
        assertTrue(!viewModel.uiState.value.isRefreshing)
    }

    private fun pageview(session: String, timestamp: String) = Event(
        timestamp = timestamp,
        sessionId = session,
        pathname = "/$session",
        type = "pageview",
        country = "GB",
        browser = "Firefox",
    )

    private fun serverError() = Server(status = 500, code = null, serverMessage = null, resolution = null)

    private companion object {
        const val SITE_ID = "1"
    }
}
