package app.rybbitclient.feature.realtime

import app.rybbitclient.core.model.Event
import app.rybbitclient.core.network.RybbitTimestamps

/** Newest rows kept in memory. Older rows fall off the bottom as new ones arrive. */
const val REALTIME_FEED_LIMIT = 100

private const val PAGEVIEW_TYPE = "pageview"
private const val CUSTOM_EVENT_TYPE = "custom_event"
private const val UNTITLED_PAGE = "/"
private const val UNNAMED_EVENT = "Custom event"
private val VISIBLE_TYPES = setOf(PAGEVIEW_TYPE, CUSTOM_EVENT_TYPE)

/** One line of the live feed, already reduced to what the screen shows. */
data class FeedRow(
    val key: String,
    val timestampMillis: Long,
    val title: String,
    val isCustomEvent: Boolean,
    val countryCode: String?,
    val browser: String?,
    val sessionId: String?,
)

/**
 * Turns polled events into feed rows and merges polls together. Each poll re-reads the newest page,
 * so consecutive polls overlap; [merge] drops rows whose key is already on screen.
 */
object RealtimeFeed {

    /** Pageviews and custom events only. Other event types (outbound clicks, errors) are skipped. */
    fun rowsFrom(events: List<Event>): List<FeedRow> = events.mapNotNull(::toRow)

    /** Newest first, unique by [dedupeKey], capped at [limit]. The incoming rows win on duplicates. */
    fun merge(existing: List<FeedRow>, incoming: List<FeedRow>, limit: Int = REALTIME_FEED_LIMIT): List<FeedRow> =
        (incoming + existing)
            .distinctBy { it.key }
            .sortedByDescending { it.timestampMillis }
            .take(limit)

    /**
     * Server cursor when the event has one. Otherwise the fields that together identify an event:
     * two events in the same second from different sessions stay separate.
     */
    fun dedupeKey(event: Event): String = event.eventCursor ?: listOf(
        event.timestamp,
        event.sessionId,
        event.type,
        event.eventName,
        event.pathname,
        event.querystring,
    ).joinToString(separator = "|")

    private fun toRow(event: Event): FeedRow? {
        if (event.type != null && event.type !in VISIBLE_TYPES) return null
        val timestamp = RybbitTimestamps.parseUtcOrNull(event.timestamp)?.toEpochMilli() ?: return null
        val isCustom = event.type == CUSTOM_EVENT_TYPE
        return FeedRow(
            key = dedupeKey(event),
            timestampMillis = timestamp,
            title = titleFor(event, isCustom),
            isCustomEvent = isCustom,
            countryCode = event.country?.takeIf { it.isNotBlank() },
            browser = event.browser?.takeIf { it.isNotBlank() },
            sessionId = event.sessionId?.takeIf { it.isNotBlank() },
        )
    }

    private fun titleFor(event: Event, isCustom: Boolean): String =
        if (isCustom) {
            event.eventName?.takeIf { it.isNotBlank() } ?: UNNAMED_EVENT
        } else {
            event.pathname?.takeIf { it.isNotBlank() } ?: UNTITLED_PAGE
        }
}
