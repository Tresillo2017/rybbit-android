package app.rybbitclient.core.query

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * The date range and filters shared by every tab of the site being viewed, so picking "Last 30 days"
 * or filtering by a country on Overview also applies to Sessions, Events, Users, Goals, etc.
 *
 * Held as a singleton in [app.rybbitclient.di.AppContainer]. Each transition produces a new
 * [AnalyticsQuery]; nothing is mutated in place. Filters reset when the site changes.
 */
class SiteQueryState(initial: AnalyticsQuery = AnalyticsQuery(range = DateRange.Last7Days)) {

    private val state = MutableStateFlow(initial)
    private var currentSiteId: String? = null

    val query: StateFlow<AnalyticsQuery> = state.asStateFlow()

    /** Call when a site is opened. Switching to a different site clears its filters (range is kept). */
    fun onSiteOpened(siteId: String) {
        if (currentSiteId != null && currentSiteId != siteId) {
            state.update { it.copy(filters = emptyList()) }
        }
        currentSiteId = siteId
    }

    fun setRange(range: DateRange) = state.update { it.copy(range = range) }

    /** Adds a filter, replacing any existing filter on the same parameter. */
    fun addFilter(filter: Filter) = state.update { query ->
        query.copy(filters = query.filters.filterNot { it.parameter == filter.parameter } + filter)
    }

    fun removeFilter(filter: Filter) = state.update { query -> query.copy(filters = query.filters - filter) }

    fun clearFilters() = state.update { it.copy(filters = emptyList()) }
}
