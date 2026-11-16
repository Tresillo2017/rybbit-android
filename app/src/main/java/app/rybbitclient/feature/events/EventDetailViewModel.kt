package app.rybbitclient.feature.events

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.rybbitclient.core.data.AnalyticsRepository
import app.rybbitclient.core.network.ApiException
import app.rybbitclient.core.network.FeatureUnavailable
import app.rybbitclient.core.query.AnalyticsQuery
import app.rybbitclient.core.query.SiteQueryState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private const val OCCURRENCE_PAGE_SIZE = AnalyticsRepository.DEFAULT_LIMIT

/**
 * ViewModel for [EventDetailScreen]: one event's count, its time series, property breakdown and recent
 * occurrences. Reloads when the shared site query changes or [refresh] is called.
 */
class EventDetailViewModel(
    private val siteId: String,
    private val eventName: String,
    private val analytics: AnalyticsRepository,
    private val queryState: SiteQueryState,
) : ViewModel() {

    private val mutableState = MutableStateFlow<EventDetailUiState>(EventDetailUiState.Loading)
    val uiState: StateFlow<EventDetailUiState> = mutableState.asStateFlow()

    private val refreshRequests = MutableStateFlow(0)

    init {
        viewModelScope.launch {
            combine(queryState.query, refreshRequests) { query, _ -> query }
                .collectLatest { query -> load(query) }
        }
    }

    /** Reloads every section. Keeps the current content on screen while the reload runs. */
    fun refresh() {
        refreshRequests.update { it + 1 }
    }

    private suspend fun load(query: AnalyticsQuery) {
        mutableState.update { state ->
            if (state is EventDetailUiState.Content) state.copy(isRefreshing = true) else EventDetailUiState.Loading
        }
        mutableState.value = try {
            fetchContent(query)
        } catch (unavailable: FeatureUnavailable) {
            EventDetailUiState.Unavailable
        } catch (error: ApiException) {
            EventDetailUiState.Error(error)
        }
    }

    /**
     * Runs one secondary section of the screen. A failure only empties that section, so a single rejected
     * sub-request does not replace the whole event detail with an error. Cancellation still propagates.
     */
    private suspend fun <T> optionalSection(load: suspend () -> T): T? = try {
        load()
    } catch (cancellation: CancellationException) {
        throw cancellation
    } catch (failure: ApiException) {
        null
    }

    private suspend fun fetchContent(query: AnalyticsQuery): EventDetailUiState.Content = coroutineScope {
        val bucket = query.defaultBucket()
        // Narrowing to sessions that contain the event keeps its own counts exact and makes it more likely to be in the top series.
        val points = async { analytics.eventsTimeSeries(siteId, withEventNameFilter(query, eventName), bucket) }
        val names = async { analytics.eventNames(siteId, query) }
        val properties = async { optionalSection { analytics.eventProperties(siteId, query, eventName) } }
        val occurrences = async {
            analytics.events(siteId, withEventNameFilter(query, eventName), OCCURRENCE_PAGE_SIZE)
        }
        EventDetailUiState.Content(
            query = query,
            totalCount = names.await().firstOrNull { it.eventName == eventName }?.count,
            bucket = bucket,
            points = points.await().filter { it.eventName == eventName },
            propertyGroups = groupEventProperties(properties.await().orEmpty()),
            occurrences = occurrencesOf(occurrences.await().data, eventName),
        )
    }
}
