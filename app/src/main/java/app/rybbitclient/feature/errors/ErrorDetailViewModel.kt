package app.rybbitclient.feature.errors

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.rybbitclient.core.data.AnalyticsRepository
import app.rybbitclient.core.model.ErrorName
import app.rybbitclient.core.network.ApiException
import app.rybbitclient.core.network.FeatureUnavailable
import app.rybbitclient.core.query.AnalyticsQuery
import app.rybbitclient.core.query.SiteQueryState
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private const val OCCURRENCE_LIMIT = AnalyticsRepository.DEFAULT_LIMIT

/**
 * ViewModel for [ErrorDetailScreen]: one error message's totals, its time series and its recent
 * occurrences. Reloads when the shared site query changes or [refresh] is called.
 */
class ErrorDetailViewModel(
    private val siteId: String,
    private val errorMessage: String,
    private val analytics: AnalyticsRepository,
    private val queryState: SiteQueryState,
) : ViewModel() {

    private val mutableState = MutableStateFlow<ErrorDetailUiState>(ErrorDetailUiState.Loading)
    val uiState: StateFlow<ErrorDetailUiState> = mutableState.asStateFlow()

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
            if (state is ErrorDetailUiState.Content) state.copy(isRefreshing = true) else ErrorDetailUiState.Loading
        }
        mutableState.value = try {
            fetchContent(query)
        } catch (unavailable: FeatureUnavailable) {
            ErrorDetailUiState.Unavailable
        } catch (error: ApiException) {
            ErrorDetailUiState.Error(error)
        }
    }

    private suspend fun fetchContent(query: AnalyticsQuery): ErrorDetailUiState.Content = coroutineScope {
        val bucket = query.defaultBucket()
        val points = async { analytics.errorTimeSeries(siteId, query, bucket, errorMessage) }
        val occurrences = async { analytics.errorEvents(siteId, query, errorMessage, OCCURRENCE_LIMIT) }
        val names = async { analytics.errorNames(siteId, query, OCCURRENCE_LIMIT) }
        ErrorDetailUiState.Content(
            query = query,
            summary = names.await().firstOrNull { it.value == errorMessage }?.toSummary(),
            bucket = bucket,
            points = points.await(),
            occurrences = occurrences.await(),
        )
    }
}

private fun ErrorName.toSummary(): ErrorSummary = ErrorSummary(
    errorName = errorName,
    count = count ?: 0,
    sessionCount = sessionCount ?: 0,
    percentage = percentage,
)
