package app.rybbitclient.feature.events

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import app.rybbitclient.core.model.Event
import app.rybbitclient.core.model.EventName
import app.rybbitclient.core.model.EventTimePoint
import app.rybbitclient.core.network.ApiException
import app.rybbitclient.core.query.TimeBucket
import app.rybbitclient.core.ui.components.BarItem
import app.rybbitclient.core.ui.components.BarListCard
import app.rybbitclient.core.ui.components.LineChart
import app.rybbitclient.core.ui.components.SectionCard
import app.rybbitclient.core.ui.format.formatFullNumber
import app.rybbitclient.core.ui.format.formatPercent
import app.rybbitclient.core.ui.theme.Spacing
import app.rybbitclient.feature.common.toErrorCopy

private const val PERCENT_SCALE = 100.0
private const val LOADING_OLDER_EVENTS = "Loading older events"
private const val RECENT_EVENTS_TITLE = "Recent events"
private const val LOAD_OLDER_LABEL = "Load older events"

/** Count of each custom event over time, as one line per event name. */
@Composable
fun EventsChartCard(points: List<EventTimePoint>, bucket: TimeBucket) {
    val chart = remember(points, bucket) {
        buildTimelineChart(eventTimeline(points), bucket, subject = "Custom events")
    }
    SectionCard(title = "Custom events", subtitle = "Top events for this period") {
        LineChart(
            series = chart.series,
            summary = chart.summary,
            xLabelFormatter = { index -> chart.labels.getOrElse(index) { "" } },
        )
    }
}

/** Custom event names ranked by count. Tapping a name opens EventDetail. */
@Composable
fun EventNamesCard(names: List<EventName>, onOpenEvent: (String) -> Unit) {
    val items = remember(names) { eventNameBarItems(names) }
    BarListCard(
        title = "Event names",
        subtitle = "Most frequent first",
        items = items,
        onItemClick = onOpenEvent,
    )
}

/** Maps event-name counts to ranked bar rows with each name's share of all occurrences. */
fun eventNameBarItems(names: List<EventName>): List<BarItem> {
    val total = names.sumOf { it.count ?: 0 }.toDouble()
    return names.map { name ->
        val count = (name.count ?: 0).toDouble()
        BarItem(
            key = name.eventName,
            label = name.eventName,
            value = count,
            valueText = formatFullNumber(count),
            percentText = if (total > 0.0) formatPercent(count / total * PERCENT_SCALE) else null,
        )
    }
}

/** The recent-events stream: type chips, the filtered rows, and the infinite-scroll footer. */
internal fun LazyListScope.recentEventsItems(
    stream: EventStream,
    typeFilter: EventTypeFilter,
    onTypeSelected: (EventTypeFilter) -> Unit,
    onOpenEvent: (String) -> Unit,
    onLoadMore: () -> Unit,
) {
    item(key = "recent-events-header") {
        RecentEventsHeader(selected = typeFilter, onSelected = onTypeSelected)
    }
    val visible = filterEvents(stream.events, typeFilter)
    itemsIndexed(visible, key = { index, event -> rowKey(index, event) }) { _, event ->
        EventRow(event = event, onClick = eventClick(event, onOpenEvent))
    }
    item(key = "recent-events-footer") {
        StreamFooter(
            stream = stream,
            typeFilter = typeFilter,
            isFilteredEmpty = visible.isEmpty(),
            onLoadMore = onLoadMore,
        )
    }
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
private fun RecentEventsHeader(selected: EventTypeFilter, onSelected: (EventTypeFilter) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        Text(text = RECENT_EVENTS_TITLE, style = MaterialTheme.typography.titleMedium)
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            EventTypeFilter.entries.forEach { filter ->
                FilterChip(
                    selected = filter == selected,
                    onClick = { onSelected(filter) },
                    label = { Text(filter.label) },
                    modifier = Modifier.minimumInteractiveComponentSize(),
                )
            }
        }
    }
}

/**
 * Footer of the recent-events stream. With the All chip, the next older page loads automatically as the
 * footer scrolls into view (infinite scroll). With a narrowed chip, most loaded rows are hidden, so
 * auto-loading would page through the whole history; the user asks for each page instead.
 * A failed page shows a retry button rather than retrying in a loop.
 */
@Composable
private fun StreamFooter(
    stream: EventStream,
    typeFilter: EventTypeFilter,
    isFilteredEmpty: Boolean,
    onLoadMore: () -> Unit,
) {
    val error = stream.loadMoreError
    val canAutoLoad = stream.shouldAutoLoad(typeFilter)
    LaunchedEffect(stream.nextBeforeCursor, stream.nextBeforeTimestamp, canAutoLoad) {
        if (canAutoLoad) onLoadMore()
    }
    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = Spacing.md),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Spacing.xs),
    ) {
        when {
            error != null -> LoadMoreFailure(error = error, onRetry = onLoadMore)
            stream.isLoadingMore || canAutoLoad -> CircularProgressIndicator(
                modifier = Modifier.semantics { contentDescription = LOADING_OLDER_EVENTS },
            )
            stream.hasMore -> TextButton(
                onClick = onLoadMore,
                modifier = Modifier.fillMaxWidth().minimumInteractiveComponentSize(),
            ) {
                Text(LOAD_OLDER_LABEL)
            }
            isFilteredEmpty && stream.events.isNotEmpty() -> FooterNote("No loaded events match this type.")
            stream.events.isNotEmpty() -> FooterNote("No older events.")
        }
    }
}

@Composable
private fun LoadMoreFailure(error: ApiException, onRetry: () -> Unit) {
    FooterNote(error.toErrorCopy().message)
    TextButton(onClick = onRetry, modifier = Modifier.minimumInteractiveComponentSize()) {
        Text("Try again")
    }
}

@Composable
private fun FooterNote(message: String) {
    Text(
        text = message,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
    )
}

/** Stable LazyColumn key: the index keeps keys unique even when two rows share a second and session. */
private fun rowKey(index: Int, event: Event): String =
    listOf(index, event.timestamp, event.sessionId, event.type, event.eventName).joinToString("|")

/** Tapping a named custom event opens its detail. Other rows are not tappable. */
private fun eventClick(event: Event, onOpenEvent: (String) -> Unit): (() -> Unit)? {
    val name = event.openableEventName() ?: return null
    return { onOpenEvent(name) }
}
