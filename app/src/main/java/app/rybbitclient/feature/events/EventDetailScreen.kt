package app.rybbitclient.feature.events

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import app.rybbitclient.core.data.AnalyticsRepository
import app.rybbitclient.core.model.Event
import app.rybbitclient.core.model.EventTimePoint
import app.rybbitclient.core.query.TimeBucket
import app.rybbitclient.core.ui.components.BarListCard
import app.rybbitclient.core.ui.components.KpiCard
import app.rybbitclient.core.ui.components.LineChart
import app.rybbitclient.core.ui.components.LoadingView
import app.rybbitclient.core.ui.components.SectionCard
import app.rybbitclient.core.ui.components.UnavailableView
import app.rybbitclient.core.ui.format.EMPTY_VALUE
import app.rybbitclient.core.ui.format.formatFullNumber
import app.rybbitclient.core.ui.theme.Spacing
import app.rybbitclient.di.LocalAppContainer

private const val PROPERTY_BAR_LIMIT = 5
private const val OCCURRENCE_PAGE_SIZE = AnalyticsRepository.DEFAULT_LIMIT
private const val NO_PROPERTIES_MESSAGE = "No properties were recorded for this event in this period."
private const val NOT_IN_TOP_EVENTS_MESSAGE =
    "This event is not among the most frequent custom events, so its timeline is not available for this period."

/**
 * Detail for one custom event: its count, count over time, property breakdown, and recent occurrences.
 * Tapping an occurrence opens its session.
 *
 * Parameters:
 * - [siteId]: Site.analyticsId for API calls.
 * - [eventName]: Event name, exactly as stored.
 * - [onOpenSession]: Open SessionDetail for an occurrence's session id.
 * - [onBack]: Back navigation.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventDetailScreen(
    siteId: String,
    eventName: String,
    onOpenSession: (sessionId: String) -> Unit,
    onBack: () -> Unit,
) {
    val container = LocalAppContainer.current
    val viewModel: EventDetailViewModel = viewModel(key = "event-$siteId-$eventName") {
        EventDetailViewModel(siteId, eventName, container.analyticsRepository, container.siteQueryState)
    }
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val query by container.siteQueryState.query.collectAsStateWithLifecycle()

    DetailScaffold(title = eventName, onBack = onBack) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            SiteQueryControls(
                queryState = container.siteQueryState,
                query = query,
                modifier = Modifier.padding(vertical = Spacing.sm),
            )
            when (val current = state) {
                EventDetailUiState.Loading -> LoadingView(modifier = Modifier.fillMaxSize())
                is EventDetailUiState.Error -> ApiErrorView(
                    error = current.error,
                    onRetry = viewModel::refresh,
                    modifier = Modifier.fillMaxSize(),
                )
                EventDetailUiState.Unavailable -> UnavailableView(modifier = Modifier.fillMaxSize())
                is EventDetailUiState.Content -> EventDetailList(
                    content = current,
                    eventName = eventName,
                    onRefresh = viewModel::refresh,
                    onOpenSession = onOpenSession,
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EventDetailList(
    content: EventDetailUiState.Content,
    eventName: String,
    onRefresh: () -> Unit,
    onOpenSession: (String) -> Unit,
) {
    PullToRefreshBox(
        isRefreshing = content.isRefreshing,
        onRefresh = onRefresh,
        modifier = Modifier.fillMaxSize(),
    ) {
        LazyColumn(
            contentPadding = PaddingValues(horizontal = Spacing.screenGutter, vertical = Spacing.sm),
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            item {
                KpiCard(
                    label = "Occurrences",
                    value = content.totalCount?.let { formatFullNumber(it.toDouble()) } ?: EMPTY_VALUE,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            item { EventDetailChartCard(points = content.points, bucket = content.bucket, eventName = eventName) }
            if (content.propertyGroups.isEmpty()) {
                item { PropertiesEmptyCard() }
            }
            items(content.propertyGroups, key = { it.key }) { group ->
                BarListCard(
                    title = group.key,
                    subtitle = "Property values",
                    items = propertyBarItems(group),
                    limit = PROPERTY_BAR_LIMIT,
                )
            }
            item { OccurrencesCard(occurrences = content.occurrences, onOpenSession = onOpenSession) }
        }
    }
}

@Composable
private fun EventDetailChartCard(points: List<EventTimePoint>, bucket: TimeBucket, eventName: String) {
    val chart = remember(points, bucket) {
        buildTimelineChart(eventTimeline(points), bucket, subject = eventName)
    }
    SectionCard(title = "Occurrences over time", subtitle = "Count per bucket") {
        LineChart(
            series = chart.series,
            summary = chart.summary,
            xLabelFormatter = { index -> chart.labels.getOrElse(index) { "" } },
        )
        if (chart.series.isEmpty()) {
            Text(
                text = NOT_IN_TOP_EVENTS_MESSAGE,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun PropertiesEmptyCard() {
    SectionCard(title = "Properties") {
        Text(
            text = NO_PROPERTIES_MESSAGE,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun OccurrencesCard(occurrences: List<Event>, onOpenSession: (String) -> Unit) {
    val isCapped = occurrences.size >= OCCURRENCE_PAGE_SIZE
    SectionCard(
        title = "Recent occurrences",
        subtitle = if (isCapped) "Latest $OCCURRENCE_PAGE_SIZE in this period" else "Newest first",
    ) {
        if (occurrences.isEmpty()) {
            Text(
                text = "No recent occurrences in this period.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        occurrences.forEach { event ->
            EventRow(event = event, onClick = sessionClick(event.sessionId, onOpenSession))
        }
    }
}

private fun sessionClick(sessionId: String?, onOpenSession: (String) -> Unit): (() -> Unit)? {
    val id = sessionId?.takeIf { it.isNotBlank() } ?: return null
    return { onOpenSession(id) }
}
