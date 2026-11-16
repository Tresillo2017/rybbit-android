package app.rybbitclient.core.query

/** Time-series bucket sizes accepted by the `bucket` parameter. [apiValue] is the wire value. */
enum class TimeBucket(val apiValue: String) {
    Minute("minute"),
    FiveMinutes("five_minutes"),
    TenMinutes("ten_minutes"),
    FifteenMinutes("fifteen_minutes"),
    Hour("hour"),
    Day("day"),
    Week("week"),
    Month("month");

    companion object {
        /** Default bucket: hourly up to two days, daily up to 90 days, weekly to a year, monthly beyond. */
        fun defaultFor(range: ResolvedRange): TimeBucket = when (range) {
            is ResolvedRange.PastMinutes -> if (range.startMinutesAgo - range.endMinutesAgo <= MINUTES_PER_DAY) {
                Hour
            } else {
                Day
            }
            is ResolvedRange.Dates -> defaultForDays(daysInclusive(range))
            ResolvedRange.Unbounded -> Month
        }

        private fun defaultForDays(days: Long): TimeBucket = when {
            days <= HOURLY_MAX_DAYS -> Hour
            days <= DAILY_MAX_DAYS -> Day
            days <= WEEKLY_MAX_DAYS -> Week
            else -> Month
        }

        private fun daysInclusive(range: ResolvedRange.Dates): Long =
            java.time.temporal.ChronoUnit.DAYS.between(range.start, range.end) + 1

        private const val MINUTES_PER_DAY = 24L * 60L
        private const val HOURLY_MAX_DAYS = 2L
        private const val DAILY_MAX_DAYS = 90L
        private const val WEEKLY_MAX_DAYS = 365L
    }
}
