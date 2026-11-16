package app.rybbitclient.core.query

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

class DateRangeTest {

    // 2026-11-16 10:00 in Madrid (UTC+1 in November) is 09:00Z.
    private val zone = ZoneId.of("Europe/Madrid")
    private val clock = Clock.fixed(Instant.parse("2026-11-16T09:00:00Z"), zone)
    private val resolver = DateRangeResolver(clock, zone)

    private fun dates(start: String, end: String) =
        ResolvedRange.Dates(LocalDate.parse(start), LocalDate.parse(end))

    @Test
    fun `today is a single day`() {
        assertEquals(dates("2026-11-16", "2026-11-16"), resolver.resolve(DateRange.Today))
    }

    @Test
    fun `yesterday is the day before`() {
        assertEquals(dates("2026-11-15", "2026-11-15"), resolver.resolve(DateRange.Yesterday))
    }

    @Test
    fun `completed windows compare with whole previous days`() {
        assertEquals(dates("2026-11-14", "2026-11-14"), resolver.previousPeriod(DateRange.Yesterday))
    }

    @Test
    fun `last 24 hours uses past minutes`() {
        assertEquals(ResolvedRange.PastMinutes(1440, 0), resolver.resolve(DateRange.Last24Hours))
    }

    @Test
    fun `last 7 days includes today`() {
        assertEquals(dates("2026-11-10", "2026-11-16"), resolver.resolve(DateRange.Last7Days))
    }

    @Test
    fun `last 30 and 90 days are inclusive of today`() {
        assertEquals(dates("2026-10-18", "2026-11-16"), resolver.resolve(DateRange.Last30Days))
        assertEquals(dates("2026-08-19", "2026-11-16"), resolver.resolve(DateRange.Last90Days))
    }

    @Test
    fun `this month starts on the first`() {
        assertEquals(dates("2026-11-01", "2026-11-16"), resolver.resolve(DateRange.ThisMonth))
    }

    @Test
    fun `last month spans the whole previous month across a year boundary`() {
        val january = Clock.fixed(Instant.parse("2027-01-05T12:00:00Z"), zone)
        assertEquals(
            ResolvedRange.Dates(LocalDate.parse("2026-12-01"), LocalDate.parse("2026-12-31")),
            DateRangeResolver(january, zone).resolve(DateRange.LastMonth),
        )
    }

    @Test
    fun `this year starts on January first`() {
        assertEquals(dates("2026-01-01", "2026-11-16"), resolver.resolve(DateRange.ThisYear))
    }

    @Test
    fun `all time is unbounded`() {
        assertEquals(ResolvedRange.Unbounded, resolver.resolve(DateRange.AllTime))
    }

    @Test
    fun `custom range resolves to its own dates`() {
        val custom = DateRange.Custom(LocalDate.parse("2026-11-01"), LocalDate.parse("2026-11-30"))
        assertEquals(dates("2026-11-01", "2026-11-30"), resolver.resolve(custom))
    }

    @Test(expected = IllegalArgumentException::class)
    fun `custom range rejects reversed dates`() {
        DateRange.Custom(LocalDate.parse("2026-11-30"), LocalDate.parse("2026-11-01"))
    }

    @Test
    fun `today uses the zone not the UTC date near midnight`() {
        // 23:30 UTC on Nov 15 is already Nov 16 in Madrid.
        val lateUtc = Clock.fixed(Instant.parse("2026-11-15T23:30:00Z"), zone)
        assertEquals(dates("2026-11-16", "2026-11-16"), DateRangeResolver(lateUtc, zone).resolve(DateRange.Today))
    }

    @Test
    fun `previous period of a date range is the same length right before it`() {
        // The current window ends today, so the previous one is clipped to the same elapsed time of day.
        assertEquals(ResolvedRange.PastMinutes(19320, 10080), resolver.previousPeriod(DateRange.Last7Days))
        // Today at 10:00 is compared with yesterday from 00:00 to 10:00.
        assertEquals(ResolvedRange.PastMinutes(2040, 1440), resolver.previousPeriod(DateRange.Today))
    }

    @Test
    fun `previous period of past minutes is the equal window before`() {
        assertEquals(ResolvedRange.PastMinutes(2880, 1440), resolver.previousPeriod(DateRange.Last24Hours))
    }

    @Test
    fun `all time has no comparison period`() {
        assertNull(resolver.previousPeriod(DateRange.AllTime))
    }

    @Test
    fun `default bucket is hour up to two days and day up to 90 days`() {
        assertEquals(TimeBucket.Hour, TimeBucket.defaultFor(resolver.resolve(DateRange.Today)))
        assertEquals(TimeBucket.Hour, TimeBucket.defaultFor(resolver.resolve(DateRange.Last24Hours)))
        assertEquals(TimeBucket.Hour, TimeBucket.defaultFor(dates("2026-11-15", "2026-11-16")))
        assertEquals(TimeBucket.Day, TimeBucket.defaultFor(resolver.resolve(DateRange.Last7Days)))
        assertEquals(TimeBucket.Day, TimeBucket.defaultFor(resolver.resolve(DateRange.Last90Days)))
    }

    @Test
    fun `default bucket is week then month beyond 90 days`() {
        assertEquals(TimeBucket.Week, TimeBucket.defaultFor(dates("2026-01-01", "2026-06-30")))
        assertEquals(TimeBucket.Month, TimeBucket.defaultFor(dates("2025-01-01", "2026-11-16")))
        assertEquals(TimeBucket.Month, TimeBucket.defaultFor(ResolvedRange.Unbounded))
    }

    @Test
    fun `bucket api values match the server contract`() {
        assertTrue(TimeBucket.entries.map { it.apiValue }.containsAll(listOf("minute", "five_minutes", "hour", "day", "week", "month")))
    }
}
