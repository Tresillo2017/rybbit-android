package app.rybbitclient.feature.events

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ExperimentalMaterial3Api
import app.rybbitclient.core.ui.components.ExpressiveRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import app.rybbitclient.core.ui.components.EmptyView
import app.rybbitclient.core.ui.components.LoadingView
import app.rybbitclient.core.ui.components.UnavailableView
import app.rybbitclient.core.ui.theme.Spacing
import app.rybbitclient.di.LocalAppContainer

private const val EMPTY_PERIOD_TITLE = "No events in this period"
private const val EMPTY_PERIOD_MESSAGE = "Custom events and pageviews appear here once your tracking script sends them."

/**
 * The Events tab: custom events over time, the event names ranked by count, and a recent-events stream
 * with type chips. Date range and filters come from the shared site query state.
 *
 * Parameters:
 * - [siteId]: Site.analyticsId for API calls.
 * - [onOpenEvent]: Open EventDetail for a custom event name.
 */
@Composable
fun EventsScreen(
    siteId: String,
    onOpenEvent: (eventName: String) -> Unit,
) {
    val container = LocalAppContainer.current
    val viewModel: EventsViewModel = viewModel(key = "events-$siteId") {
        EventsViewModel(siteId, container.analyticsRepository, container.siteQueryState)
    }
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val query by container.siteQueryState.query.collectAsStateWithLifecycle()

    Column(modifier = Modifier.fillMaxSize()) {
        SiteQueryControls(
            queryState = container.siteQueryState,
            query = query,
            modifier = Modifier.padding(vertical = Spacing.sm),
        )
        when (val current = state) {
            EventsUiState.Loading -> LoadingView(modifier = Modifier.fillMaxSize())
            is EventsUiState.Error -> ApiErrorView(
                error = current.error,
                onRetry = viewModel::refresh,
                modifier = Modifier.fillMaxSize(),
            )
            EventsUiState.Unavailable -> UnavailableView(modifier = Modifier.fillMaxSize())
            is EventsUiState.Content -> EventsContentList(
                content = current,
                onRefresh = viewModel::refresh,
                onTypeSelected = viewModel::selectType,
                onLoadMore = viewModel::loadMore,
                onOpenEvent = onOpenEvent,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EventsContentList(
    content: EventsUiState.Content,
    onRefresh: () -> Unit,
    onTypeSelected: (EventTypeFilter) -> Unit,
    onLoadMore: () -> Unit,
    onOpenEvent: (String) -> Unit,
) {
    ExpressiveRefreshBox(
        isRefreshing = content.isRefreshing,
        onRefresh = onRefresh,
        modifier = Modifier.fillMaxSize(),
    ) {
        LazyColumn(
            contentPadding = PaddingValues(
                horizontal = Spacing.screenGutter,
                vertical = Spacing.sm,
            ),
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            if (content.isEmptyPeriod()) {
                item { EmptyView(title = EMPTY_PERIOD_TITLE, message = EMPTY_PERIOD_MESSAGE) }
            }
            item { EventsChartCard(points = content.points, bucket = content.bucket) }
            item { EventNamesCard(names = content.names, onOpenEvent = onOpenEvent) }
            recentEventsItems(
                stream = content.stream,
                typeFilter = content.typeFilter,
                onTypeSelected = onTypeSelected,
                onOpenEvent = onOpenEvent,
                onLoadMore = onLoadMore,
            )
        }
    }
}

private fun EventsUiState.Content.isEmptyPeriod(): Boolean =
    points.isEmpty() && names.isEmpty() && stream.events.isEmpty()
