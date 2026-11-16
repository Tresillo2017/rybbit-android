package app.rybbitclient.feature.users

import app.rybbitclient.core.model.UserSummary
import app.rybbitclient.core.model.UsersResponse
import app.rybbitclient.core.network.Server
import app.rybbitclient.core.query.AnalyticsQuery
import app.rybbitclient.core.query.DateRange
import app.rybbitclient.feature.sessions.FakeAnalyticsRepository
import app.rybbitclient.feature.sessions.PagedUiState
import app.rybbitclient.feature.sessions.items
import app.rybbitclient.feature.sessions.user
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
class UsersViewModelTest {

    private val dispatcher = UnconfinedTestDispatcher()
    private val analytics = FakeAnalyticsRepository()
    private val query = MutableStateFlow(AnalyticsQuery(range = DateRange.Last30Days))

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `pages in until the total count is reached`() = runTest(dispatcher) {
        // Arrange: 120 users in pages of 50
        analytics.usersPage = { page ->
            val count = if (page < 3) PAGE_SIZE else 20
            UsersResponse(data = items(count) { user("u-$page-$it") }, totalCount = 120, page = page, pageSize = PAGE_SIZE)
        }
        val viewModel = UsersViewModel(SITE_ID, analytics, query, searchDebounceMillis = DEBOUNCE)

        // Act
        viewModel.loadMore()
        val afterPage2 = viewModel.uiState.value as PagedUiState.Content<UserSummary>
        viewModel.loadMore()

        // Assert
        assertTrue(afterPage2.hasMore)
        val content = viewModel.uiState.value as PagedUiState.Content<UserSummary>
        assertEquals(120, content.items.size)
        assertFalse(content.hasMore)
    }

    @Test
    fun `a failing page 2 keeps page 1 and exposes a retry`() = runTest(dispatcher) {
        // Arrange
        analytics.usersPage = { page ->
            if (page == 2) throw serverError()
            UsersResponse(data = items(PAGE_SIZE) { user("u$it") }, totalCount = 500, page = page, pageSize = PAGE_SIZE)
        }
        val viewModel = UsersViewModel(SITE_ID, analytics, query, searchDebounceMillis = DEBOUNCE)

        // Act
        viewModel.loadMore()

        // Assert
        val content = viewModel.uiState.value as PagedUiState.Content<UserSummary>
        assertEquals(PAGE_SIZE, content.items.size)
        assertNotNull(content.loadMoreError)
        assertTrue(content.hasMore)
    }

    @Test
    fun `toggling identified only reloads page 1 with the filter`() = runTest(dispatcher) {
        // Arrange
        analytics.usersPage = { UsersResponse(data = items(1) { user("u$it") }) }
        val viewModel = UsersViewModel(SITE_ID, analytics, query, searchDebounceMillis = DEBOUNCE)

        // Act
        viewModel.setIdentifiedOnly(true)

        // Assert
        val last = analytics.userRequests.last()
        assertTrue(last.identifiedOnly)
        assertEquals(1, last.page)
        assertTrue(viewModel.identifiedOnly.value)
    }

    @Test
    fun `typing waits for the debounce then searches for the trimmed text`() = runTest(dispatcher) {
        // Arrange
        analytics.usersPage = { UsersResponse(data = items(1) { user("u$it") }) }
        val viewModel = UsersViewModel(SITE_ID, analytics, query, searchDebounceMillis = DEBOUNCE)
        assertEquals(1, analytics.userRequests.size)

        // Act
        viewModel.onSearchTextChanged("ab")
        viewModel.onSearchTextChanged("ab ")
        testScheduler.advanceTimeBy(DEBOUNCE - 1)
        testScheduler.runCurrent()
        val beforeDebounce = analytics.userRequests.size
        testScheduler.advanceTimeBy(1)
        testScheduler.runCurrent()

        // Assert
        assertEquals(1, beforeDebounce)
        assertEquals(2, analytics.userRequests.size)
        assertEquals("ab", analytics.userRequests.last().search)
        assertEquals("ab ", viewModel.searchText.value)
    }

    @Test
    fun `an empty search sends no search term`() = runTest(dispatcher) {
        // Arrange
        analytics.usersPage = { UsersResponse(data = emptyList()) }
        val viewModel = UsersViewModel(SITE_ID, analytics, query, searchDebounceMillis = DEBOUNCE)

        // Act
        viewModel.onSearchTextChanged("   ")
        testScheduler.advanceTimeBy(DEBOUNCE)
        testScheduler.runCurrent()

        // Assert
        assertEquals(null, analytics.userRequests.last().search)
    }

    @Test
    fun `changing the shared range reloads page 1`() = runTest(dispatcher) {
        // Arrange
        analytics.usersPage = { UsersResponse(data = items(1) { user("u$it") }) }
        UsersViewModel(SITE_ID, analytics, query, searchDebounceMillis = DEBOUNCE)

        // Act
        query.value = AnalyticsQuery(range = DateRange.Last7Days)

        // Assert
        assertEquals(DateRange.Last7Days, analytics.userRequests.last().query.range)
        assertEquals(1, analytics.userRequests.last().page)
    }

    @Test
    fun `an empty page never reports more`() {
        assertFalse(UsersViewModel.hasMoreUsers(page = 1, response = UsersResponse(data = emptyList(), totalCount = 10)))
    }

    @Test
    fun `without a total a full page suggests more`() {
        val full = UsersResponse(data = items(PAGE_SIZE) { user("u$it") })
        val short = UsersResponse(data = items(3) { user("u$it") })
        assertTrue(UsersViewModel.hasMoreUsers(page = 1, response = full))
        assertFalse(UsersViewModel.hasMoreUsers(page = 1, response = short))
    }

    private fun serverError() = Server(status = 500, code = null, serverMessage = null, resolution = null)

    private companion object {
        const val SITE_ID = "1"
        const val PAGE_SIZE = UsersViewModel.PAGE_SIZE
        const val DEBOUNCE = 300L
    }
}
