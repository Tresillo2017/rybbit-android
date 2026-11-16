package app.rybbitclient.feature.realtime

import app.rybbitclient.core.model.Event
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Feed reduction and merge: which events show, how overlapping polls dedupe, and ordering. */
class RealtimeFeedTest {

    @Test
    fun `only pageviews and custom events reach the feed`() {
        val rows = RealtimeFeed.rowsFrom(
            listOf(
                event(type = "pageview", pathname = "/home", sessionId = "a"),
                event(type = "custom_event", eventName = "signup", sessionId = "b"),
                event(type = "outbound", pathname = "/x", sessionId = "c"),
                event(type = "error", pathname = "/y", sessionId = "d"),
            ),
        )

        assertEquals(listOf("/home", "signup"), rows.map { it.title })
        assertTrue(rows.last().isCustomEvent)
    }

    @Test
    fun `events in the same second from different sessions stay distinct`() {
        val sameSecond = listOf(
            event(pathname = "/pricing", sessionId = "s1", timestamp = "2026-11-16 08:41:48"),
            event(pathname = "/pricing", sessionId = "s2", timestamp = "2026-11-16 08:41:48"),
            event(pathname = "/pricing", sessionId = "s3", timestamp = "2026-11-16 08:41:48"),
        )

        val rows = RealtimeFeed.rowsFrom(sameSecond)

        assertEquals(3, rows.map { it.key }.toSet().size)
    }

    @Test
    fun `the server cursor is the dedupe key when present`() {
        val first = event(sessionId = "s1", eventCursor = "1706710920412_9F3C2A61B07E4D18")
        val second = event(sessionId = "other", pathname = "/elsewhere", eventCursor = "1706710920412_9F3C2A61B07E4D18")

        assertEquals(RealtimeFeed.dedupeKey(first), RealtimeFeed.dedupeKey(second))
    }

    @Test
    fun `rows without a readable timestamp are dropped`() {
        val rows = RealtimeFeed.rowsFrom(listOf(event(timestamp = null), event(timestamp = "not a date")))

        assertTrue(rows.isEmpty())
    }

    @Test
    fun `merging overlapping polls keeps one row per event, newest first`() {
        val older = RealtimeFeed.rowsFrom(listOf(event(sessionId = "a", timestamp = "2026-11-16 08:41:40")))
        val poll = RealtimeFeed.rowsFrom(
            listOf(
                event(sessionId = "b", timestamp = "2026-11-16 08:41:50"),
                event(sessionId = "a", timestamp = "2026-11-16 08:41:40"),
            ),
        )

        val merged = RealtimeFeed.merge(existing = older, incoming = poll)

        assertEquals(2, merged.size)
        assertEquals(listOf("b", "a"), merged.map { it.sessionId })
        assertTrue(merged[0].timestampMillis > merged[1].timestampMillis)
    }

    @Test
    fun `merge caps the feed at the limit and keeps the newest rows`() {
        val rows = RealtimeFeed.rowsFrom((0 until 10).map { second -> event(sessionId = "s$second", timestamp = "2026-11-16 08:41:%02d".format(second)) })

        val merged = RealtimeFeed.merge(existing = emptyList(), incoming = rows, limit = 3)

        assertEquals(listOf("s9", "s8", "s7"), merged.map { it.sessionId })
    }

    @Test
    fun `identical keys from different polls are not duplicated`() {
        val first = RealtimeFeed.rowsFrom(listOf(event(sessionId = "x", timestamp = "2026-11-16 08:41:49")))
        val second = RealtimeFeed.rowsFrom(listOf(event(sessionId = "x", timestamp = "2026-11-16 08:41:49")))

        val merged = RealtimeFeed.merge(existing = first, incoming = second)

        assertEquals(1, merged.size)
        assertNotEquals(0L, merged.single().timestampMillis)
    }

    private fun event(
        type: String? = "pageview",
        pathname: String? = "/",
        eventName: String? = null,
        sessionId: String? = "session",
        timestamp: String? = "2026-11-16 08:41:49",
        eventCursor: String? = null,
    ) = Event(
        timestamp = timestamp,
        eventName = eventName,
        sessionId = sessionId,
        pathname = pathname,
        type = type,
        country = "GB",
        browser = "Firefox",
        eventCursor = eventCursor,
    )
}
