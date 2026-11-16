package app.rybbitclient.feature.overview

import app.rybbitclient.core.model.MetricPage
import app.rybbitclient.core.model.MetricRow
import app.rybbitclient.core.model.OverviewPoint
import app.rybbitclient.core.query.DateRange
import app.rybbitclient.core.query.TimeBucket
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.util.Locale

/** Pure Overview logic: deltas, KPI tiles, chart series, date presets, labels and paging. */
class OverviewMathTest {

    private val clock = Clock.fixed(Instant.parse("2026-11-16T12:00:00Z"), ZoneOffset.UTC)

    @Test
    fun `delta is the percent change from the previous value`() {
        assertEquals(20.0, deltaPercent(120.0, 100.0)!!, 1e-9)
        assertEquals(-50.0, deltaPercent(5.0, 10.0)!!, 1e-9)
    }

    @Test
    fun `delta is null when a side is missing or the previous value is zero`() {
        assertNull(deltaPercent(10.0, null))
        assertNull(deltaPercent(null, 10.0))
        assertNull(deltaPercent(10.0, 0.0))
    }

    @Test
    fun `bounce rate is marked inverse and other KPIs are not`() {
        val items = kpiItems(KpiData(current = overviewOf(users = 1, bounceRate = 40.0), previous = null))

        assertTrue(items.first { it.key == KpiMetric.BounceRate.name }.isInverse)
        assertFalse(items.first { it.key == KpiMetric.Visitors.name }.isInverse)
    }

    @Test
    fun `KPI items carry deltas only when a previous period exists`() {
        val current = overviewOf(users = 110, sessions = 30)
        val withPrevious = kpiItems(KpiData(current, previous = overviewOf(users = 100, sessions = 60)))
        val withoutPrevious = kpiItems(KpiData(current, previous = null))

        assertEquals(10.0, withPrevious.first { it.key == KpiMetric.Visitors.name }.deltaPercent!!, 1e-9)
        assertEquals(-50.0, withPrevious.first { it.key == KpiMetric.Sessions.name }.deltaPercent!!, 1e-9)
        assertTrue(withoutPrevious.all { it.deltaPercent == null })
    }

    @Test
    fun `KPI items list every metric in display order`() {
        val items = kpiItems(KpiData(overviewOf(), previous = null))

        assertEquals(KpiMetric.entries.map { it.name }, items.map { it.key })
    }

    @Test
    fun `chart adds a dashed previous series only when previous points exist`() {
        val points = listOf(point("2026-11-15 00:00:00", pageviews = 4), point("2026-11-16 00:00:00", pageviews = 9))
        val withPrevious = TrendData(TimeBucket.Day, points, previous = listOf(point("2026-11-13 00:00:00", pageviews = 2)))
        val withoutPrevious = TrendData(TimeBucket.Day, points, previous = emptyList())

        val series = chartSeries(withPrevious, KpiMetric.Pageviews)
        assertEquals(listOf(4.0, 9.0), series[0].values)
        assertFalse(series[0].isPrevious)
        assertTrue(series[1].isPrevious)
        assertEquals(listOf(2.0), series[1].values)
        assertEquals(1, chartSeries(withoutPrevious, KpiMetric.Pageviews).size)
    }

    @Test
    fun `chart values follow the selected metric and treat missing values as zero`() {
        val trend = TrendData(
            bucket = TimeBucket.Hour,
            current = listOf(point("2026-11-16 09:00:00", users = 3), OverviewPoint(time = "2026-11-16 10:00:00")),
            previous = emptyList(),
        )

        assertEquals(listOf(3.0, 0.0), chartSeries(trend, KpiMetric.Visitors).single().values)
    }

    @Test
    fun `presets map to ranges and back, and custom ranges have no preset key`() {
        assertEquals(DateRange.Last30Days, rangeForPreset("30d"))
        assertEquals("30d", presetKeyFor(DateRange.Last30Days))
        assertNull(presetKeyFor(DateRange.Custom(LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 3))))
        assertNull(rangeForPreset("unknown"))
    }

    @Test
    fun `a reversed custom picker range is swapped instead of failing`() {
        val start = millisOf(LocalDate.of(2026, 11, 9))
        val end = millisOf(LocalDate.of(2026, 11, 3))

        val range = customRangeFromMillis(start, end)

        assertEquals(LocalDate.of(2026, 11, 3), range.start)
        assertEquals(LocalDate.of(2026, 11, 9), range.end)
    }

    @Test
    fun `previous range of last 7 days is the 7 days before it`() {
        val previous = previousRangeOf(DateRange.Last7Days, clock, ZoneOffset.UTC)

        assertEquals(DateRange.Custom(LocalDate.of(2026, 11, 3), LocalDate.of(2026, 11, 9)), previous)
    }

    @Test
    fun `all time and rolling windows have no previous range`() {
        assertNull(previousRangeOf(DateRange.AllTime, clock, ZoneOffset.UTC))
        assertNull(previousRangeOf(DateRange.Last24Hours, clock, ZoneOffset.UTC))
    }

    @Test
    fun `axis labels show time for hourly buckets and dates for daily ones`() {
        val hourly = axisLabels(TimeBucket.Hour, listOf("2026-11-15 09:00:00"), Locale.US)
        val daily = axisLabels(TimeBucket.Day, listOf("2026-11-15 00:00:00"), Locale.US)
        val monthly = axisLabels(TimeBucket.Month, listOf("2026-11-01 00:00:00"), Locale.US)

        assertEquals(listOf("09:00"), hourly)
        assertEquals(listOf("Nov 15"), daily)
        assertEquals(listOf("Nov 2026"), monthly)
    }

    @Test
    fun `custom range label shows both ends`() {
        val label = customRangeLabel(DateRange.Custom(LocalDate.of(2026, 11, 3), LocalDate.of(2026, 11, 9)), Locale.US)

        assertEquals("3 Nov - 9 Nov", label)
    }

    @Test
    fun `blank referrers read as direct and countries carry a flag`() {
        assertEquals("Direct / none", displayValue("referrer", ""))
        assertEquals("Unknown", displayValue("browser", null))
        assertTrue(displayValue("country", "GB").contains("🇬🇧"))
    }

    @Test
    fun `filter chip names the parameter and value`() {
        val filter = equalsFilter("browser", "Firefox")

        assertEquals("Browser: Firefox", filterChipLabel(filter))
    }

    @Test
    fun `bar items keep the raw value as the key so taps can filter`() {
        val items = barItemsFor("operating_system", listOf(MetricRow(value = "iOS", count = 12, percentage = 25.0)))

        assertEquals("iOS", items.single().key)
        assertEquals(12.0, items.single().value, 1e-9)
        assertTrue(items.single().percentText.orEmpty().startsWith("25"))
    }

    @Test
    fun `a page with a total count stops when every row is loaded`() {
        val first = FullListState(parameter = "pathname")

        val partial = appendPage(first, MetricPage(rows(50), totalCount = 60), pageNumber = 1, pageSize = 50)
        val complete = appendPage(partial, MetricPage(rows(10), totalCount = 60), pageNumber = 2, pageSize = 50)

        assertTrue(partial.hasMore)
        assertEquals(50, partial.rows.size)
        assertFalse(complete.hasMore)
        assertEquals(60, complete.rows.size)
        assertEquals(2, complete.page)
    }

    @Test
    fun `without a total count a full page means there may be more`() {
        val first = FullListState(parameter = "pathname")

        val full = appendPage(first, MetricPage(rows(50), totalCount = null), pageNumber = 1, pageSize = 50)
        val short = appendPage(first, MetricPage(rows(7), totalCount = null), pageNumber = 1, pageSize = 50)

        assertTrue(full.hasMore)
        assertFalse(short.hasMore)
    }

    @Test
    fun `an empty page ends paging`() {
        val list = appendPage(FullListState(parameter = "pathname"), MetricPage(emptyList(), totalCount = 99), 1, 50)

        assertFalse(list.hasMore)
    }

    private fun rows(count: Int): List<MetricRow> = (1..count).map { MetricRow(value = "/page-$it", count = it) }
}
