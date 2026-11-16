package app.rybbitclient.core.query

import java.time.Clock
import java.time.ZoneId

/** Everything that scopes an analytics request except the site: date range, filters and time zone. */
data class AnalyticsQuery(
    val range: DateRange,
    val filters: List<Filter> = emptyList(),
    val zone: ZoneId = ZoneId.systemDefault(),
) {

    /** Query parameters for the current window, resolved against [clock]. Empty filters are omitted. */
    fun toParams(clock: Clock = Clock.systemDefaultZone()): Map<String, String> =
        paramsFor(range, DateRangeResolver(clock, zone))

    /** The same filters and zone over the comparison period, or null when the range has none. */
    fun previousPeriod(clock: Clock = Clock.systemDefaultZone()): Map<String, String>? {
        val resolver = DateRangeResolver(clock, zone)
        val previous = resolver.previousPeriod(range) ?: return null
        return paramsForResolved(previous)
    }

    /** The default bucket for this query's window, resolved against [clock]. */
    fun defaultBucket(clock: Clock = Clock.systemDefaultZone()): TimeBucket =
        TimeBucket.defaultFor(DateRangeResolver(clock, zone).resolve(range))

    private fun paramsFor(range: DateRange, resolver: DateRangeResolver): Map<String, String> =
        paramsForResolved(resolver.resolve(range))

    private fun paramsForResolved(resolved: ResolvedRange): Map<String, String> {
        val window = when (resolved) {
            is ResolvedRange.Dates -> mapOf(
                PARAM_START_DATE to resolved.start.toString(),
                PARAM_END_DATE to resolved.end.toString(),
            )
            is ResolvedRange.PastMinutes -> mapOf(
                PARAM_PAST_MINUTES_START to resolved.startMinutesAgo.toString(),
                PARAM_PAST_MINUTES_END to resolved.endMinutesAgo.toString(),
            )
            ResolvedRange.Unbounded -> emptyMap()
        }
        val filterParams = if (filters.isEmpty()) emptyMap() else mapOf(PARAM_FILTERS to FilterEncoder.encode(filters))
        return window + mapOf(PARAM_TIME_ZONE to zone.id) + filterParams
    }

    companion object {
        const val PARAM_START_DATE = "start_date"
        const val PARAM_END_DATE = "end_date"
        const val PARAM_TIME_ZONE = "time_zone"
        const val PARAM_PAST_MINUTES_START = "past_minutes_start"
        const val PARAM_PAST_MINUTES_END = "past_minutes_end"
        const val PARAM_FILTERS = "filters"
    }
}
