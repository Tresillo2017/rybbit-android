package app.rybbitclient.feature.events

import app.rybbitclient.core.model.EventName
import app.rybbitclient.core.model.EventTimePoint
import app.rybbitclient.core.network.ApiException
import app.rybbitclient.core.query.AnalyticsQuery
import app.rybbitclient.core.query.TimeBucket

/** UI state for [EventsScreen]. [Content.isRefreshing] keeps the list on screen during a reload. */
sealed interface EventsUiState {

    data object Loading : EventsUiState

    data class Content(
        val query: AnalyticsQuery,
        val bucket: TimeBucket,
        val points: List<EventTimePoint>,
        val names: List<EventName>,
        val stream: EventStream,
        val typeFilter: EventTypeFilter = EventTypeFilter.All,
        val isRefreshing: Boolean = false,
    ) : EventsUiState

    data class Error(val error: ApiException) : EventsUiState

    /** The instance does not offer these reports (a 404 `API_ROUTE_NOT_FOUND`). Calm, never red. */
    data object Unavailable : EventsUiState
}
