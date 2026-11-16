package app.rybbitclient.feature.events

import app.rybbitclient.core.model.Event
import app.rybbitclient.core.model.EventCursor
import app.rybbitclient.core.model.EventName
import app.rybbitclient.core.model.EventsResponse
import app.rybbitclient.core.network.FeatureUnavailable
import app.rybbitclient.core.network.Server
import app.rybbitclient.core.query.DateRange
import app.rybbitclient.core.query.SiteQueryState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class EventsViewModelTest {

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
    fun `loads event names and the first page of recent events`() = runTest(dispatcher) {
        // Arrange
        val analytics = FakeAnalyticsRepository().apply {
            eventNamesResult = listOf(EventName("signup_click", 28))
            eventPages = mapOf(null to EventsResponse(data = listOf(customEvent("signup_click"))))
        }

        // Act
        val viewModel = EventsViewModel(SITE_ID, analytics, SiteQueryState())

        // Assert
        val content = contentOf(viewModel)
        assertEquals(listOf(EventName("signup_click", 28)), content.names)
        assertEquals(listOf(customEvent("signup_click")), content.stream.events)
        assertFalse(content.stream.hasMore)
    }

    @Test
    fun `load more appends the older page and stops at the last cursor`() = runTest(dispatcher) {
        // Arrange
        val analytics = FakeAnalyticsRepository().apply {
            eventPages = mapOf(
                null to EventsResponse(
                    data = listOf(customEvent("first")),
                    cursor = EventCursor(hasMore = true, oldestCursor = "cursor-1"),
                ),
                "cursor-1" to EventsResponse(
                    data = listOf(pageview()),
                    cursor = EventCursor(hasMore = false),
                ),
            )
        }
        val viewModel = EventsViewModel(SITE_ID, analytics, SiteQueryState())

        // Act
        viewModel.loadMore()

        // Assert
        val stream = contentOf(viewModel).stream
        assertEquals(listOf(customEvent("first"), pageview()), stream.events)
        assertFalse(stream.hasMore)
        assertEquals(listOf<String?>(null, "cursor-1"), analytics.eventCursors)
    }

    @Test
    fun `load more does nothing when the server has no older rows`() = runTest(dispatcher) {
        // Arrange
        val analytics = FakeAnalyticsRepository().apply {
            eventPages = mapOf(null to EventsResponse(data = listOf(customEvent("only")), cursor = EventCursor(hasMore = false)))
        }
        val viewModel = EventsViewModel(SITE_ID, analytics, SiteQueryState())

        // Act
        viewModel.loadMore()

        // Assert
        assertEquals(listOf<String?>(null), analytics.eventCursors)
    }

    @Test
    fun `a failed older page keeps the loaded rows and records the error`() = runTest(dispatcher) {
        // Arrange
        val analytics = FakeAnalyticsRepository().apply {
            eventPages = mapOf(
                null to EventsResponse(data = listOf(customEvent("first")), cursor = EventCursor(hasMore = true, oldestCursor = "cursor-1")),
            )
        }
        val viewModel = EventsViewModel(SITE_ID, analytics, SiteQueryState())
        analytics.failure = serverError()

        // Act
        viewModel.loadMore()

        // Assert
        val stream = contentOf(viewModel).stream
        assertEquals(listOf(customEvent("first")), stream.events)
        assertTrue(stream.loadMoreError is Server)
        assertTrue(stream.hasMore)
        assertFalse(stream.isLoadingMore)
    }

    @Test
    fun `changing the shared site query reloads with the new range`() = runTest(dispatcher) {
        // Arrange
        val queryState = SiteQueryState()
        val analytics = FakeAnalyticsRepository()
        val viewModel = EventsViewModel(SITE_ID, analytics, queryState)

        // Act
        queryState.setRange(DateRange.Last30Days)

        // Assert
        assertEquals(DateRange.Last30Days, analytics.eventQueries.last().range)
        assertEquals(DateRange.Last30Days, contentOf(viewModel).query.range)
    }

    @Test
    fun `type chips filter loaded rows on the client and survive a refresh`() = runTest(dispatcher) {
        // Arrange
        val analytics = FakeAnalyticsRepository().apply {
            eventPages = mapOf(null to EventsResponse(data = listOf(customEvent("signup"), pageview())))
        }
        val viewModel = EventsViewModel(SITE_ID, analytics, SiteQueryState())

        // Act
        viewModel.selectType(EventTypeFilter.Custom)
        viewModel.refresh()

        // Assert
        val content = contentOf(viewModel)
        assertEquals(EventTypeFilter.Custom, content.typeFilter)
        assertEquals(listOf(customEvent("signup")), filterEvents(content.stream.events, content.typeFilter))
        assertEquals(2, analytics.eventCursors.size)
    }

    @Test
    fun `shows the error state when the first load fails`() = runTest(dispatcher) {
        // Arrange
        val analytics = FakeAnalyticsRepository().apply { failure = serverError() }

        // Act
        val viewModel = EventsViewModel(SITE_ID, analytics, SiteQueryState())

        // Assert
        assertTrue(viewModel.uiState.value is EventsUiState.Error)
    }

    @Test
    fun `reports unavailable instead of an error when the route is missing`() = runTest(dispatcher) {
        // Arrange
        val analytics = FakeAnalyticsRepository().apply {
            failure = FeatureUnavailable(code = "API_ROUTE_NOT_FOUND", serverMessage = null, resolution = null)
        }

        // Act
        val viewModel = EventsViewModel(SITE_ID, analytics, SiteQueryState())

        // Assert
        assertEquals(EventsUiState.Unavailable, viewModel.uiState.value)
    }

    @Test
    fun `refresh after an error reloads and returns to content`() = runTest(dispatcher) {
        // Arrange
        val analytics = FakeAnalyticsRepository().apply { failure = serverError() }
        val viewModel = EventsViewModel(SITE_ID, analytics, SiteQueryState())
        analytics.failure = null

        // Act
        viewModel.refresh()

        // Assert
        assertTrue(viewModel.uiState.value is EventsUiState.Content)
    }

    private fun contentOf(viewModel: EventsViewModel): EventsUiState.Content =
        viewModel.uiState.value as? EventsUiState.Content ?: error("Expected Content but was ${viewModel.uiState.value}")

    private fun customEvent(name: String) = Event(
        timestamp = "2026-11-16 08:41:49",
        eventName = name,
        type = "custom_event",
        sessionId = "session-$name",
    )

    private fun pageview() = Event(timestamp = "2026-11-16 08:41:48", pathname = "/signup", type = "pageview")

    private fun serverError() = Server(status = 500, code = null, serverMessage = null, resolution = null)

    private companion object {
        const val SITE_ID = "1"
    }
}
