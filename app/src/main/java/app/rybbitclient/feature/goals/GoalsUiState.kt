package app.rybbitclient.feature.goals

import app.rybbitclient.core.model.Goal
import app.rybbitclient.core.model.SessionSummary
import app.rybbitclient.core.network.ApiException

/** UI state for [GoalsScreen]. Immutable; every change is a `copy` or a new value. */
sealed interface GoalsUiState {

    /** First load, before any page has arrived. */
    data object Loading : GoalsUiState

    /**
     * Loaded pages so far. An empty [goals] list is the empty state, not an error.
     * [isRefreshing] keeps the list on screen during pull-to-refresh; [isLoadingMore] shows a footer row.
     * [loadMoreError] is set when the next page failed, so the footer can offer a retry.
     */
    data class Content(
        val goals: List<Goal>,
        val page: Int,
        val hasMore: Boolean,
        val isRefreshing: Boolean = false,
        val isLoadingMore: Boolean = false,
        val loadMoreError: ApiException? = null,
    ) : GoalsUiState

    /** The first page failed. The screen maps [error] to friendly copy. */
    data class Error(val error: ApiException) : GoalsUiState
}

/** The bottom sheet that lists the sessions which converted one goal. */
sealed interface GoalSessionsState {

    data object Closed : GoalSessionsState

    data class Loading(val goal: Goal) : GoalSessionsState

    /** An empty [sessions] list is shown as an empty state inside the sheet. */
    data class Loaded(val goal: Goal, val sessions: List<SessionSummary>) : GoalSessionsState

    data class Failed(val goal: Goal, val error: ApiException) : GoalSessionsState
}
