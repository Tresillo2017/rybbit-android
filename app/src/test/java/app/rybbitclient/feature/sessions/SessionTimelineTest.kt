package app.rybbitclient.feature.sessions

import app.rybbitclient.core.model.SessionDetailEvent
import app.rybbitclient.core.network.RybbitJson
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.ZoneId

class SessionTimelineTest {

    @Test
    fun `converts UTC event timestamps to local wall time`() {
        // Arrange
        val events = listOf(event("2026-11-16 08:41:49.074", type = "pageview"))

        // Act
        val inMadrid = buildTimeline(events, ZoneId.of("Europe/Madrid"))
        val inUtc = buildTimeline(events, ZoneId.of("UTC"))

        // Assert: November is CET (UTC+1) in Madrid
        assertEquals("09:41:49", inMadrid.single().timeLabel)
        assertEquals("08:41:49", inUtc.single().timeLabel)
    }

    @Test
    fun `gap labels measure from the previous timestamped event`() {
        // Arrange
        val events = listOf(
            event("2026-11-16 08:00:00", type = "pageview"),
            event("2026-11-16 08:01:05", type = "pageview"),
            event("not a timestamp", type = "pageview"),
            event("2026-11-16 08:03:05", type = "pageview"),
        )

        // Act
        val timeline = buildTimeline(events, ZoneId.of("UTC"))

        // Assert
        assertNull(timeline[0].sinceLabel)
        assertEquals("+1m 05s", timeline[1].sinceLabel)
        assertEquals("—", timeline[2].timeLabel)
        assertNull(timeline[2].sinceLabel)
        assertEquals("+2m 00s", timeline[3].sinceLabel)
    }

    @Test
    fun `titles follow the event kind`() {
        // Arrange
        val events = listOf(
            event("2026-11-16 08:00:00", type = "pageview", pageTitle = "Pricing", pathname = "/pricing"),
            event("2026-11-16 08:00:01", type = "custom_event", eventName = "signup_started", pathname = "/signup"),
            event("2026-11-16 08:00:02", type = "outbound_click", pathname = "/docs"),
        )

        // Act
        val timeline = buildTimeline(events, ZoneId.of("UTC"))

        // Assert
        assertEquals(TimelineKind.Pageview, timeline[0].kind)
        assertEquals("Pricing", timeline[0].title)
        assertEquals("/pricing", timeline[0].subtitle)
        assertEquals(TimelineKind.CustomEvent, timeline[1].kind)
        assertEquals("signup_started", timeline[1].title)
        assertEquals(TimelineKind.Other, timeline[2].kind)
        assertEquals("Outbound click", timeline[2].title)
    }

    @Test
    fun `only events with properties get an expandable pretty-printed block`() {
        // Arrange
        val withProps = event(
            "2026-11-16 08:00:00",
            type = "custom_event",
            eventName = "checkout",
            props = buildJsonObject {
                put("plan", "pro")
                putJsonObject("cart") { put("items", 2) }
            },
        )
        val withoutProps = event("2026-11-16 08:00:01", type = "pageview", props = JsonObject(emptyMap()))

        // Act
        val timeline = buildTimeline(listOf(withProps, withoutProps), ZoneId.of("UTC"))

        // Assert
        val pretty = timeline[0].properties.orEmpty()
        assertTrue(pretty.contains("\n"))
        assertTrue(pretty.contains("\"plan\": \"pro\""))
        assertTrue(pretty.contains("\"items\": 2"))
        assertNull(timeline[1].properties)
    }

    @Test
    fun `properties decode from the JSON object in the session detail payload`() {
        // Arrange
        val json = """{"timestamp":"2026-11-16 08:00:00","type":"custom_event","event_name":"signup","props":{"plan":"pro","trial":true}}"""

        // Act
        val event = RybbitJson.json.decodeFromString(SessionDetailEvent.serializer(), json)
        val pretty = prettyProperties(event.props)

        // Assert
        assertEquals("pro", event.props?.get("plan")?.toString()?.trim('"'))
        assertTrue(pretty.orEmpty().contains("\"trial\": true"))
    }

    @Test
    fun `empty properties produce no block`() {
        assertNull(prettyProperties(JsonObject(emptyMap())))
        assertNull(prettyProperties(null))
    }

    @Test
    fun `an empty event list produces an empty timeline`() {
        assertTrue(buildTimeline(emptyList(), ZoneId.of("UTC")).isEmpty())
    }

    private fun event(
        timestamp: String,
        type: String,
        pageTitle: String? = null,
        pathname: String? = null,
        eventName: String? = null,
        props: JsonObject? = null,
    ) = SessionDetailEvent(
        timestamp = timestamp,
        type = type,
        pageTitle = pageTitle,
        pathname = pathname,
        eventName = eventName,
        props = props,
    )
}
