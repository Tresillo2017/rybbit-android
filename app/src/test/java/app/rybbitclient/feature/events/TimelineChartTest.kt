package app.rybbitclient.feature.events

import app.rybbitclient.core.model.ErrorTimePoint
import app.rybbitclient.core.model.EventTimePoint
import app.rybbitclient.core.query.TimeBucket
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime

class TimelineChartTest {

    @Test
    fun `aligns every series to the shared buckets and fills gaps with zero`() {
        // Arrange
        val cells = listOf(
            TimelineCell(series = "download", time = LocalDateTime.of(2026, 11, 16, 1, 0), count = 3.0),
            TimelineCell(series = "signup", time = LocalDateTime.of(2026, 11, 16, 0, 0), count = 28.0),
        )

        // Act
        val chart = buildTimelineChart(cells, TimeBucket.Hour, subject = "Custom events")

        // Assert
        assertEquals(listOf("00:00", "01:00"), chart.labels)
        assertEquals(listOf("signup", "download"), chart.series.map { it.label })
        assertEquals(listOf(28.0, 0.0), chart.series[0].values)
        assertEquals(listOf(0.0, 3.0), chart.series[1].values)
    }

    @Test
    fun `summary names the series and totals every count`() {
        // Arrange
        val cells = listOf(TimelineCell(series = ERRORS_SERIES_LABEL, time = LocalDateTime.of(2026, 11, 16, 0, 0), count = 12.0))

        // Act
        val chart = buildTimelineChart(cells, TimeBucket.Day, subject = "Errors")

        // Assert
        assertTrue(chart.summary.startsWith("Errors over time: Errors."))
        assertTrue(chart.summary.endsWith("Total 12."))
    }

    @Test
    fun `empty input gives no series and a no-data summary`() {
        // Act
        val chart = buildTimelineChart(emptyList(), TimeBucket.Day, subject = "Custom events")

        // Assert
        assertTrue(chart.series.isEmpty())
        assertTrue(chart.labels.isEmpty())
        assertEquals("No data for this period", chart.summary)
    }

    @Test
    fun `event timeline drops rows without a name or a readable time`() {
        // Arrange
        val points = listOf(
            EventTimePoint(time = "2026-11-16 00:00:00", eventName = "signup", eventCount = 4),
            EventTimePoint(time = "2026-11-16 00:00:00", eventName = null, eventCount = 9),
            EventTimePoint(time = "not a time", eventName = "signup", eventCount = 1),
        )

        // Act
        val cells = eventTimeline(points)

        // Assert
        assertEquals(listOf("signup"), cells.map { it.series })
        assertEquals(listOf(4.0), cells.map { it.count })
    }

    @Test
    fun `error timeline puts every count on the single errors line`() {
        // Act
        val cells = errorTimeline(listOf(ErrorTimePoint(time = "2026-11-16 00:00:00", errorCount = 12)))

        // Assert
        assertEquals(listOf(ERRORS_SERIES_LABEL), cells.map { it.series })
        assertEquals(listOf(12.0), cells.map { it.count })
    }
}
