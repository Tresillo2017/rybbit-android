package app.rybbitclient.feature.sessions

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import app.rybbitclient.di.LocalAppContainer

/**
 * The Sessions tab. The shared date range and filters sit on top; the list pages in as the user scrolls.
 * Tapping a session opens its detail.
 *
 * Parameters:
 * - [siteId]: Site.analyticsId for API calls.
 * - [onOpenSession]: Open SessionDetail.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SessionsScreen(
    siteId: String,
    onOpenSession: (sessionId: String) -> Unit,
) {
    val container = LocalAppContainer.current
    val viewModel: SessionsViewModel = viewModel(key = "sessions-$siteId") {
        SessionsViewModel(siteId, container.analyticsRepository, container.siteQueryState.query)
    }
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val query by container.siteQueryState.query.collectAsStateWithLifecycle()

    Column(modifier = Modifier.fillMaxSize()) {
        SiteQueryHeader(
            query = query,
            onRangeSelected = container.siteQueryState::setRange,
            onRemoveFilter = container.siteQueryState::removeFilter,
            onClearFilters = container.siteQueryState::clearFilters,
        )
        PagedListBody(
            state = state,
            empty = EmptyCopy(
                title = "No sessions yet",
                message = "No sessions match this date range or filter. Try a wider range.",
            ),
            itemKey = { it.sessionId },
            onRefresh = viewModel::refresh,
            onLoadMore = viewModel::loadMore,
            modifier = Modifier.fillMaxSize(),
        ) { session ->
            SessionRow(session = session, onClick = { onOpenSession(session.sessionId) })
        }
    }
}
