package app.rybbitclient.feature.events

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.rybbitclient.core.data.AnalyticsRepository
import app.rybbitclient.core.network.ApiException
import app.rybbitclient.core.network.FeatureUnavailable
import app.rybbitclient.core.query.AnalyticsQuery
import app.rybbitclient.core.query.SiteQueryState
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private const val PAGE_SIZE = AnalyticsRepository.DEFAULT_LIMIT

/** The server arguments that request one older page. */
private data class PageToken(val cursor: String?, val timestamp: String?) {
    val isPresent: Boolean get() = cursor != null || timestamp != null
}

private fun EventStream.pageToken(): PageToken = PageToken(nextBeforeCursor, nextBeforeTimestamp)

/**
 * ViewModel for the Events tab. Reloads whenever the shared site query changes or [refresh] is called.
 * The recent-events stream pages backwards with the server cursor; type chips filter loaded rows locally.
 */
class EventsViewModel(
    private val siteId: String,
    private val analytics: AnalyticsRepository,
    private val queryState: SiteQueryState,
) : ViewModel() {

    private val mutableState = MutableStateFlow<EventsUiState>(EventsUiState.Loading)
    val uiState: StateFlow<EventsUiState> = mutableState.asStateFlow()

    private val refreshRequests = MutableStateFlow(0)
    private var loadMoreJob: Job? = null

    init {
        viewModelScope.launch {
            combine(queryState.query, refreshRequests) { query, _ -> query }
                .collectLatest { query -> load(query) }
        }
    }

    /** Reloads everything. Keeps the current content on screen while the reload runs. */
    fun refresh() {
        refreshRequests.update { it + 1 }
    }

    fun selectType(filter: EventTypeFilter) {
        mutableState.update { state -> if (state is EventsUiState.Content) state.copy(typeFilter = filter) else state }
    }

    /** Fetches the next older page of the stream. Does nothing when there is no cursor or a page is already loading. */
    fun loadMore() {
        val content = mutableState.value as? EventsUiState.Content ?: return
        val token = PageToken(content.stream.nextBeforeCursor, content.stream.nextBeforeTimestamp)
        if (!token.isPresent || content.stream.isLoadingMore) return
        mutableState.value = content.copy(stream = content.stream.startingLoadMore())
        loadMoreJob = viewModelScope.launch { fetchNextPage(content.query, token) }
    }

    private suspend fun load(query: AnalyticsQuery) {
        loadMoreJob?.cancel()
        val previousFilter = (mutableState.value as? EventsUiState.Content)?.typeFilter ?: EventTypeFilter.All
        mutableState.update { state ->
            if (state is EventsUiState.Content) state.copy(isRefreshing = true) else EventsUiState.Loading
        }
        mutableState.value = try {
            fetchContent(query, previousFilter)
        } catch (unavailable: FeatureUnavailable) {
            EventsUiState.Unavailable
        } catch (error: ApiException) {
            EventsUiState.Error(error)
        }
    }

    private suspend fun fetchContent(query: AnalyticsQuery, typeFilter: EventTypeFilter): EventsUiState.Content = coroutineScope {
        val bucket = query.defaultBucket()
        val points = async { analytics.eventsTimeSeries(siteId, query, bucket) }
        val names = async { analytics.eventNames(siteId, query) }
        val firstPage = async { analytics.events(siteId, query, PAGE_SIZE) }
        EventsUiState.Content(
            query = query,
            bucket = bucket,
            points = points.await(),
            names = names.await(),
            stream = firstPage.await().toStream(),
            typeFilter = typeFilter,
        )
    }

    private suspend fun fetchNextPage(query: AnalyticsQuery, token: PageToken) {
        try {
            val page = analytics.events(siteId, query, PAGE_SIZE, token.cursor, token.timestamp)
            updateStreamIfCurrent(query, token) { stream -> stream.appended(page) }
        } catch (error: ApiException) {
            updateStreamIfCurrent(query, token) { stream -> stream.failedLoadMore(error) }
        }
    }

    /** Applies a page result only if the screen still shows the same query and page token (a reload may have replaced it). */
    private fun updateStreamIfCurrent(query: AnalyticsQuery, token: PageToken, transform: (EventStream) -> EventStream) {
        mutableState.update { state ->
            if (state is EventsUiState.Content && state.query == query && state.stream.pageToken() == token) {
                state.copy(stream = transform(state.stream))
            } else {
                state
            }
        }
    }
}
