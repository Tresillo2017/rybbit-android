package app.rybbitclient.core.query

import java.time.Clock
import java.time.LocalDate
import java.time.ZonedDateTime
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
            is DateRange.ElapsedWindow -> ResolvedRange.PastMinutes(range.startMinutesAgo, range.endMinutesAgo)
        }
    }

    /**
     * The comparison window of equal length immediately before [range]. Null when there is nothing
     * to compare against (all time).
     */
    fun previousPeriod(range: DateRange): ResolvedRange? = when (val resolved = resolve(range)) {
        is ResolvedRange.Dates -> previousDates(resolved)
        is ResolvedRange.PastMinutes -> {
            val span = resolved.startMinutesAgo - resolved.endMinutesAgo
            ResolvedRange.PastMinutes(
                startMinutesAgo = resolved.startMinutesAgo + span,
                endMinutesAgo = resolved.startMinutesAgo,
            )
        }
        ResolvedRange.Unbounded -> null
    }

    /**
     * A window that ends today is still in progress, so its previous period is clipped to the same time of
     * day: the same number of days, ending at the same elapsed minute. Completed windows compare whole days.
     */
    private fun previousDates(current: ResolvedRange.Dates): ResolvedRange {
        val now = ZonedDateTime.now(clock.withZone(zone))
        if (current.end != now.toLocalDate()) {
            val length = ChronoUnit.DAYS.between(current.start, current.end) + 1
            val previousEnd = current.start.minusDays(1)
            return dates(previousEnd.minusDays(length - 1), previousEnd)
        }
        val days = ChronoUnit.DAYS.between(current.start, current.end) + 1
        val elapsedToday = ChronoUnit.MINUTES.between(now.toLocalDate().atStartOfDay(zone), now)
        // Minutes ago: the previous window starts `days` days before the current start and ends at
        // the same time of day, one day before today (the last `days` days, clipped at `elapsedToday`).
        val endMinutesAgo = days * MINUTES_PER_DAY
        val startMinutesAgo = endMinutesAgo + (days - 1) * MINUTES_PER_DAY + elapsedToday
        return ResolvedRange.PastMinutes(startMinutesAgo = startMinutesAgo, endMinutesAgo = endMinutesAgo)
    }

    private fun dates(start: LocalDate, end: LocalDate) = ResolvedRange.Dates(start, end)

    private companion object {
        const val MINUTES_PER_DAY = 24L * 60L
    }
}
