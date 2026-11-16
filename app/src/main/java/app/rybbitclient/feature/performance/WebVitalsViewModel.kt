package app.rybbitclient.feature.performance

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.rybbitclient.core.data.AnalyticsRepository
import app.rybbitclient.core.network.ApiException
import app.rybbitclient.core.network.FeatureUnavailable
import app.rybbitclient.core.query.AnalyticsQuery
import app.rybbitclient.core.query.SiteQueryState
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** UI state for [WebVitalsScreen]. */
sealed interface WebVitalsUiState {
    data object Loading : WebVitalsUiState

    /** [sampleCount] is the number of page loads with performance data, when the server reports it. */
    data class Content(
        val readings: List<VitalReading>,
        val sampleCount: Int?,
        val isRefreshing: Boolean = false,
    ) : WebVitalsUiState

    /** Every percentile is missing, or the server reports the feature as absent. Not an error. */
    data object Unavailable : WebVitalsUiState

    data class Error(val error: ApiException) : WebVitalsUiState
}

/** Web Vitals percentiles for the site under the shared date range and filters. */
class WebVitalsViewModel(
    private val siteId: String,
    private val analytics: AnalyticsRepository,
    private val siteQuery: SiteQueryState,
) : ViewModel() {

    private val mutableState = MutableStateFlow<WebVitalsUiState>(WebVitalsUiState.Loading)
    val uiState: StateFlow<WebVitalsUiState> = mutableState.asStateFlow()

    private var query: AnalyticsQuery = siteQuery.query.value
    private var loadJob: Job? = null

    init {
        viewModelScope.launch {
            siteQuery.query.collect { latest ->
                query = latest
                reload(keepContent = false)
            }
        }
    }

    /** Pull-to-refresh: reloads while keeping the current readings on screen. */
    fun refresh() = reload(keepContent = true)

    private fun reload(keepContent: Boolean) {
        loadJob?.cancel()
        mutableState.update { current ->
            if (keepContent && current is WebVitalsUiState.Content) current.copy(isRefreshing = true) else WebVitalsUiState.Loading
        }
        loadJob = viewModelScope.launch { mutableState.value = fetch() }
    }

    private suspend fun fetch(): WebVitalsUiState = try {
        val overview = analytics.performanceOverview(siteId, query)
        if (hasNoVitalValues(overview)) {
            WebVitalsUiState.Unavailable
        } else {
            WebVitalsUiState.Content(
                readings = vitalReadings(overview),
                sampleCount = overview.totalPerformanceEvents,
            )
        }
    } catch (error: FeatureUnavailable) {
        WebVitalsUiState.Unavailable
    } catch (error: ApiException) {
        WebVitalsUiState.Error(error)
    }
}
