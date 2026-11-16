package app.rybbitclient.feature.retention

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.rybbitclient.core.data.AnalyticsRepository
import app.rybbitclient.core.network.ApiException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Cohort retention for one site. The retention endpoint takes its own trailing window and mode rather
 * than the shared date range, so this screen keeps its own mode and range controls. Each change reloads.
 */
class RetentionViewModel(
    private val siteId: String,
    private val analytics: AnalyticsRepository,
) : ViewModel() {

    private var mode: RetentionMode = RetentionMode.Day
    private var range: Int = DEFAULT_RETENTION_WINDOW_DAYS
    private var loadJob: Job? = null

    private val mutableState = MutableStateFlow<RetentionUiState>(RetentionUiState.Loading(mode, range))
    val uiState: StateFlow<RetentionUiState> = mutableState.asStateFlow()

    init {
        reload(keepContent = false)
    }

    fun selectMode(next: RetentionMode) {
        if (next == mode) return
        mode = next
        range = clampRangeFor(next, range)
        reload(keepContent = false)
    }

    fun selectRange(days: Int) {
        if (days == range) return
        range = days
        reload(keepContent = false)
    }

    /** Pull-to-refresh: reloads the current mode and range, keeping the grid on screen meanwhile. */
    fun refresh() = reload(keepContent = true)

    private fun reload(keepContent: Boolean) {
        loadJob?.cancel()
        val current = mutableState.value
        mutableState.value = if (keepContent && current is RetentionUiState.Content && current.mode == mode && current.range == range) {
            current.copy(isRefreshing = true)
        } else {
            RetentionUiState.Loading(mode, range)
        }
        val requestedMode = mode
        val requestedRange = range
        loadJob = viewModelScope.launch {
            mutableState.value = fetch(requestedMode, requestedRange)
        }
    }

    private suspend fun fetch(requestedMode: RetentionMode, requestedRange: Int): RetentionUiState = try {
        val data = analytics.retention(siteId, requestedMode.apiValue, requestedRange)
        RetentionUiState.Content(mode = requestedMode, range = requestedRange, cohorts = cohortRows(data))
    } catch (error: ApiException) {
        RetentionUiState.Error(mode = requestedMode, range = requestedRange, error = error)
    }
}
