package app.rybbitclient.feature.sessions

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import app.rybbitclient.core.network.ApiException
import app.rybbitclient.core.ui.components.EmptyView
import app.rybbitclient.core.ui.components.ErrorView
import app.rybbitclient.core.ui.components.LoadingView
import app.rybbitclient.core.ui.components.UnavailableView
import app.rybbitclient.core.ui.theme.Spacing
import app.rybbitclient.feature.common.toErrorCopy

private const val LOAD_MORE_DESCRIPTION = "Loading more"
private const val LOAD_MORE_ERROR_TITLE = "Couldn't load more"
private const val RETRY_LABEL = "Try again"
private val FOOTER_SPINNER_SIZE = 24.dp

/** Copy for the empty state of a list. */
data class EmptyCopy(val title: String, val message: String)

/**
 * A paginated list with every state designed: pull-to-refresh, loading skeleton, error with retry,
 * unavailable, empty, infinite scroll, and a footer for a next page that is loading or failed.
 *
 * [leading] adds items above the list (for example a profile card). [row] renders one item.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T : Any> PagedListBody(
    state: PagedUiState<T>,
    empty: EmptyCopy,
    itemKey: (T) -> Any,
    onRefresh: () -> Unit,
    onLoadMore: () -> Unit,
    modifier: Modifier = Modifier,
    leading: LazyListScope.() -> Unit = {},
    row: @Composable (T) -> Unit,
) {
    val listState = rememberLazyListState()
    val content = state as? PagedUiState.Content<T>
    listState.OnNearEnd(enabled = content?.canLoadMore() == true, onNearEnd = onLoadMore)
    PullToRefreshBox(
        isRefreshing = content?.isRefreshing == true,
        onRefresh = onRefresh,
        modifier = modifier.fillMaxSize(),
    ) {
        LazyColumn(state = listState, modifier = Modifier.fillMaxSize()) {
            leading()
            pagedItems(state, empty, itemKey, onRefresh, onLoadMore, row)
        }
    }
}

/** Items for a paged list, so a screen can place them inside its own [LazyColumn]. */
fun <T : Any> LazyListScope.pagedItems(
    state: PagedUiState<T>,
    empty: EmptyCopy,
    itemKey: (T) -> Any,
    onRetry: () -> Unit,
    onLoadMore: () -> Unit,
    row: @Composable (T) -> Unit,
) {
    when (state) {
        PagedUiState.Loading -> item(key = "loading") { LoadingView(modifier = Modifier.fillMaxWidth()) }
        is PagedUiState.Error -> item(key = "error") {
            val copy = state.error.toErrorCopy()
            ErrorView(title = copy.title, message = copy.message, modifier = Modifier.fillMaxWidth(), onRetry = onRetry)
        }
        PagedUiState.Unavailable -> item(key = "unavailable") { UnavailableView(modifier = Modifier.fillMaxWidth()) }
        is PagedUiState.Content -> contentItems(state, empty, itemKey, onLoadMore, row)
    }
}

private fun <T : Any> LazyListScope.contentItems(
    state: PagedUiState.Content<T>,
    empty: EmptyCopy,
    itemKey: (T) -> Any,
    onLoadMore: () -> Unit,
    row: @Composable (T) -> Unit,
) {
    if (state.items.isEmpty()) {
        item(key = "empty") { EmptyView(title = empty.title, message = empty.message) }
    }
    items(items = state.items, key = itemKey) { item -> row(item) }
    if (state.isLoadingMore || state.loadMoreError != null) {
        item(key = "footer") { PagingFooter(state.loadMoreError, onLoadMore) }
    }
}

private fun <T : Any> PagedUiState.Content<T>.canLoadMore(): Boolean =
    hasMore && !isLoadingMore && loadMoreError == null

/** Footer shown under a list while the next page loads, or after it failed (with a retry). */
@Composable
internal fun PagingFooter(error: ApiException?, onRetry: () -> Unit) {
    if (error == null) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(Spacing.md).semantics { contentDescription = LOAD_MORE_DESCRIPTION },
            horizontalArrangement = Arrangement.Center,
        ) {
            CircularProgressIndicator(modifier = Modifier.size(FOOTER_SPINNER_SIZE))
        }
        return
    }
    Column(
        modifier = Modifier.fillMaxWidth().padding(Spacing.md),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(text = LOAD_MORE_ERROR_TITLE, style = MaterialTheme.typography.bodyMedium)
        TextButton(onClick = onRetry) { Text(RETRY_LABEL) }
    }
}
