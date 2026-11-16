package app.rybbitclient.feature.errors

import app.rybbitclient.core.model.ErrorName
import app.rybbitclient.core.network.ApiException
import app.rybbitclient.core.query.AnalyticsQuery

/** UI state for [ErrorsScreen]. [Content.isRefreshing] keeps the list on screen during a reload. */
sealed interface ErrorsUiState {

    data object Loading : ErrorsUiState

    data class Content(
        val query: AnalyticsQuery,
        val errors: List<ErrorName>,
        val isRefreshing: Boolean = false,
    ) : ErrorsUiState

    data class Error(val error: ApiException) : ErrorsUiState

    /** The instance does not offer error reports (a 404 `API_ROUTE_NOT_FOUND`). Calm, never red. */
    data object Unavailable : ErrorsUiState
}
