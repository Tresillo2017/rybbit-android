package app.rybbitclient.feature.events

import app.rybbitclient.core.model.Event
import app.rybbitclient.core.model.EventName
import app.rybbitclient.core.model.EventPropertyCount
import app.rybbitclient.core.model.EventTimePoint
import app.rybbitclient.core.model.EventsResponse
import app.rybbitclient.core.network.FeatureUnavailable
import app.rybbitclient.core.network.Server
import app.rybbitclient.core.query.Filter
import app.rybbitclient.core.query.FilterType
import app.rybbitclient.core.query.SiteQueryState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class EventDetailViewModelTest {

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
    fun `reads the total, groups properties by key and skips blank keys`() = runTest(dispatcher) {
        // Arrange
        val analytics = FakeAnalyticsRepository().apply {
            eventNamesResult = listOf(EventName("signup_click", 28), EventName("download", 3))
            propertyRows = listOf(
                EventPropertyCount("plan", "pro", 16),
                EventPropertyCount("plan", "free", 12),
                EventPropertyCount("", "ignored", 1),
                EventPropertyCount("region", "EU", 5),
            )
        }

        // Act
        val viewModel = EventDetailViewModel(SITE_ID, EVENT_NAME, analytics, SiteQueryState())

        // Assert
        val content = contentOf(viewModel)
        assertEquals(28, content.totalCount)
        assertEquals(listOf("plan", "region"), content.propertyGroups.map { it.key })
        assertEquals(listOf("pro" to 16, "free" to 12), content.propertyGroups.first().values.map { it.label to it.count })
        assertEquals(listOf(EVENT_NAME), analytics.propertyEventNames)
    }

    @Test
    fun `keeps only this event's occurrences and narrows the query to its sessions`() = runTest(dispatcher) {
        // Arrange
        val analytics = FakeAnalyticsRepository().apply {
            eventPages = mapOf(
                null to EventsResponse(data = listOf(customEvent(EVENT_NAME), customEvent("download"), pageview())),
            )
        }

        // Act
        val viewModel = EventDetailViewModel(SITE_ID, EVENT_NAME, analytics, SiteQueryState())

        // Assert
        val content = contentOf(viewModel)
        assertEquals(listOf(customEvent(EVENT_NAME)), content.occurrences)
        assertEquals(
            listOf(Filter(parameter = "event_name", type = FilterType.Equals, values = listOf(EVENT_NAME))),
            analytics.eventQueries.last().filters,
        )
    }

    @Test
    fun `keeps only this event's points in the count over time`() = runTest(dispatcher) {
        // Arrange
        val analytics = FakeAnalyticsRepository().apply {
            eventTimeSeriesResult = listOf(
                EventTimePoint(time = "2026-11-16 00:00:00", eventName = EVENT_NAME, eventCount = 28),
                EventTimePoint(time = "2026-11-16 00:00:00", eventName = "download", eventCount = 3),
            )
        }

        // Act
        val viewModel = EventDetailViewModel(SITE_ID, EVENT_NAME, analytics, SiteQueryState())

        // Assert
        assertEquals(listOf(EVENT_NAME), contentOf(viewModel).points.map { it.eventName })
    }

    @Test
    fun `shows the error state when a section fails`() = runTest(dispatcher) {
        // Arrange
        val analytics = FakeAnalyticsRepository().apply { failure = Server(status = 500, code = null, serverMessage = null, resolution = null) }

        // Act
        val viewModel = EventDetailViewModel(SITE_ID, EVENT_NAME, analytics, SiteQueryState())

        // Assert
        assertTrue(viewModel.uiState.value is EventDetailUiState.Error)
    }

    @Test
    fun `reports unavailable when the server lacks the route`() = runTest(dispatcher) {
        // Arrange
        val analytics = FakeAnalyticsRepository().apply {
            failure = FeatureUnavailable(code = "API_ROUTE_NOT_FOUND", serverMessage = null, resolution = null)
        }

        // Act
        val viewModel = EventDetailViewModel(SITE_ID, EVENT_NAME, analytics, SiteQueryState())

        // Assert
        assertEquals(EventDetailUiState.Unavailable, viewModel.uiState.value)
    }

    private fun contentOf(viewModel: EventDetailViewModel): EventDetailUiState.Content =
        viewModel.uiState.value as? EventDetailUiState.Content ?: error("Expected Content but was ${viewModel.uiState.value}")

    private fun customEvent(name: String) = Event(
        timestamp = "2026-11-16 08:41:49",
        eventName = name,
        type = "custom_event",
        sessionId = "session-$name",
    )

    private fun pageview() = Event(timestamp = "2026-11-16 08:41:48", pathname = "/signup", type = "pageview")

    private companion object {
        const val SITE_ID = "1"
        const val EVENT_NAME = "signup_click"
    }
}
