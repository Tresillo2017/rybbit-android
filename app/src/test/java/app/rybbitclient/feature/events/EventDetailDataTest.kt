package app.rybbitclient.feature.events

import app.rybbitclient.core.model.Event
import app.rybbitclient.core.model.EventPropertyCount
import app.rybbitclient.core.query.AnalyticsQuery
import app.rybbitclient.core.query.DateRange
import app.rybbitclient.core.query.Filter
import app.rybbitclient.core.query.FilterType
import app.rybbitclient.core.ui.format.formatPercent
import org.junit.Assert.assertEquals
import org.junit.Test

class EventDetailDataTest {

    @Test
    fun `groups property values by key with the most frequent first and keys sorted`() {
        // Arrange
        val rows = listOf(
            EventPropertyCount("plan", "free", 12),
            EventPropertyCount("region", "EU", 5),
            EventPropertyCount("plan", "pro", 16),
        )

        // Act
        val groups = groupEventProperties(rows)

        // Assert
        assertEquals(listOf("plan", "region"), groups.map { it.key })
        assertEquals(listOf("pro", "free"), groups.first().values.map { it.label })
    }

    @Test
    fun `shows blank values as empty and treats a missing count as zero`() {
        // Arrange
        val rows = listOf(
            EventPropertyCount("plan", "", 4),
            EventPropertyCount("plan", null, null),
            EventPropertyCount("plan", "pro", 2),
        )

        // Act
        val values = groupEventProperties(rows).single().values

        // Assert
        assertEquals(listOf("(empty)" to 4, "pro" to 2, "(empty)" to 0), values.map { it.label to it.count })
    }

    @Test
    fun `drops rows with a blank property key`() {
        // Arrange
        val rows = listOf(EventPropertyCount(" ", "value", 9))

        // Act
        val groups = groupEventProperties(rows)

        // Assert
        assertEquals(emptyList<String>(), groups.map { it.key })
    }

    @Test
    fun `property bar rows carry each value's share of its group`() {
        // Arrange
        val group = groupEventProperties(
            listOf(EventPropertyCount("plan", "pro", 3), EventPropertyCount("plan", "free", 1)),
        ).single()

        // Act
        val items = propertyBarItems(group)

        // Assert
        assertEquals(formatPercent(75.0), items.first().percentText)
        assertEquals(formatPercent(25.0), items.last().percentText)
        assertEquals(listOf(3.0, 1.0), items.map { it.value })
    }

    @Test
    fun `occurrences keep only rows whose event name matches exactly`() {
        // Arrange
        val events = listOf(
            Event(eventName = "signup", type = "custom_event"),
            Event(eventName = "signup_click", type = "custom_event"),
            Event(eventName = "", type = "pageview"),
        )

        // Act
        val matches = occurrencesOf(events, "signup")

        // Assert
        assertEquals(listOf(events.first()), matches)
    }

    @Test
    fun `event name filter replaces an earlier event filter and keeps the others`() {
        // Arrange
        val query = AnalyticsQuery(
            range = DateRange.Last7Days,
            filters = listOf(
                Filter("browser", FilterType.Equals, listOf("Firefox")),
                Filter("event_name", FilterType.Equals, listOf("old")),
            ),
        )

        // Act
        val narrowed = withEventNameFilter(query, "signup")

        // Assert
        assertEquals(
            listOf(
                Filter("browser", FilterType.Equals, listOf("Firefox")),
                Filter("event_name", FilterType.Equals, listOf("signup")),
            ),
            narrowed.filters,
        )
        assertEquals(2, query.filters.size)
    }
}
