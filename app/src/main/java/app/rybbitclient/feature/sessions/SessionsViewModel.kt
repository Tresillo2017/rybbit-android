package app.rybbitclient.feature.sessions

import app.rybbitclient.core.data.AnalyticsRepository
import app.rybbitclient.core.model.SessionSummary
import app.rybbitclient.core.query.AnalyticsQuery
import kotlinx.coroutines.flow.StateFlow

/**
 * The Sessions tab: infinite list of sessions for the shared site query. Changing the date range or a filter
 * on the shared [queryState] reloads page 1.
 */
class SessionsViewModel(
    private val siteId: String,
    private val analytics: AnalyticsRepository,
    private val queryState: StateFlow<AnalyticsQuery>,
) : PagedListViewModel<SessionSummary>() {

    init {
        reloadOn(queryState)
    }

    override suspend fun fetchPage(page: Int): PageResult<SessionSummary> {
        val items = analytics.sessions(siteId, queryState.value, page, PAGE_SIZE)
        return PageResult(items = items, hasMore = items.size >= PAGE_SIZE)
    }

    override fun itemKey(item: SessionSummary): Any = item.sessionId

    companion object {
        const val PAGE_SIZE = 50
    }
}
