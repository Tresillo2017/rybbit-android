package app.rybbitclient.feature.events

import app.rybbitclient.core.model.Event
import app.rybbitclient.core.model.EventsResponse
import app.rybbitclient.core.network.ApiException

/**
 * Type chips for the recent-events stream. The server has no type filter, so these narrow the rows
 * already loaded on the client. [types] holds the wire values of `Event.type`; null means every type.
 */
enum class EventTypeFilter(val label: String, private val types: Set<String>?) {
    All("All", null),
    Pageviews("Pageviews", setOf("pageview")),
    Custom("Custom", setOf("custom_event")),
    Outbound("Outbound", setOf("outbound")),
    Errors("Errors", setOf("error"));

    fun matches(event: Event): Boolean = types == null || event.type in types
}

/**
 * Event rows loaded so far, newest first. [nextBeforeCursor] is the cursor for the next, older page;
 * null means the server has no more rows.
 */
data class EventStream(
    val events: List<Event> = emptyList(),
    val nextBeforeCursor: String? = null,
    val isLoadingMore: Boolean = false,
    val loadMoreError: ApiException? = null,
) {
    val hasMore: Boolean get() = nextBeforeCursor != null
}

/** The first page of the stream. */
fun EventsResponse.toStream(): EventStream = EventStream(events = data, nextBeforeCursor = olderCursor())

/** Adds an older page after the rows already loaded. */
fun EventStream.appended(page: EventsResponse): EventStream = copy(
    events = events + page.data,
    nextBeforeCursor = page.olderCursor(),
    isLoadingMore = false,
    loadMoreError = null,
)

fun EventStream.startingLoadMore(): EventStream = copy(isLoadingMore = true, loadMoreError = null)

fun EventStream.failedLoadMore(error: ApiException): EventStream = copy(isLoadingMore = false, loadMoreError = error)

/**
 * Whether the stream should fetch the next page by itself. Only the All chip auto-loads: a narrowed chip
 * hides most loaded rows, so auto-loading would page through the whole history.
 */
fun EventStream.shouldAutoLoad(typeFilter: EventTypeFilter): Boolean =
    typeFilter == EventTypeFilter.All && hasMore && !isLoadingMore && loadMoreError == null

/** Rows that match the selected type chip. */
fun filterEvents(events: List<Event>, filter: EventTypeFilter): List<Event> = events.filter(filter::matches)

/** The cursor for the next older page, or null when the server reports no more rows. */
private fun EventsResponse.olderCursor(): String? = cursor?.takeIf { it.hasMore }?.oldestCursor
