package app.rybbitclient.feature.users

import androidx.lifecycle.viewModelScope
import app.rybbitclient.core.data.AnalyticsRepository
import app.rybbitclient.core.model.UserSummary
import app.rybbitclient.core.model.UsersResponse
import app.rybbitclient.core.query.AnalyticsQuery
import app.rybbitclient.feature.sessions.PageResult
import app.rybbitclient.feature.sessions.PagedListViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.launch

/**
 * The Users tab: infinite list of users with a search box and an identified-only toggle. Typing is debounced
 * before it reloads, so each keystroke does not hit the server. Date range and filters come from [queryState].
 */
@OptIn(FlowPreview::class)
class UsersViewModel(
    private val siteId: String,
    private val analytics: AnalyticsRepository,
    private val queryState: StateFlow<AnalyticsQuery>,
    searchDebounceMillis: Long = SEARCH_DEBOUNCE_MILLIS,
) : PagedListViewModel<UserSummary>() {

    private val mutableSearchText = MutableStateFlow("")

    /** The text in the search box, updated on every keystroke. */
    val searchText: StateFlow<String> = mutableSearchText.asStateFlow()

    /** The search term sent to the server (trimmed, debounced). */
    private val appliedSearch = MutableStateFlow("")

    private val mutableIdentifiedOnly = MutableStateFlow(false)
    val identifiedOnly: StateFlow<Boolean> = mutableIdentifiedOnly.asStateFlow()

    init {
        viewModelScope.launch {
            mutableSearchText.debounce(searchDebounceMillis).collect { appliedSearch.value = it.trim() }
        }
        reloadOn(combine(queryState, appliedSearch, mutableIdentifiedOnly) { _, _, _ -> Unit })
    }

    fun onSearchTextChanged(text: String) {
        mutableSearchText.value = text
    }

    fun setIdentifiedOnly(enabled: Boolean) {
        mutableIdentifiedOnly.value = enabled
    }

    override suspend fun fetchPage(page: Int): PageResult<UserSummary> {
        val response = analytics.usersFiltered(
            siteId = siteId,
            query = queryState.value,
            page = page,
            pageSize = PAGE_SIZE,
            identifiedOnly = mutableIdentifiedOnly.value,
            search = appliedSearch.value.takeIf(String::isNotEmpty),
        )
        return PageResult(items = response.data, hasMore = hasMoreUsers(page, response))
    }

    override fun itemKey(item: UserSummary): Any = item.userId

    companion object {
        const val PAGE_SIZE = 50
        const val SEARCH_DEBOUNCE_MILLIS = 300L

        /**
         * True when a later page exists. Uses `totalCount` when the server sends it, otherwise a full page
         * suggests more. An empty page never has more.
         */
        fun hasMoreUsers(page: Int, response: UsersResponse): Boolean {
            if (response.data.isEmpty()) return false
            val total = response.totalCount ?: return response.data.size >= PAGE_SIZE
            return page.toLong() * PAGE_SIZE < total
        }
    }
}
