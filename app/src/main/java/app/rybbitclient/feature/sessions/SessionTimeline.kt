package app.rybbitclient.feature.sessions

import app.rybbitclient.core.model.SessionDetailEvent
import app.rybbitclient.core.network.RybbitTimestamps
import app.rybbitclient.core.ui.format.EMPTY_VALUE
import app.rybbitclient.core.ui.format.formatDuration
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/** What a timeline row represents, which decides its icon and whether it can expand. */
enum class TimelineKind { Pageview, CustomEvent, Other }

/**
 * One row of the session timeline. [timeLabel] is local wall time in the device zone. [sinceLabel] is the
 * gap from the previous timestamped event (`+1m 05s`), null for the first event. [properties] is pretty
 * JSON for events that carry properties, null otherwise.
 */
data class TimelineEntry(
    val id: String,
    val kind: TimelineKind,
    val title: String,
    val subtitle: String?,
    val timeLabel: String,
    val sinceLabel: String?,
    val properties: String?,
)

private const val TYPE_PAGEVIEW = "pageview"
private const val TYPE_CUSTOM_EVENT = "custom_event"
private val TIME_OF_DAY: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss")
private val PRETTY_JSON = Json { prettyPrint = true }

/**
 * Maps events (oldest first, UTC timestamps) to timeline rows in [zone]. A row with an unparseable timestamp
 * shows an em dash and does not reset the gap measured for the next row.
 */
fun buildTimeline(events: List<SessionDetailEvent>, zone: ZoneId): List<TimelineEntry> {
    val instants = events.map { RybbitTimestamps.parseUtcOrNull(it.timestamp) }
    // previousKnown[i] is the last timestamp strictly before event i that parsed successfully.
    val previousKnown = instants.runningFold<Instant?, Instant?>(null) { known, current -> current ?: known }
    return events.mapIndexed { index, event ->
        timelineEntry(
            index = index,
            event = event,
            instant = instants[index],
            previous = previousKnown[index],
            zone = zone,
        )
    }
}

/** Pretty-printed JSON for event properties, or null when there are none. */
fun prettyProperties(properties: JsonObject?): String? =
    properties?.takeIf { it.isNotEmpty() }?.let { PRETTY_JSON.encodeToString(JsonObject.serializer(), it) }

private fun timelineEntry(
    index: Int,
    event: SessionDetailEvent,
    instant: Instant?,
    previous: Instant?,
    zone: ZoneId,
): TimelineEntry {
    val kind = kindOf(event.type)
    return TimelineEntry(
        id = "$index",
        kind = kind,
        title = titleOf(kind, event, type = event.type.orEmpty()),
        subtitle = subtitleOf(kind, event),
        timeLabel = instant?.atZone(zone)?.format(TIME_OF_DAY) ?: EMPTY_VALUE,
        sinceLabel = sinceLabel(previous, instant),
        properties = prettyProperties(event.props),
    )
}

private fun kindOf(type: String?): TimelineKind = when (type) {
    TYPE_PAGEVIEW -> TimelineKind.Pageview
    TYPE_CUSTOM_EVENT -> TimelineKind.CustomEvent
    else -> TimelineKind.Other
}

private fun titleOf(kind: TimelineKind, event: SessionDetailEvent, type: String): String = when (kind) {
    TimelineKind.Pageview -> event.pageTitle?.takeIf(String::isNotBlank) ?: event.pathname?.takeIf(String::isNotBlank) ?: "Pageview"
    TimelineKind.CustomEvent -> event.eventName?.takeIf(String::isNotBlank) ?: "Custom event"
    TimelineKind.Other -> humanize(type)
}

private fun subtitleOf(kind: TimelineKind, event: SessionDetailEvent): String? = when (kind) {
    TimelineKind.Pageview -> event.pathname?.takeIf(String::isNotBlank)
    TimelineKind.CustomEvent -> event.pathname?.takeIf(String::isNotBlank)
    TimelineKind.Other -> event.eventName?.takeIf(String::isNotBlank) ?: event.pathname?.takeIf(String::isNotBlank)
}

private fun sinceLabel(previous: Instant?, current: Instant?): String? {
    if (previous == null || current == null) return null
    val seconds = Duration.between(previous, current).seconds.coerceAtLeast(0L)
    return "+${formatDuration(seconds)}"
}

/** `outbound_click` -> `Outbound click`. */
private fun humanize(type: String): String {
    if (type.isBlank()) return "Event"
    val words = type.replace('_', ' ').trim()
    return words.replaceFirstChar { it.uppercaseChar() }
}
