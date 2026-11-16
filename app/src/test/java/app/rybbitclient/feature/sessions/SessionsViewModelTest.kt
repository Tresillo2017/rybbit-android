package app.rybbitclient.feature.sessions

import app.rybbitclient.core.model.SessionSummary
import app.rybbitclient.core.network.FeatureUnavailable
import app.rybbitclient.core.network.Server
import app.rybbitclient.core.query.AnalyticsQuery
import app.rybbitclient.core.query.DateRange
import app.rybbitclient.core.query.Filter
import app.rybbitclient.core.query.FilterType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SessionsViewModelTest {

    private val dispatcher = UnconfinedTestDispatcher()
    private val analytics = FakeAnalyticsRepository()
    private val query = MutableStateFlow(AnalyticsQuery(range = DateRange.Last7Days))

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `loads the first page and reports the end when it is short`() = runTest(dispatcher) {
        // Arrange
        analytics.sessionsPage = { items(count = 3) { session("s$it") } }

        // Act
        val viewModel = SessionsViewModel(SITE_ID, analytics, query)

        // Assert
        val content = viewModel.uiState.value as PagedUiState.Content<SessionSummary>
        assertEquals(3, content.items.size)
        assertFalse(content.hasMore)
        assertEquals(listOf(1), analytics.sessionRequests.map { it.page })
    }

    @Test
    fun `appends the next page on loadMore and stops at a short page`() = runTest(dispatcher) {
        // Arrange
        analytics.sessionsPage = { page ->
            when (page) {
                1 -> items(count = PAGE_SIZE) { session("first-$it") }
                else -> items(count = 3) { session("second-$it") }
            }
        }
        val viewModel = SessionsViewModel(SITE_ID, analytics, query)
        assertTrue((viewModel.uiState.value as PagedUiState.Content<SessionSummary>).hasMore)

        // Act
        viewModel.loadMore()

        // Assert
        val content = viewModel.uiState.value as PagedUiState.Content<SessionSummary>
        assertEquals(PAGE_SIZE + 3, content.items.size)
        assertEquals("second-0", content.items[PAGE_SIZE].sessionId)
        assertFalse(content.hasMore)
        assertFalse(content.isLoadingMore)
        assertEquals(listOf(1, 2), analytics.sessionRequests.map { it.page })
    }

    @Test
    fun `loadMore is a no-op once the end is reached`() = runTest(dispatcher) {
        // Arrange
        analytics.sessionsPage = { items(count = 2) { session("s$it") } }
        val viewModel = SessionsViewModel(SITE_ID, analytics, query)

        // Act
        viewModel.loadMore()

        // Assert
        assertEquals(listOf(1), analytics.sessionRequests.map { it.page })
    }

    @Test
    fun `a failing page 2 keeps page 1 and exposes a retry`() = runTest(dispatcher) {
        // Arrange
        analytics.sessionsPage = { items(count = PAGE_SIZE) { session("s$it") } }
        analytics.failSessions = { page -> if (page == 2) serverError() else null }
        val viewModel = SessionsViewModel(SITE_ID, analytics, query)

        // Act
        viewModel.loadMore()

        // Assert
        val content = viewModel.uiState.value as PagedUiState.Content<SessionSummary>
        assertEquals(PAGE_SIZE, content.items.size)
        assertNotNull(content.loadMoreError)
        assertFalse(content.isLoadingMore)
    }

    @Test
    fun `retrying after a failed page 2 appends it and clears the error`() = runTest(dispatcher) {
        // Arrange
        analytics.sessionsPage = { page -> if (page == 1) items(PAGE_SIZE) { session("s$it") } else items(1) { session("late") } }
        analytics.failSessions = { page -> if (page == 2 && analytics.sessionRequests.count { it.page == 2 } == 1) serverError() else null }
        val viewModel = SessionsViewModel(SITE_ID, analytics, query)
        viewModel.loadMore()

        // Act
        viewModel.loadMore()

        // Assert
        val content = viewModel.uiState.value as PagedUiState.Content<SessionSummary>
        assertEquals(PAGE_SIZE + 1, content.items.size)
        assertEquals(null, content.loadMoreError)
    }

    @Test
    fun `duplicates across pages are dropped`() = runTest(dispatcher) {
        // Arrange: the list shifted, so the second page repeats the last session of the first
        analytics.sessionsPage = { page ->
            if (page == 1) items(PAGE_SIZE) { session("s$it") } else listOf(session("s${PAGE_SIZE - 1}"), session("new"))
        }
        val viewModel = SessionsViewModel(SITE_ID, analytics, query)

        // Act
        viewModel.loadMore()

        // Assert
        val ids = (viewModel.uiState.value as PagedUiState.Content<SessionSummary>).items.map { it.sessionId }
        assertEquals(PAGE_SIZE + 1, ids.size)
        assertEquals(ids.size, ids.toSet().size)
    }

    @Test
    fun `a failing first page shows an error state`() = runTest(dispatcher) {
        // Arrange
        analytics.failSessions = { serverError() }

        // Act
        val viewModel = SessionsViewModel(SITE_ID, analytics, query)

        // Assert
        assertTrue(viewModel.uiState.value is PagedUiState.Error)
    }

    @Test
    fun `a missing endpoint shows the unavailable state, not an error`() = runTest(dispatcher) {
        // Arrange
        analytics.failSessions = { FeatureUnavailable(code = "API_ROUTE_NOT_FOUND", serverMessage = null, resolution = null) }

        // Act
        val viewModel = SessionsViewModel(SITE_ID, analytics, query)

        // Assert
        assertEquals(PagedUiState.Unavailable, viewModel.uiState.value)
    }

    @Test
    fun `changing the shared range reloads page 1 with the new query`() = runTest(dispatcher) {
        // Arrange
        analytics.sessionsPage = { items(count = 2) { session("s$it") } }
        val viewModel = SessionsViewModel(SITE_ID, analytics, query)

        // Act
        query.value = AnalyticsQuery(range = DateRange.Last30Days)

        // Assert
        val last = analytics.sessionRequests.last()
        assertEquals(DateRange.Last30Days, last.query.range)
        assertEquals(1, last.page)
        assertTrue(viewModel.uiState.value is PagedUiState.Content<SessionSummary>)
    }

    @Test
    fun `adding a filter reloads with that filter in the query`() = runTest(dispatcher) {
        // Arrange
        analytics.sessionsPage = { emptyList() }
        SessionsViewModel(SITE_ID, analytics, query)
        val filter = Filter(parameter = "browser", type = FilterType.Equals, values = listOf("Firefox"))

        // Act
        query.value = query.value.copy(filters = listOf(filter))

        // Assert
        assertEquals(listOf(filter), analytics.sessionRequests.last().query.filters)
    }

    @Test
    fun `refresh reloads page 1 and keeps the loaded items on screen`() = runTest(dispatcher) {
        // Arrange
        analytics.sessionsPage = { items(count = 4) { session("s$it") } }
        val viewModel = SessionsViewModel(SITE_ID, analytics, query)

        // Act
        viewModel.refresh()

        // Assert
        val content = viewModel.uiState.value as PagedUiState.Content<SessionSummary>
        assertEquals(4, content.items.size)
        assertFalse(content.isRefreshing)
        assertEquals(2, analytics.sessionRequests.size)
    }

    private fun serverError() = Server(status = 500, code = null, serverMessage = null, resolution = null)

    private companion object {
        const val SITE_ID = "1"
        const val PAGE_SIZE = SessionsViewModel.PAGE_SIZE
    }
}
