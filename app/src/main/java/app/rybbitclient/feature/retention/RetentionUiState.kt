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

/** The server cuts daily retention to its most recent 92 days and flags the result as truncated. */
const val MAX_DAILY_RETENTION_DAYS = 92

private const val DAILY_FALLBACK_DAYS = 90

/** Windows offered for [mode]. Daily mode stops at [MAX_DAILY_RETENTION_DAYS] so no chip reads as a longer span than it is. */
fun rangeOptionsFor(mode: RetentionMode): List<Int> = RETENTION_RANGE_OPTIONS.filter { days ->
    mode == RetentionMode.Week || days <= MAX_DAILY_RETENTION_DAYS
}

/** A range that [mode] can show: switching to daily mode caps a longer window at [DAILY_FALLBACK_DAYS]. */
fun clampRangeFor(mode: RetentionMode, days: Int): Int =
    if (mode == RetentionMode.Day && days > MAX_DAILY_RETENTION_DAYS) DAILY_FALLBACK_DAYS else days

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
