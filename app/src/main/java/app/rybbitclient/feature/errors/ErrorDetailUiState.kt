package app.rybbitclient.feature.errors

import app.rybbitclient.core.model.ErrorEvent
import app.rybbitclient.core.model.ErrorTimePoint
import app.rybbitclient.core.network.ApiException
import app.rybbitclient.core.query.AnalyticsQuery
import app.rybbitclient.core.query.TimeBucket

/** Totals for one error message, from the error-name list. Null when the message is not in that list. */
data class ErrorSummary(
    val errorName: String?,
    val count: Int,
    val sessionCount: Int,
    val percentage: Double?,
)

/** UI state for [ErrorDetailScreen]. Every section comes from one load. */
sealed interface ErrorDetailUiState {

    data object Loading : ErrorDetailUiState

    data class Content(
        val query: AnalyticsQuery,
        val summary: ErrorSummary?,
        val bucket: TimeBucket,
        val points: List<ErrorTimePoint>,
        val occurrences: List<ErrorEvent>,
        val isRefreshing: Boolean = false,
    ) : ErrorDetailUiState

    data class Error(val error: ApiException) : ErrorDetailUiState

    data object Unavailable : ErrorDetailUiState
}
