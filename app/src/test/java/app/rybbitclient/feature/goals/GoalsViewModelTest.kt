package app.rybbitclient.feature.goals

import app.rybbitclient.core.model.Goal
import app.rybbitclient.core.model.GoalsResponse
import app.rybbitclient.core.model.PageMeta
import app.rybbitclient.core.model.SessionSummary
import app.rybbitclient.core.network.Server
import app.rybbitclient.core.query.AnalyticsQuery
import app.rybbitclient.core.query.DateRange
import app.rybbitclient.core.query.SiteQueryState
import app.rybbitclient.feature.common.FakeSiteAnalytics
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class GoalsViewModelTest {

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
    fun `loads the first page of goals into content`() {
        // Arrange
        val analytics = FakeSiteAnalytics(goalsHandler = { _, _ -> response(listOf(goal(1), goal(2)), total = 2, page = 1) })

        // Act
        val viewModel = GoalsViewModel(SITE_ID, analytics, SiteQueryState())

        // Assert
        val content = viewModel.uiState.value as GoalsUiState.Content
        assertEquals(listOf(1, 2), content.goals.map { it.goalId })
        assertEquals(1, content.page)
        assertFalse(content.hasMore)
    }

    @Test
    fun `no goals is an empty content state rather than an error`() {
        // Arrange
        val analytics = FakeSiteAnalytics(goalsHandler = { _, _ -> response(emptyList(), total = 0, page = 1) })

        // Act
        val viewModel = GoalsViewModel(SITE_ID, analytics, SiteQueryState())

        // Assert
        val content = viewModel.uiState.value as GoalsUiState.Content
        assertTrue(content.goals.isEmpty())
        assertFalse(content.hasMore)
    }

    @Test
    fun `next page is appended and paging stops on the last page`() {
        // Arrange
        val pages = mapOf(
            1 to response((1..GOALS_PAGE_SIZE).map { goal(it) }, total = 12, page = 1),
            2 to response((11..12).map { goal(it) }, total = 12, page = 2),
        )
        val analytics = FakeSiteAnalytics(goalsHandler = { _, page -> pages.getValue(page) })
        val viewModel = GoalsViewModel(SITE_ID, analytics, SiteQueryState())

        // Act
        viewModel.loadNextPage()

        // Assert
        val content = viewModel.uiState.value as GoalsUiState.Content
        assertEquals((1..12).toList(), content.goals.map { it.goalId })
        assertEquals(2, content.page)
        assertFalse(content.hasMore)
        assertFalse(content.isLoadingMore)
    }

    @Test
    fun `a failed next page keeps loaded goals and records the error`() {
        // Arrange
        val analytics = FakeSiteAnalytics(goalsHandler = { _, page ->
            if (page == 1) response((1..GOALS_PAGE_SIZE).map { goal(it) }, total = 20, page = 1) else throw serverError()
        })
        val viewModel = GoalsViewModel(SITE_ID, analytics, SiteQueryState())

        // Act
        viewModel.loadNextPage()

        // Assert
        val content = viewModel.uiState.value as GoalsUiState.Content
        assertEquals(GOALS_PAGE_SIZE, content.goals.size)
        assertTrue(content.loadMoreError is Server)
        assertTrue(content.hasMore)
    }

    @Test
    fun `a failed first load is an error state`() {
        // Arrange
        val analytics = FakeSiteAnalytics(goalsHandler = { _, _ -> throw serverError() })

        // Act
        val viewModel = GoalsViewModel(SITE_ID, analytics, SiteQueryState())

        // Assert
        assertTrue(viewModel.uiState.value is GoalsUiState.Error)
    }

    @Test
    fun `changing the shared date range reloads goals from page one with the new range`() {
        // Arrange
        val seenRanges = mutableListOf<DateRange>()
        val siteQuery = SiteQueryState()
        val analytics = FakeSiteAnalytics(goalsHandler = { query, _ ->
            seenRanges += query.range
            response(listOf(goal(1)), total = 1, page = 1)
        })
        val viewModel = GoalsViewModel(SITE_ID, analytics, siteQuery)

        // Act
        siteQuery.setRange(DateRange.Last30Days)

        // Assert
        assertEquals(listOf(DateRange.Last7Days, DateRange.Last30Days), seenRanges)
        assertEquals(1, (viewModel.uiState.value as GoalsUiState.Content).goals.size)
    }

    @Test
    fun `refresh keeps the list on screen and clears the refreshing flag when done`() {
        // Arrange
        val analytics = FakeSiteAnalytics(goalsHandler = { _, _ -> response(listOf(goal(3)), total = 1, page = 1) })
        val viewModel = GoalsViewModel(SITE_ID, analytics, SiteQueryState())

        // Act
        viewModel.refresh()

        // Assert
        val content = viewModel.uiState.value as GoalsUiState.Content
        assertEquals(listOf(3), content.goals.map { it.goalId })
        assertFalse(content.isRefreshing)
    }

    @Test
    fun `opening a goal loads its converting sessions into the sheet and closing resets it`() {
        // Arrange
        val analytics = FakeSiteAnalytics(
            goalsHandler = { _, _ -> response(listOf(goal(7)), total = 1, page = 1) },
            goalSessionsHandler = { _, goalId -> listOf(SessionSummary(sessionId = "session-$goalId")) },
        )
        val viewModel = GoalsViewModel(SITE_ID, analytics, SiteQueryState())

        // Act
        viewModel.openGoalSessions(goal(7))
        val loaded = viewModel.sheetState.value
        viewModel.closeGoalSessions()

        // Assert
        val sheet = loaded as GoalSessionsState.Loaded
        assertEquals("session-7", sheet.sessions.single().sessionId)
        assertEquals(GoalSessionsState.Closed, viewModel.sheetState.value)
    }

    @Test
    fun `a failed sessions request puts the sheet in its failed state`() {
        // Arrange
        val analytics = FakeSiteAnalytics(
            goalsHandler = { _, _ -> response(emptyList(), total = 0, page = 1) },
            goalSessionsHandler = { _, _ -> throw serverError() },
        )
        val viewModel = GoalsViewModel(SITE_ID, analytics, SiteQueryState())

        // Act
        viewModel.openGoalSessions(goal(4))

        // Assert
        assertTrue(viewModel.sheetState.value is GoalSessionsState.Failed)
        assertNull((viewModel.uiState.value as GoalsUiState.Content).loadMoreError)
    }

    private fun response(goals: List<Goal>, total: Int, page: Int) =
        GoalsResponse(data = goals, meta = PageMeta(total = total, page = page, pageSize = GOALS_PAGE_SIZE))

    private fun goal(id: Int) = Goal(goalId = id, name = "Goal $id", goalType = "path")

    private fun serverError() = Server(status = 500, code = null, serverMessage = null, resolution = null)

    private companion object {
        const val SITE_ID = "1"
    }
}
