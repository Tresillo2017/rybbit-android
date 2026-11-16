package app.rybbitclient.feature.goals

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.rybbitclient.core.data.AnalyticsRepository
import app.rybbitclient.core.model.Funnel
import app.rybbitclient.core.network.ApiException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** UI state for [FunnelsScreen]. */
sealed interface FunnelsUiState {
    data object Loading : FunnelsUiState

    /** An empty [funnels] list is the empty state. [isRefreshing] keeps the list during pull-to-refresh. */
    data class Content(val funnels: List<Funnel>, val isRefreshing: Boolean = false) : FunnelsUiState

    data class Error(val error: ApiException) : FunnelsUiState
}

/** Saved funnels for a site. Funnel definitions have no date range, so the shared query is not used here. */
class FunnelsViewModel(
    private val siteId: String,
    private val analytics: AnalyticsRepository,
) : ViewModel() {

    private val mutableState = MutableStateFlow<FunnelsUiState>(FunnelsUiState.Loading)
    val uiState: StateFlow<FunnelsUiState> = mutableState.asStateFlow()

    init {
        load()
    }

    /** Pull-to-refresh: keeps the current list on screen while it reloads. */
    fun refresh() {
        mutableState.update { current ->
            if (current is FunnelsUiState.Content) current.copy(isRefreshing = true) else FunnelsUiState.Loading
        }
        load()
    }

    private fun load() {
        viewModelScope.launch {
            mutableState.value = try {
                FunnelsUiState.Content(funnels = analytics.funnels(siteId))
            } catch (error: ApiException) {
                FunnelsUiState.Error(error)
            }
        }
    }
}
