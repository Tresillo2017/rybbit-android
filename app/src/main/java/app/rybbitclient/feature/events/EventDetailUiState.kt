package app.rybbitclient.feature.events

import app.rybbitclient.core.model.Event
import app.rybbitclient.core.model.EventTimePoint
import app.rybbitclient.core.network.ApiException
import app.rybbitclient.core.query.AnalyticsQuery
import app.rybbitclient.core.query.TimeBucket

/** UI state for [EventDetailScreen]. Every section comes from one load, so they are always consistent. */
sealed interface EventDetailUiState {

    data object Loading : EventDetailUiState

    data class Content(
        val query: AnalyticsQuery,
        /** Occurrences of this event in the period, from the event-name counts. Null when the server did not list it. */
        val totalCount: Int?,
        val bucket: TimeBucket,
        /** Time-series rows for this event only. Empty when it is not among the top events for the period. */
        val points: List<EventTimePoint>,
        val propertyGroups: List<EventPropertyGroup>,
        val occurrences: List<Event>,
        val isRefreshing: Boolean = false,
    ) : EventDetailUiState

    data class Error(val error: ApiException) : EventDetailUiState

    data object Unavailable : EventDetailUiState
}
