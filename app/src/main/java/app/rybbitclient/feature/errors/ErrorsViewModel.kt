package app.rybbitclient.feature.errors

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.rybbitclient.core.data.AnalyticsRepository
import app.rybbitclient.core.network.ApiException
import app.rybbitclient.core.network.FeatureUnavailable
import app.rybbitclient.core.query.AnalyticsQuery
import app.rybbitclient.core.query.SiteQueryState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private const val ERROR_NAME_LIMIT = AnalyticsRepository.DEFAULT_LIMIT

/**
 * ViewModel for [ErrorsScreen]: distinct error messages in the shared site period, with counts and
 * affected sessions. Reloads when the site query changes or [refresh] is called.
 */
class ErrorsViewModel(
    private val siteId: String,
    private val analytics: AnalyticsRepository,
    private val queryState: SiteQueryState,
) : ViewModel() {

    private val mutableState = MutableStateFlow<ErrorsUiState>(ErrorsUiState.Loading)
    val uiState: StateFlow<ErrorsUiState> = mutableState.asStateFlow()

    private val refreshRequests = MutableStateFlow(0)

    init {
        viewModelScope.launch {
            combine(queryState.query, refreshRequests) { query, _ -> query }
                .collectLatest { query -> load(query) }
        }
    }

    /** Reloads the list. Keeps the current content on screen while the reload runs. */
    fun refresh() {
        refreshRequests.update { it + 1 }
    }

    private suspend fun load(query: AnalyticsQuery) {
        mutableState.update { state ->
            if (state is ErrorsUiState.Content) state.copy(isRefreshing = true) else ErrorsUiState.Loading
        }
        mutableState.value = try {
            ErrorsUiState.Content(
                query = query,
                errors = analytics.errorNames(siteId, query, ERROR_NAME_LIMIT),
            )
        } catch (unavailable: FeatureUnavailable) {
            ErrorsUiState.Unavailable
        } catch (error: ApiException) {
            ErrorsUiState.Error(error)
        }
    }
}
