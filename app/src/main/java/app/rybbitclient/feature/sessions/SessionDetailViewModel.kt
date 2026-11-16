package app.rybbitclient.feature.sessions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.rybbitclient.core.data.AnalyticsRepository
import app.rybbitclient.core.model.SessionDetail
import app.rybbitclient.core.model.SessionDetailEvent
import app.rybbitclient.core.model.SessionDetailResponse
import app.rybbitclient.core.network.ApiException
import app.rybbitclient.core.network.FeatureUnavailable
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.ZoneId

/** UI state for [SessionDetailScreen]. Events accumulate across pages; [timeline] is derived from them. */
sealed interface SessionDetailUiState {

    data object Loading : SessionDetailUiState

    data class Content(
        val session: SessionDetail?,
        val events: List<SessionDetailEvent>,
        val timeline: List<TimelineEntry>,
        val hasMore: Boolean,
        val isRefreshing: Boolean = false,
        val isLoadingMore: Boolean = false,
        val loadMoreError: ApiException? = null,
    ) : SessionDetailUiState

    data class Error(val error: ApiException) : SessionDetailUiState

    data object Unavailable : SessionDetailUiState
}

/**
 * Loads one session: the header and the first page of events, then further pages on demand. Timestamps are
 * shown in [zone] (the device zone by default). Page 2 or later failing keeps the events already loaded.
 */
class SessionDetailViewModel(
    private val siteId: String,
    private val sessionId: String,
    private val analytics: AnalyticsRepository,
    private val zone: ZoneId = ZoneId.systemDefault(),
) : ViewModel() {

    private val mutableState = MutableStateFlow<SessionDetailUiState>(SessionDetailUiState.Loading)
    val uiState: StateFlow<SessionDetailUiState> = mutableState.asStateFlow()

    private var requestJob: Job? = null

    init {
        reload()
    }

    /** Pull-to-refresh and retry: reloads from the first event and keeps the timeline on screen meanwhile. */
    fun refresh() = reload()

    /** Appends the next page of events. No-op while a page is loading or when the session has no more events. */
    fun loadMore() {
        val current = mutableState.value as? SessionDetailUiState.Content ?: return
        if (!current.hasMore || current.isLoadingMore || current.isRefreshing) return
        mutableState.value = current.copy(isLoadingMore = true, loadMoreError = null)
        requestJob = viewModelScope.launch {
            try {
                val page = analytics.sessionEventsPage(siteId, sessionId, offset = current.events.size, limit = PAGE_SIZE)
                mutableState.update { latest ->
                    (latest as? SessionDetailUiState.Content)?.let { content ->
                        content.withEvents(events = content.events + page.events, pagination = page)
                    } ?: latest
                }
            } catch (error: ApiException) {
                mutableState.update { latest ->
                    (latest as? SessionDetailUiState.Content)?.copy(isLoadingMore = false, loadMoreError = error) ?: latest
                }
            }
        }
    }

    private fun reload() {
        requestJob?.cancel()
        mutableState.update { current ->
            (current as? SessionDetailUiState.Content)
                ?.copy(isRefreshing = true, isLoadingMore = false, loadMoreError = null)
                ?: SessionDetailUiState.Loading
        }
        requestJob = viewModelScope.launch {
            mutableState.value = try {
                val first = analytics.sessionEventsPage(siteId, sessionId, offset = 0, limit = PAGE_SIZE)
                contentFrom(first)
            } catch (error: ApiException) {
                error.toDetailState()
            }
        }
    }

    private fun contentFrom(response: SessionDetailResponse): SessionDetailUiState.Content =
        SessionDetailUiState.Content(
            session = response.session,
            events = response.events,
            timeline = buildTimeline(response.events, zone),
            hasMore = hasMoreEvents(response),
        )

    private fun SessionDetailUiState.Content.withEvents(
        events: List<SessionDetailEvent>,
        pagination: SessionDetailResponse,
    ): SessionDetailUiState.Content = copy(
        events = events,
        timeline = buildTimeline(events, zone),
        hasMore = hasMoreEvents(pagination),
        isLoadingMore = false,
        loadMoreError = null,
    )

    private fun ApiException.toDetailState(): SessionDetailUiState = when (this) {
        is FeatureUnavailable -> SessionDetailUiState.Unavailable
        else -> SessionDetailUiState.Error(this)
    }

    companion object {
        const val PAGE_SIZE = 100

        /** The server says so when it sends pagination; otherwise a full page suggests more. */
        fun hasMoreEvents(response: SessionDetailResponse): Boolean =
            response.pagination?.hasMore ?: (response.events.size >= PAGE_SIZE)
    }
}
