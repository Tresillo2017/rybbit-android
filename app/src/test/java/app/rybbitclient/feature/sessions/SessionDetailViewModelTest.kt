package app.rybbitclient.feature.sessions

import app.rybbitclient.core.model.SessionDetail
import app.rybbitclient.core.model.SessionDetailEvent
import app.rybbitclient.core.model.SessionDetailResponse
import app.rybbitclient.core.model.SessionPagination
import app.rybbitclient.core.network.FeatureUnavailable
import app.rybbitclient.core.network.Server
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.ZoneId

@OptIn(ExperimentalCoroutinesApi::class)
class SessionDetailViewModelTest {

    private val dispatcher = UnconfinedTestDispatcher()
    private val analytics = FakeAnalyticsRepository()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `loads the header and a timeline in the given zone`() = runTest(dispatcher) {
        // Arrange
        analytics.eventsPage = { _, _ ->
            response(events = listOf(event("2026-11-16 08:41:49.074"), event("2026-11-16 08:41:59.074")), hasMore = false)
        }

        // Act
        val viewModel = SessionDetailViewModel(SITE_ID, SESSION_ID, analytics, zone = MADRID)

        // Assert
        val content = viewModel.uiState.value as SessionDetailUiState.Content
        assertEquals(SESSION_ID, content.session?.sessionId)
        assertEquals(listOf("09:41:49", "09:41:59"), content.timeline.map { it.timeLabel })
        assertEquals(listOf(null, "+10s"), content.timeline.map { it.sinceLabel })
        assertFalse(content.hasMore)
    }

    @Test
    fun `loadMore appends the next page from the current event offset`() = runTest(dispatcher) {
        // Arrange
        val offsets = mutableListOf<Int>()
        analytics.eventsPage = { offset, _ ->
            offsets += offset
            if (offset == 0) response(events = items(PAGE_SIZE) { event("2026-11-16 08:00:%02d".format(it % 60)) }, hasMore = true)
            else response(events = items(2) { event("2026-11-16 09:00:0$it") }, hasMore = false)
        }
        val viewModel = SessionDetailViewModel(SITE_ID, SESSION_ID, analytics, zone = MADRID)

        // Act
        viewModel.loadMore()

        // Assert
        val content = viewModel.uiState.value as SessionDetailUiState.Content
        assertEquals(listOf(0, PAGE_SIZE), offsets)
        assertEquals(PAGE_SIZE + 2, content.events.size)
        assertEquals(PAGE_SIZE + 2, content.timeline.size)
        assertFalse(content.hasMore)
        assertFalse(content.isLoadingMore)
    }

    @Test
    fun `a failing page 2 keeps the events already loaded`() = runTest(dispatcher) {
        // Arrange
        analytics.eventsPage = { offset, _ ->
            if (offset == 0) response(events = items(PAGE_SIZE) { event("2026-11-16 08:00:00") }, hasMore = true)
            else throw serverError()
        }
        val viewModel = SessionDetailViewModel(SITE_ID, SESSION_ID, analytics, zone = MADRID)

        // Act
        viewModel.loadMore()

        // Assert
        val content = viewModel.uiState.value as SessionDetailUiState.Content
        assertEquals(PAGE_SIZE, content.events.size)
        assertEquals(PAGE_SIZE, content.timeline.size)
        assertTrue(content.loadMoreError != null)
        assertTrue(content.hasMore)
    }

    @Test
    fun `a failing first page shows an error state`() = runTest(dispatcher) {
        // Arrange
        analytics.eventsPage = { _, _ -> throw serverError() }

        // Act
        val viewModel = SessionDetailViewModel(SITE_ID, SESSION_ID, analytics, zone = MADRID)

        // Assert
        assertTrue(viewModel.uiState.value is SessionDetailUiState.Error)
    }

    @Test
    fun `a missing endpoint shows the unavailable state`() = runTest(dispatcher) {
        // Arrange
        analytics.eventsPage = { _, _ -> throw FeatureUnavailable(code = "API_ROUTE_NOT_FOUND", serverMessage = null, resolution = null) }

        // Act
        val viewModel = SessionDetailViewModel(SITE_ID, SESSION_ID, analytics, zone = MADRID)

        // Assert
        assertEquals(SessionDetailUiState.Unavailable, viewModel.uiState.value)
    }

    @Test
    fun `without pagination a full page is taken to mean more events follow`() = runTest(dispatcher) {
        // Arrange
        val full = SessionDetailResponse(
            session = SessionDetail(sessionId = SESSION_ID),
            events = items(PAGE_SIZE) { event("2026-11-16 08:00:00") },
            pagination = null,
        )
        analytics.eventsPage = { _, _ -> full }

        // Act
        val viewModel = SessionDetailViewModel(SITE_ID, SESSION_ID, analytics, zone = MADRID)

        // Assert
        assertTrue((viewModel.uiState.value as SessionDetailUiState.Content).hasMore)
    }

    @Test
    fun `refresh reloads from the first event`() = runTest(dispatcher) {
        // Arrange
        val offsets = mutableListOf<Int>()
        analytics.eventsPage = { offset, _ ->
            offsets += offset
            response(events = listOf(event("2026-11-16 08:00:00")), hasMore = false)
        }
        val viewModel = SessionDetailViewModel(SITE_ID, SESSION_ID, analytics, zone = MADRID)

        // Act
        viewModel.refresh()

        // Assert
        assertEquals(listOf(0, 0), offsets)
        assertNull((viewModel.uiState.value as SessionDetailUiState.Content).loadMoreError)
    }

    private fun response(events: List<SessionDetailEvent>, hasMore: Boolean) = SessionDetailResponse(
        session = SessionDetail(sessionId = SESSION_ID, userId = "device-1"),
        events = events,
        pagination = SessionPagination(total = events.size, limit = PAGE_SIZE, offset = 0, hasMore = hasMore),
    )

    private fun event(timestamp: String) = SessionDetailEvent(timestamp = timestamp, pathname = "/", type = "pageview")

    private fun serverError() = Server(status = 500, code = null, serverMessage = null, resolution = null)

    private companion object {
        const val SITE_ID = "1"
        const val SESSION_ID = "session-1"
        const val PAGE_SIZE = SessionDetailViewModel.PAGE_SIZE
        val MADRID: ZoneId = ZoneId.of("Europe/Madrid")
    }
}
