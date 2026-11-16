package app.rybbitclient.feature.more

import app.rybbitclient.core.network.ApiException

/**
 * UI state for [MoreScreen]. Immutable: every change produces a new value with `copy` or a new instance.
 * This is the pattern feature agents follow: Loading -> Content (optionally refreshing) or Error.
 */
sealed interface MoreUiState {

    /** First load, before anything is known. */
    data object Loading : MoreUiState

    /**
     * Loaded. [isRefreshing] keeps the list on screen during a pull-to-refresh.
     * [webVitalsAvailable] is false on self-hosted servers, where the performance report is empty.
     */
    data class Content(
        val funnelCount: Int,
        val webVitalsAvailable: Boolean,
        val isRefreshing: Boolean = false,
    ) : MoreUiState

    /** The load failed. The screen maps [error] to friendly copy with `toErrorCopy()`. */
    data class Error(val error: ApiException) : MoreUiState
}
