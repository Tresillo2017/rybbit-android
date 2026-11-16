package app.rybbitclient.feature.sites

import app.rybbitclient.core.model.OrganizationSites
import app.rybbitclient.core.model.Site

private const val UNNAMED_ORGANIZATION = "Organization"

/**
 * Turns organization/site pairs into list sections. Organizations with no sites are dropped, so the list
 * never shows an empty header. A site without a name falls back to its domain, then to its id.
 */
fun buildGroups(entries: List<OrganizationSites>): List<OrganizationGroup> =
    entries.mapNotNull { entry ->
        val sites = entry.sites.map { it.toSiteItem() }
        if (sites.isEmpty()) return@mapNotNull null
        OrganizationGroup(
            id = entry.organization?.id.orEmpty(),
            name = entry.organization?.name?.takeIf { it.isNotBlank() } ?: UNNAMED_ORGANIZATION,
            sites = sites,
        )
    }

/** Case-insensitive match on name or domain. Groups left with no matching sites are removed. */
fun filterOrganizations(groups: List<OrganizationGroup>, query: String): List<OrganizationGroup> {
    val needle = query.trim()
    if (needle.isEmpty()) return groups
    return groups.mapNotNull { group ->
        val matches = group.sites.filter { it.matches(needle) }
        if (matches.isEmpty()) null else group.copy(sites = matches)
    }
}

private fun Site.toSiteItem(): SiteItem = SiteItem(
    analyticsId = analyticsId,
    name = name?.takeIf { it.isNotBlank() } ?: domain?.takeIf { it.isNotBlank() } ?: analyticsId,
    domain = domain?.takeIf { it.isNotBlank() },
)

private fun SiteItem.matches(needle: String): Boolean =
    name.contains(needle, ignoreCase = true) || domain?.contains(needle, ignoreCase = true) == true
