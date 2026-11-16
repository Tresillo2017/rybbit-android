package app.rybbitclient.feature.sessions

import app.rybbitclient.core.network.ApiException
import app.rybbitclient.core.network.FeatureUnavailable

/** One loaded page of a paginated list: its items and whether another page follows. */
data class PageResult<out T>(val items: List<T>, val hasMore: Boolean)

/**
 * UI state of a paginated list (sessions, users). Empty results are [Content] with no items.
 * A failure on page 2 or later keeps the loaded items in [Content.loadMoreError]; only a failure
 * of the first page replaces the list with [Error].
 */
sealed interface PagedUiState<out T> {

    /** First page in flight. */
    data object Loading : PagedUiState<Nothing>

    data class Content<out T>(
        val items: List<T>,
        val hasMore: Boolean,
        val isRefreshing: Boolean = false,
        val isLoadingMore: Boolean = false,
        val loadMoreError: ApiException? = null,
    ) : PagedUiState<T>

    data class Error(val error: ApiException) : PagedUiState<Nothing>

    /** The endpoint does not exist on this server. Not an error. */
    data object Unavailable : PagedUiState<Nothing>
}

/** Maps a failed first-page request to its state: 404 `API_ROUTE_NOT_FOUND` is "unavailable", the rest errors. */
fun ApiException.toPagedState(): PagedUiState<Nothing> = when (this) {
    is FeatureUnavailable -> PagedUiState.Unavailable
    else -> PagedUiState.Error(this)
}
