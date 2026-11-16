package app.rybbitclient.core.query

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

class AnalyticsQueryTest {

    private val zone = ZoneId.of("Europe/Madrid")
    private val clock = Clock.fixed(Instant.parse("2026-11-16T09:00:00Z"), zone)

    @Test
    fun `date range becomes start and end date params with the zone`() {
        val params = AnalyticsQuery(DateRange.Last7Days, zone = zone).toParams(clock)
        assertEquals(
            mapOf("start_date" to "2026-11-10", "end_date" to "2026-11-16", "time_zone" to "Europe/Madrid"),
            params,
        )
    }

    @Test
    fun `last 24 hours becomes past minute params`() {
        val params = AnalyticsQuery(DateRange.Last24Hours, zone = zone).toParams(clock)
        assertEquals("1440", params["past_minutes_start"])
        assertEquals("0", params["past_minutes_end"])
        assertFalse(params.containsKey("start_date"))
    }

    @Test
    fun `all time sends only the zone`() {
        assertEquals(mapOf("time_zone" to "Europe/Madrid"), AnalyticsQuery(DateRange.AllTime, zone = zone).toParams(clock))
    }

    @Test
    fun `filters are serialized exactly like the server fixture`() {
        val query = AnalyticsQuery(
            range = DateRange.AllTime,
            filters = listOf(Filter("browser", FilterType.Equals, listOf("Firefox"))),
            zone = zone,
        )
        assertEquals(
            """[{"parameter":"browser","type":"equals","value":["Firefox"]}]""",
            query.toParams(clock)["filters"],
        )
    }

    @Test
    fun `multiple values are kept as a list`() {
        val encoded = FilterEncoder.encode(listOf(Filter("country", FilterType.NotEquals, listOf("US", "GB"))))
        assertEquals("""[{"parameter":"country","type":"not_equals","value":["US","GB"]}]""", encoded)
    }

    @Test
    fun `previous period carries filters and zone`() {
        val query = AnalyticsQuery(
            range = DateRange.Last7Days,
            filters = listOf(Filter("channel", FilterType.Contains, listOf("Organic"))),
            zone = zone,
        )
        val previous = query.previousPeriod(clock)!!
        // The current week is still in progress (10:00 Madrid), so the comparison window ends at 10:00 too.
        assertEquals("19320", previous["past_minutes_start"])
        assertEquals("10080", previous["past_minutes_end"])
        assertTrue(previous.containsKey("filters"))
    }

    @Test
    fun `all time has no previous period`() {
        assertNull(AnalyticsQuery(DateRange.AllTime, zone = zone).previousPeriod(clock))
    }

    @Test
    fun `default bucket follows the window`() {
        assertEquals(TimeBucket.Day, AnalyticsQuery(DateRange.Last30Days, zone = zone).defaultBucket(clock))
    }

    @Test
    fun `custom range params`() {
        val query = AnalyticsQuery(
            DateRange.Custom(LocalDate.parse("2026-11-01"), LocalDate.parse("2026-11-30")),
            zone = zone,
        )
        assertEquals("2026-11-30", query.toParams(clock)["end_date"])
    }
}
