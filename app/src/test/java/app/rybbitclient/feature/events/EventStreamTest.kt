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
    fun `a page that reports more rows without a cursor does not keep paging`() {
        // Arrange
        val page = EventsResponse(data = listOf(Event(type = "pageview")), cursor = EventCursor(hasMore = true, oldestCursor = null))

        // Act
        val stream = page.toStream()

        // Assert
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
