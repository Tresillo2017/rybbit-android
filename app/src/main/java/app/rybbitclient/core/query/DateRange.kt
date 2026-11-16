package app.rybbitclient.core.query

import java.time.LocalDate

/** A user-facing date window. Resolved against a clock and zone by [DateRangeResolver]. */
sealed interface DateRange {
    data object Today : DateRange
    data object Yesterday : DateRange
    data object Last24Hours : DateRange
    data object Last7Days : DateRange
    data object Last30Days : DateRange
    data object Last90Days : DateRange
    data object ThisMonth : DateRange
    data object LastMonth : DateRange
    data object ThisYear : DateRange
    /** No time parameters: the server returns all-time data. Has no comparison period. */
    data object AllTime : DateRange

    /** Inclusive calendar dates. [end] must not be before [start]. */
    data class Custom(val start: LocalDate, val end: LocalDate) : DateRange {
        init {
            require(!end.isBefore(start)) { "Custom range end ($end) must not be before start ($start)" }
        }
    }

    /**
     * A rolling window in minutes before now. Used for comparison periods that must end at the same elapsed
     * time of day as a window still in progress.
     */
    data class ElapsedWindow(val startMinutesAgo: Long, val endMinutesAgo: Long) : DateRange
}

/** A [DateRange] turned into concrete query boundaries for a specific clock and zone. */
sealed interface ResolvedRange {
    /** Inclusive calendar days, sent as `start_date` / `end_date`. */
    data class Dates(val start: LocalDate, val end: LocalDate) : ResolvedRange

    /** Rolling window in minutes ago, sent as `past_minutes_start` (older) / `past_minutes_end` (newer). */
    data class PastMinutes(val startMinutesAgo: Long, val endMinutesAgo: Long) : ResolvedRange

    /** No boundaries: all time. */
    data object Unbounded : ResolvedRange
}
