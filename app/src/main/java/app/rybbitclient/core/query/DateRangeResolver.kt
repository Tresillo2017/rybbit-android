package app.rybbitclient.core.query

import java.time.Clock
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit

/**
 * Resolves [DateRange] presets into concrete query windows. Takes an injected [Clock] and [ZoneId]
 * so "today" is deterministic in tests.
 */
class DateRangeResolver(
    private val clock: Clock = Clock.systemDefaultZone(),
    private val zone: ZoneId = ZoneId.systemDefault(),
) {

    fun resolve(range: DateRange): ResolvedRange {
        val today = LocalDate.now(clock.withZone(zone))
        return when (range) {
            DateRange.Today -> dates(today, today)
            DateRange.Yesterday -> today.minusDays(1).let { dates(it, it) }
            DateRange.Last24Hours -> ResolvedRange.PastMinutes(startMinutesAgo = MINUTES_PER_DAY, endMinutesAgo = 0)
            DateRange.Last7Days -> dates(today.minusDays(6), today)
            DateRange.Last30Days -> dates(today.minusDays(29), today)
            DateRange.Last90Days -> dates(today.minusDays(89), today)
            DateRange.ThisMonth -> dates(today.withDayOfMonth(1), today)
            DateRange.LastMonth -> {
                val firstOfThis = today.withDayOfMonth(1)
                dates(firstOfThis.minusMonths(1), firstOfThis.minusDays(1))
            }
            DateRange.ThisYear -> dates(today.withDayOfYear(1), today)
            DateRange.AllTime -> ResolvedRange.Unbounded
            is DateRange.Custom -> dates(range.start, range.end)
        }
    }

    /**
     * The comparison window of equal length immediately before [range]. Null when there is nothing
     * to compare against (all time).
     */
    fun previousPeriod(range: DateRange): ResolvedRange? = when (val resolved = resolve(range)) {
        is ResolvedRange.Dates -> {
            val length = ChronoUnit.DAYS.between(resolved.start, resolved.end) + 1
            val previousEnd = resolved.start.minusDays(1)
            dates(previousEnd.minusDays(length - 1), previousEnd)
        }
        is ResolvedRange.PastMinutes -> {
            val span = resolved.startMinutesAgo - resolved.endMinutesAgo
            ResolvedRange.PastMinutes(
                startMinutesAgo = resolved.startMinutesAgo + span,
                endMinutesAgo = resolved.startMinutesAgo,
            )
        }
        ResolvedRange.Unbounded -> null
    }

    private fun dates(start: LocalDate, end: LocalDate) = ResolvedRange.Dates(start, end)

    private companion object {
        const val MINUTES_PER_DAY = 24L * 60L
    }
}
