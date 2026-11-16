package app.rybbitclient.feature.sessions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.rybbitclient.core.network.ApiException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Shared paging state machine for infinite lists. Subclasses supply [fetchPage] (1-indexed) and a stable
 * [itemKey]. Page 1 replaces the list; [loadMore] appends the next page. Reloads cancel any request in flight,
 * so a stale page never lands on top of a newer query.
 *
 * Call [reloadOn] at the end of the subclass init, after its own fields are set.
 */
abstract class PagedListViewModel<T : Any> : ViewModel() {

    private val mutableState = MutableStateFlow<PagedUiState<T>>(PagedUiState.Loading)
    val uiState: StateFlow<PagedUiState<T>> = mutableState.asStateFlow()

    private var loadedPages = 0
    private var requestJob: Job? = null

    /** Fetches [page] (1-indexed) using the current filters. */
    protected abstract suspend fun fetchPage(page: Int): PageResult<T>

    /** Identity of an item, used to drop duplicates when a shifted list overlaps two pages. */
    protected abstract fun itemKey(item: T): Any

    /** Reloads the first page every time [trigger] emits (a StateFlow emits its current value immediately). */
    protected fun reloadOn(trigger: Flow<*>) {
        viewModelScope.launch { trigger.collect { reload() } }
    }

    /** Pull-to-refresh and retry. Keeps the current items on screen while the first page reloads. */
    fun refresh() = reload()

    /** Appends the next page. No-op while a page is loading, while refreshing, or when the list is complete. */
    fun loadMore() {
        val current = contentOrNull() ?: return
        if (!current.hasMore || current.isLoadingMore || current.isRefreshing) return
        val page = loadedPages + 1
        mutableState.value = current.copy(isLoadingMore = true, loadMoreError = null)
        requestJob = viewModelScope.launch {
            try {
                val next = fetchPage(page)
                loadedPages = page
                mutableState.update { latest -> contentOrNull(latest)?.appended(next) ?: latest }
            } catch (error: ApiException) {
                mutableState.update { latest ->
                    contentOrNull(latest)?.copy(isLoadingMore = false, loadMoreError = error) ?: latest
                }
            }
        }
    }

    protected fun reload() {
        requestJob?.cancel()
        mutableState.update { current ->
            contentOrNull(current)?.copy(isRefreshing = true, isLoadingMore = false, loadMoreError = null)
                ?: PagedUiState.Loading
        }
        requestJob = viewModelScope.launch {
            mutableState.value = try {
                val first = fetchPage(FIRST_PAGE)
                loadedPages = FIRST_PAGE
                PagedUiState.Content(
                    items = first.items.distinctBy { itemKey(it) },
                    hasMore = first.hasMore,
                )
            } catch (error: ApiException) {
                error.toPagedState()
            }
        }
    }

    private fun PagedUiState.Content<T>.appended(next: PageResult<T>): PagedUiState.Content<T> = copy(
        items = (items + next.items).distinctBy { itemKey(it) },
        hasMore = next.hasMore,
        isLoadingMore = false,
        loadMoreError = null,
    )

    private fun contentOrNull(): PagedUiState.Content<T>? = contentOrNull(mutableState.value)

    @Suppress("UNCHECKED_CAST")
    private fun contentOrNull(state: PagedUiState<T>): PagedUiState.Content<T>? =
        state as? PagedUiState.Content<T>

    private companion object {
        const val FIRST_PAGE = 1
    }
}
