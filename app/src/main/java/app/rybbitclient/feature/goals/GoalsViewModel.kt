package app.rybbitclient.feature.goals

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.rybbitclient.core.data.AnalyticsRepository
import app.rybbitclient.core.model.Goal
import app.rybbitclient.core.network.ApiException
import app.rybbitclient.core.query.AnalyticsQuery
import app.rybbitclient.core.query.SiteQueryState
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Goals per page. Matches the repository default so the server's `totalPages` lines up with it. */
const val GOALS_PAGE_SIZE = AnalyticsRepository.DEFAULT_GOALS_PAGE_SIZE

/**
 * Goals for one site under the shared date range and filters. Pages are appended as the list nears
 * its end. A query change restarts from page 1 and cancels any page still in flight.
 */
class GoalsViewModel(
    private val siteId: String,
    private val analytics: AnalyticsRepository,
    private val siteQuery: SiteQueryState,
) : ViewModel() {

    private val mutableState = MutableStateFlow<GoalsUiState>(GoalsUiState.Loading)
    val uiState: StateFlow<GoalsUiState> = mutableState.asStateFlow()

    private val mutableSheet = MutableStateFlow<GoalSessionsState>(GoalSessionsState.Closed)
    val sheetState: StateFlow<GoalSessionsState> = mutableSheet.asStateFlow()

    private var query: AnalyticsQuery = siteQuery.query.value
    private var loadJob: Job? = null
    private var sheetJob: Job? = null

    init {
        viewModelScope.launch {
            siteQuery.query.collect { latest ->
                query = latest
                reload(keepContent = false)
            }
        }
    }

    /** Pull-to-refresh: restarts from page 1 and keeps the list on screen while it runs. */
    fun refresh() = reload(keepContent = true)

    /** Fetches the next page, if the server reports one and none is already loading. */
    fun loadNextPage() {
        val current = mutableState.value as? GoalsUiState.Content ?: return
        if (!current.hasMore || current.isLoadingMore) return
        val loading = current.copy(isLoadingMore = true, loadMoreError = null)
        mutableState.value = loading
        loadJob = viewModelScope.launch { appendPage(loading) }
    }

    fun openGoalSessions(goal: Goal) {
        sheetJob?.cancel()
        mutableSheet.value = GoalSessionsState.Loading(goal)
        sheetJob = viewModelScope.launch {
            mutableSheet.value = try {
                GoalSessionsState.Loaded(goal, analytics.goalSessions(siteId, query, goal.goalId))
            } catch (error: ApiException) {
                GoalSessionsState.Failed(goal, error)
            }
        }
    }

    fun closeGoalSessions() {
        sheetJob?.cancel()
        mutableSheet.value = GoalSessionsState.Closed
    }

    private fun reload(keepContent: Boolean) {
        loadJob?.cancel()
        mutableState.update { current ->
            if (keepContent && current is GoalsUiState.Content) current.copy(isRefreshing = true) else GoalsUiState.Loading
        }
        loadJob = viewModelScope.launch { loadFirstPage() }
    }

    private suspend fun loadFirstPage() {
        mutableState.value = try {
            val response = analytics.goals(siteId, query, page = FIRST_PAGE, pageSize = GOALS_PAGE_SIZE)
            EMPTY_CONTENT.withPage(response, page = FIRST_PAGE, pageSize = GOALS_PAGE_SIZE)
        } catch (error: ApiException) {
            GoalsUiState.Error(error)
        }
    }

    private suspend fun appendPage(current: GoalsUiState.Content) {
        val nextPage = current.page + 1
        mutableState.value = try {
            val response = analytics.goals(siteId, query, page = nextPage, pageSize = GOALS_PAGE_SIZE)
            current.withPage(response, page = nextPage, pageSize = GOALS_PAGE_SIZE)
        } catch (error: ApiException) {
            current.copy(isLoadingMore = false, loadMoreError = error)
        }
    }

    private companion object {
        const val FIRST_PAGE = 1
        val EMPTY_CONTENT = GoalsUiState.Content(goals = emptyList<Goal>(), page = 0, hasMore = false)
    }
}
