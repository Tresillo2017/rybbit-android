package app.rybbitclient.feature.retention

import app.rybbitclient.core.network.ApiException
import app.rybbitclient.core.ui.components.RetentionCohort

/** Retention period granularity. [apiValue] is what `GET /retention?mode=` expects. */
enum class RetentionMode(val apiValue: String, val label: String, val periodNoun: String) {
    Day("day", "Daily", "Day"),
    Week("week", "Weekly", "Week"),
}

/** Trailing windows the screen offers, in days. The API accepts 7 to 365. */
val RETENTION_RANGE_OPTIONS: List<Int> = listOf(30, 90, 180, 365)

/** Default trailing window, matching the repository default. */
const val DEFAULT_RETENTION_WINDOW_DAYS = 30

/**
 * UI state for [RetentionScreen]. Every state carries the selected [mode] and [range], so the
 * mode and range controls stay on screen during loading and after an error.
 */
sealed interface RetentionUiState {
    val mode: RetentionMode
    val range: Int

    data class Loading(override val mode: RetentionMode, override val range: Int) : RetentionUiState

    /** Cohorts sorted oldest first. An empty [cohorts] list is the empty state. */
    data class Content(
        override val mode: RetentionMode,
        override val range: Int,
        val cohorts: List<RetentionCohort>,
        val isRefreshing: Boolean = false,
    ) : RetentionUiState

    data class Error(
        override val mode: RetentionMode,
        override val range: Int,
        val error: ApiException,
    ) : RetentionUiState
}
