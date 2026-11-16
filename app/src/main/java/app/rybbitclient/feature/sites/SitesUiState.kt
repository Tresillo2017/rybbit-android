package app.rybbitclient.feature.sites

import app.rybbitclient.core.model.Instance
import app.rybbitclient.core.network.ApiException

/** Search appears once the account has more sites than this. */
internal const val SEARCH_THRESHOLD = 6

/** A site as the list shows it. [analyticsId] is the id used in analytics URLs. */
data class SiteItem(
    val analyticsId: String,
    val name: String,
    val domain: String?,
)

/** One section of the list: an organization and the sites it contains. Organizations without sites are dropped. */
data class OrganizationGroup(
    val id: String,
    val name: String,
    val sites: List<SiteItem>,
)

/** What the top-bar instance switcher shows. */
data class InstanceSwitcherState(
    val instances: List<Instance> = emptyList(),
    val activeId: String? = null,
) {
    val active: Instance? get() = instances.firstOrNull { it.id == activeId }
}

/**
 * UI state for [SitesScreen]. [Content.liveCounts] fills in after the list is on screen, one site at a time;
 * a site whose count failed simply has no entry. [Content.query] filters the list without a network call.
 */
sealed interface SitesUiState {

    data object Loading : SitesUiState

    data class Content(
        val organizations: List<OrganizationGroup>,
        val liveCounts: Map<String, Int> = emptyMap(),
        val query: String = "",
        val isRefreshing: Boolean = false,
    ) : SitesUiState {

        val totalSiteCount: Int get() = organizations.sumOf { it.sites.size }

        val showSearch: Boolean get() = totalSiteCount > SEARCH_THRESHOLD

        val visibleOrganizations: List<OrganizationGroup>
            get() = filterOrganizations(organizations, query)
    }

    data class Error(val error: ApiException) : SitesUiState
}
