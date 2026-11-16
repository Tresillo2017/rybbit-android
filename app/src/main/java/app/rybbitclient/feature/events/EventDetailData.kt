package app.rybbitclient.feature.events

import app.rybbitclient.core.model.Event
import app.rybbitclient.core.model.EventPropertyCount
import app.rybbitclient.core.query.AnalyticsQuery
import app.rybbitclient.core.query.Filter
import app.rybbitclient.core.query.FilterType
import app.rybbitclient.core.ui.components.BarItem
import app.rybbitclient.core.ui.format.formatFullNumber
import app.rybbitclient.core.ui.format.formatPercent

/** One value of a property, with how often it was recorded. */
data class EventPropertyValue(val label: String, val count: Int)

/** All recorded values of one property key, most frequent first. */
data class EventPropertyGroup(val key: String, val values: List<EventPropertyValue>)

private const val EVENT_NAME_PARAMETER = "event_name"
private const val NO_PROPERTY_VALUE_LABEL = "(empty)"
private const val PERCENT_SCALE = 100.0

/**
 * Groups `GET /events/properties` rows by property key. Keys sort alphabetically, values by count
 * (highest first). Rows with a blank key are dropped; blank values show as "(empty)"; a missing count is zero.
 */
fun groupEventProperties(rows: List<EventPropertyCount>): List<EventPropertyGroup> = rows
    .filter { it.propertyKey.isNotBlank() }
    .groupBy { it.propertyKey }
    .map { (key, group) ->
        EventPropertyGroup(
            key = key,
            values = group
                .map { row ->
                    EventPropertyValue(
                        label = row.propertyValue?.takeIf { it.isNotBlank() } ?: NO_PROPERTY_VALUE_LABEL,
                        count = row.count ?: 0,
                    )
                }
                .sortedByDescending { it.count },
        )
    }
    .sortedBy { it.key }

/** Bar rows for one property group, each with its share of that group's total. */
fun propertyBarItems(group: EventPropertyGroup): List<BarItem> {
    val total = group.values.sumOf { it.count }.toDouble()
    return group.values.map { value ->
        val count = value.count.toDouble()
        BarItem(
            key = value.label,
            label = value.label,
            value = count,
            valueText = formatFullNumber(count),
            percentText = if (total > 0.0) formatPercent(count / total * PERCENT_SCALE) else null,
        )
    }
}

/** The rows that belong to [eventName]. The server narrows by session, so rows of other events can come back. */
fun occurrencesOf(events: List<Event>, eventName: String): List<Event> = events.filter { it.eventName == eventName }

/** [query] narrowed to sessions that contain [eventName], replacing any earlier event-name filter. */
fun withEventNameFilter(query: AnalyticsQuery, eventName: String): AnalyticsQuery = query.copy(
    filters = query.filters.filterNot { it.parameter == EVENT_NAME_PARAMETER } +
        Filter(parameter = EVENT_NAME_PARAMETER, type = FilterType.Equals, values = listOf(eventName)),
)
