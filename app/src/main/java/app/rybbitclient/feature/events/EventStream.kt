package app.rybbitclient.feature.events

import app.rybbitclient.core.model.Event
import app.rybbitclient.core.model.EventsResponse
import app.rybbitclient.core.network.ApiException
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

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
 * Event rows loaded so far, newest first. The next older page is requested with [nextBeforeCursor] when the
 * server provides one, otherwise with [nextBeforeTimestamp] (legacy, self-hosted servers). Both null means
 * the server has no more rows.
 */
data class EventStream(
    val events: List<Event> = emptyList(),
    val nextBeforeCursor: String? = null,
    val nextBeforeTimestamp: String? = null,
    val isLoadingMore: Boolean = false,
    val loadMoreError: ApiException? = null,
) {
    val hasMore: Boolean get() = nextBeforeCursor != null || nextBeforeTimestamp != null
}

/** The first page of the stream. */
fun EventsResponse.toStream(): EventStream = EventStream(
    events = data,
    nextBeforeCursor = olderCursor(),
    nextBeforeTimestamp = olderTimestamp(),
)

/**
 * Adds an older page after the rows already loaded. Legacy `before_timestamp` pages overlap the boundary
 * second, so rows already held are dropped.
 */
fun EventStream.appended(page: EventsResponse): EventStream {
    val known = events.map(Event::dedupeKey).toSet()
    return copy(
        events = events + page.data.filterNot { it.dedupeKey() in known },
        nextBeforeCursor = page.olderCursor(),
        nextBeforeTimestamp = page.olderTimestamp(),
        isLoadingMore = false,
        loadMoreError = null,
    )
}

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

/**
 * Timestamp for the next older page when the server gives no cursor. `before_timestamp` excludes its boundary
 * second, so the bound is one second past the oldest loaded row; the overlap is removed by [appended].
 */
private fun EventsResponse.olderTimestamp(): String? {
    val current = cursor?.takeIf { it.hasMore && it.oldestCursor == null } ?: return null
    return current.oldestTimestamp?.let(::secondAfter)
}

private val LEGACY_TIMESTAMP_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")

/** `yyyy-MM-dd HH:mm:ss` one second later; fractional seconds are dropped. Null when unparseable. */
internal fun secondAfter(timestamp: String): String? = try {
    LocalDateTime.parse(timestamp.substringBefore('.').trim(), LEGACY_TIMESTAMP_FORMAT)
        .plusSeconds(1)
        .format(LEGACY_TIMESTAMP_FORMAT)
} catch (unparseable: DateTimeParseException) {
    null
}

/** Identifies one event row for de-duplication across overlapping pages. */
private fun Event.dedupeKey(): List<String?> = listOf(timestamp, sessionId, type, pathname, eventName)
