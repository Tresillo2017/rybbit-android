package app.rybbitclient.feature.users

import app.rybbitclient.core.model.UserDetail
import app.rybbitclient.core.model.SessionSummary
import app.rybbitclient.core.network.FeatureUnavailable
import app.rybbitclient.core.network.Server
import app.rybbitclient.core.query.AnalyticsQuery
import app.rybbitclient.core.query.DateRange
import app.rybbitclient.feature.sessions.FakeAnalyticsRepository
import app.rybbitclient.feature.sessions.PagedUiState
import app.rybbitclient.feature.sessions.items
import app.rybbitclient.feature.sessions.session
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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class UserDetailViewModelTest {

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
    fun `loads the profile and the user's sessions in the shared range`() = runTest(dispatcher) {
        // Arrange
        analytics.userDetailResult = { UserDetail(userId = USER_ID, sessions = 2) }
        analytics.userSessionsPage = { items(2) { session("s$it") } }

        // Act
        val viewModel = UserDetailViewModel(SITE_ID, USER_ID, analytics, query)

        // Assert
        val profile = viewModel.profileState.value as UserProfileState.Loaded
        assertEquals(USER_ID, profile.profile.userId)
        val sessions = viewModel.uiState.value as PagedUiState.Content<SessionSummary>
        assertEquals(2, sessions.items.size)
        val request = analytics.userSessionRequests.single()
        assertEquals(USER_ID, request.userId)
        assertEquals(DateRange.Last30Days, request.query.range)
    }

    @Test
    fun `a failing profile is an error while the sessions still load`() = runTest(dispatcher) {
        // Arrange
        analytics.userDetailResult = { throw Server(status = 500, code = null, serverMessage = null, resolution = null) }
        analytics.userSessionsPage = { items(1) { session("s$it") } }

        // Act
        val viewModel = UserDetailViewModel(SITE_ID, USER_ID, analytics, query)

        // Assert
        assertTrue(viewModel.profileState.value is UserProfileState.Error)
        assertEquals(1, (viewModel.uiState.value as PagedUiState.Content<SessionSummary>).items.size)
    }

    @Test
    fun `a missing profile endpoint shows the unavailable state`() = runTest(dispatcher) {
        // Arrange
        analytics.userDetailResult = { throw FeatureUnavailable(code = "API_ROUTE_NOT_FOUND", serverMessage = null, resolution = null) }

        // Act
        val viewModel = UserDetailViewModel(SITE_ID, USER_ID, analytics, query)

        // Assert
        assertEquals(UserProfileState.Unavailable, viewModel.profileState.value)
    }

    @Test
    fun `refreshAll reloads the profile and the first page of sessions`() = runTest(dispatcher) {
        // Arrange
        analytics.userDetailResult = { UserDetail(userId = USER_ID) }
        analytics.userSessionsPage = { emptyList() }
        val viewModel = UserDetailViewModel(SITE_ID, USER_ID, analytics, query)

        // Act
        viewModel.refreshAll()

        // Assert
        assertTrue(viewModel.profileState.value is UserProfileState.Loaded)
        assertEquals(2, analytics.userSessionRequests.size)
        assertEquals(listOf(1, 1), analytics.userSessionRequests.map { it.page })
    }

    @Test
    fun `changing the shared range reloads the sessions`() = runTest(dispatcher) {
        // Arrange
        analytics.userDetailResult = { UserDetail(userId = USER_ID) }
        analytics.userSessionsPage = { emptyList() }
        UserDetailViewModel(SITE_ID, USER_ID, analytics, query)

        // Act
        query.value = AnalyticsQuery(range = DateRange.Last7Days)

        // Assert
        assertEquals(DateRange.Last7Days, analytics.userSessionRequests.last().query.range)
        assertFalse(analytics.userSessionRequests.isEmpty())
    }

    @Test
    fun `a full page of sessions allows loading more`() = runTest(dispatcher) {
        // Arrange
        analytics.userDetailResult = { UserDetail(userId = USER_ID) }
        analytics.userSessionsPage = { page -> items(UserDetailViewModel.PAGE_SIZE) { session("p$page-$it") } }
        val viewModel = UserDetailViewModel(SITE_ID, USER_ID, analytics, query)

        // Act
        viewModel.loadMore()

        // Assert
        val content = viewModel.uiState.value as PagedUiState.Content<SessionSummary>
        assertEquals(UserDetailViewModel.PAGE_SIZE * 2, content.items.size)
        assertEquals(listOf(1, 2), analytics.userSessionRequests.map { it.page })
    }

    private companion object {
        const val SITE_ID = "1"
        const val USER_ID = "user-1"
    }
}
