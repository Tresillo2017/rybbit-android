package app.rybbitclient.feature.goals

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.rybbitclient.core.data.AnalyticsRepository
import app.rybbitclient.core.network.ApiException
import app.rybbitclient.core.network.NotFound
import app.rybbitclient.core.query.AnalyticsQuery
import app.rybbitclient.core.query.SiteQueryState
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** UI state for [FunnelDetailScreen]. */
sealed interface FunnelDetailUiState {
    data object Loading : FunnelDetailUiState

    /** [analysis] with empty steps means the funnel has no steps to analyze. */
    data class Content(
        val funnelName: String,
        val analysis: FunnelAnalysis,
        val isRefreshing: Boolean = false,
    ) : FunnelDetailUiState

    data class Error(val error: ApiException) : FunnelDetailUiState
}

/**
 * Analyzes one saved funnel over the shared date range and filters. The funnel definition is fetched
 * with the analysis each time, so a funnel edited on the web shows up on the next refresh.
 */
class FunnelDetailViewModel(
    private val siteId: String,
    private val funnelId: Int,
    private val analytics: AnalyticsRepository,
    private val siteQuery: SiteQueryState,
) : ViewModel() {

    private val mutableState = MutableStateFlow<FunnelDetailUiState>(FunnelDetailUiState.Loading)
    val uiState: StateFlow<FunnelDetailUiState> = mutableState.asStateFlow()

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

    /** Pull-to-refresh: reloads while keeping the current analysis on screen. */
    fun refresh() = reload(keepContent = true)

    private fun reload(keepContent: Boolean) {
        loadJob?.cancel()
        mutableState.update { current ->
            if (keepContent && current is FunnelDetailUiState.Content) current.copy(isRefreshing = true) else FunnelDetailUiState.Loading
        }
        loadJob = viewModelScope.launch {
            mutableState.value = try {
                loadAnalysis()
            } catch (error: ApiException) {
                FunnelDetailUiState.Error(error)
            }
        }
    }

    private suspend fun loadAnalysis(): FunnelDetailUiState.Content {
        val funnel = analytics.funnels(siteId).firstOrNull { it.id == funnelId }
            ?: throw NotFound(code = null, serverMessage = null, resolution = null)
        val results = if (funnel.steps.isEmpty()) {
            emptyList()
        } else {
            analytics.analyzeFunnel(siteId, query, funnel.steps)
        }
        return FunnelDetailUiState.Content(funnelName = funnel.name, analysis = analyzeFunnelResults(results))
    }
}
