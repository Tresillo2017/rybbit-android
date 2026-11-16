package app.rybbitclient.feature.events

import app.rybbitclient.core.model.Event
import app.rybbitclient.core.model.EventCursor
import app.rybbitclient.core.model.EventsResponse
import app.rybbitclient.core.network.Server
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EventStreamTest {

    @Test
    fun `type chips match the wire values of the event type`() {
        assertTrue(EventTypeFilter.Errors.matches(Event(type = "error")))
        assertFalse(EventTypeFilter.Custom.matches(Event(type = "pageview")))
        assertTrue(EventTypeFilter.All.matches(Event(type = null)))
    }

    @Test
    fun `a page without more rows has no cursor for the next page`() {
        // Arrange
        val page = EventsResponse(data = listOf(Event(type = "pageview")), cursor = EventCursor(hasMore = false, oldestCursor = "c"))

        // Act
        val stream = page.toStream()

        // Assert
        assertFalse(stream.hasMore)
    }

    @Test
    fun `a page without oldestCursor pages older rows by timestamp one second past the oldest row`() {
        // Arrange
        val page = EventsResponse(
            data = listOf(Event(type = "pageview", timestamp = "2026-11-16 10:00:00")),
            cursor = EventCursor(hasMore = true, oldestCursor = null, oldestTimestamp = "2026-11-16 09:59:58.123"),
        )

        // Act
        val stream = page.toStream()

        // Assert
        assertTrue(stream.hasMore)
        assertEquals(null, stream.nextBeforeCursor)
        assertEquals("2026-11-16 09:59:59", stream.nextBeforeTimestamp)
    }

    @Test
    fun `a page with hasMore but neither cursor nor timestamp has nothing to page with`() {
        // Arrange
        val page = EventsResponse(data = listOf(Event(type = "pageview")), cursor = EventCursor(hasMore = true, oldestCursor = null))

        // Act
        val stream = page.toStream()

        // Assert
        assertFalse(stream.hasMore)
    }

    @Test
    fun `appending a timestamp page drops the rows of the overlapping boundary second`() {
        // Arrange
        val first = EventsResponse(
            data = listOf(Event(eventName = "a", timestamp = "2026-11-16 10:00:00", sessionId = "s1", type = "pageview", pathname = "/")),
            cursor = EventCursor(hasMore = true, oldestCursor = null, oldestTimestamp = "2026-11-16 10:00:00"),
        )
        val second = EventsResponse(
            data = listOf(
                Event(eventName = "a", timestamp = "2026-11-16 10:00:00", sessionId = "s1", type = "pageview", pathname = "/"),
                Event(eventName = "b", timestamp = "2026-11-16 09:59:00", sessionId = "s2", type = "pageview", pathname = "/x"),
            ),
            cursor = EventCursor(hasMore = false, oldestCursor = null, oldestTimestamp = "2026-11-16 09:59:00"),
        )

        // Act
        val stream = first.toStream().appended(second)

        // Assert
        assertEquals(listOf("a", "b"), stream.events.map { it.eventName })
        assertFalse(stream.hasMore)
    }

    @Test
    fun `only the All chip auto-loads older pages`() {
        // Arrange
        val stream = EventStream(events = listOf(Event(type = "pageview")), nextBeforeCursor = "c1")

        // Act and assert
        assertTrue(stream.shouldAutoLoad(EventTypeFilter.All))
        assertFalse(stream.shouldAutoLoad(EventTypeFilter.Errors))
        assertFalse(stream.shouldAutoLoad(EventTypeFilter.Custom))
    }

    @Test
    fun `auto-load stops while a page is loading or after a failure`() {
        // Arrange
        val stream = EventStream(nextBeforeCursor = "c1")

        // Act and assert
        assertFalse(stream.startingLoadMore().shouldAutoLoad(EventTypeFilter.All))
        assertFalse(stream.failedLoadMore(Server(status = 500, code = null, serverMessage = null, resolution = null)).shouldAutoLoad(EventTypeFilter.All))
        assertFalse(EventStream().shouldAutoLoad(EventTypeFilter.All))
    }

    @Test
    fun `appending a page keeps the rows in order and moves the cursor on`() {
        // Arrange
        val first = EventsResponse(data = listOf(Event(eventName = "a")), cursor = EventCursor(hasMore = true, oldestCursor = "c1"))
        val second = EventsResponse(data = listOf(Event(eventName = "b")), cursor = EventCursor(hasMore = true, oldestCursor = "c2"))

        // Act
        val stream = first.toStream().startingLoadMore().appended(second)

        // Assert
        assertEquals(listOf("a", "b"), stream.events.map { it.eventName })
        assertEquals("c2", stream.nextBeforeCursor)
        assertFalse(stream.isLoadingMore)
    }
}
