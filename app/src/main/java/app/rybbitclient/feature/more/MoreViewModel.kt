package app.rybbitclient.feature.more

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.rybbitclient.core.data.AnalyticsRepository
import app.rybbitclient.core.data.Feature
import app.rybbitclient.core.model.InstanceKind
import app.rybbitclient.core.network.ApiException
import app.rybbitclient.core.storage.InstanceRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * ViewModel for [MoreScreen]. Reference implementation of the UiState pattern:
 * a private MutableStateFlow, a read-only StateFlow exposed to the UI, and loads launched in viewModelScope.
 * Only [ApiException] is caught; anything else is a bug and should crash in debug builds.
 */
class MoreViewModel(
    private val siteId: String,
    private val analytics: AnalyticsRepository,
    private val instances: InstanceRepository,
) : ViewModel() {

    private val mutableState = MutableStateFlow<MoreUiState>(MoreUiState.Loading)
    val uiState: StateFlow<MoreUiState> = mutableState.asStateFlow()
    private var loadJob: Job? = null

    init {
        load()
    }

    /** Reloads. Keeps the current content on screen while the refresh runs. */
    fun refresh() {
        mutableState.update { current ->
            if (current is MoreUiState.Content) current.copy(isRefreshing = true) else MoreUiState.Loading
        }
        load()
    }

    private fun load() {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            mutableState.value = try {
                loadContent()
            } catch (error: ApiException) {
                MoreUiState.Error(error)
            }
        }
    }

    private suspend fun loadContent(): MoreUiState.Content {
        val funnelCount = analytics.funnels(siteId).size
        // Web Vitals is cloud-only in practice: self-hosted servers answer with empty percentiles, not a 404.
        val isCloud = instances.activeInstance.first()?.kind == InstanceKind.Cloud
        val webVitalsAvailable = isCloud && analytics.isFeatureAvailable(Feature.Performance)
        return MoreUiState.Content(funnelCount = funnelCount, webVitalsAvailable = webVitalsAvailable)
    }
}
