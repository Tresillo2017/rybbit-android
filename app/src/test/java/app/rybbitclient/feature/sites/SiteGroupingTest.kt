package app.rybbitclient.feature.sites

import app.rybbitclient.core.model.OrganizationSites
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Pure grouping and search rules used by the Sites list. */
class SiteGroupingTest {

    @Test
    fun `site without a name falls back to its domain, then to its id`() {
        // Arrange
        val entries = listOf(
            OrganizationSites(
                organization = testOrganization("org", "Acme"),
                sites = listOf(
                    testSite(1, name = "", domain = "shop.example.com"),
                    testSite(2, name = "", domain = null),
                ),
            ),
        )

        // Act
        val names = buildGroups(entries).single().sites.map { it.name }

        // Assert
        assertEquals(listOf("shop.example.com", "2"), names)
    }

    @Test
    fun `blank organization name becomes a generic label`() {
        // Arrange
        val entries = listOf(
            OrganizationSites(
                organization = testOrganization("org", " "),
                sites = listOf(testSite(1, "One")),
            ),
        )

        // Act
        val group = buildGroups(entries).single()

        // Assert
        assertEquals("Organization", group.name)
    }

    @Test
    fun `filter matches the domain case-insensitively and drops groups with no match`() {
        // Arrange
        val groups = buildGroups(
            listOf(
                OrganizationSites(testOrganization("a", "Acme"), listOf(testSite(1, "Shop"), testSite(2, "Blog"))),
                OrganizationSites(testOrganization("b", "Beta"), listOf(testSite(3, "Docs"))),
            ),
        )

        // Act
        val filtered = filterOrganizations(groups, query = "  SHOP.EXAMPLE  ")

        // Assert
        assertEquals(listOf("Acme"), filtered.map { it.name })
        assertEquals(listOf("Shop"), filtered.single().sites.map { it.name })
    }

    @Test
    fun `blank query returns every group unchanged`() {
        // Arrange
        val groups = buildGroups(
            listOf(OrganizationSites(testOrganization("a", "Acme"), listOf(testSite(1, "Shop")))),
        )

        // Act
        val result = filterOrganizations(groups, query = "   ")

        // Assert
        assertTrue(result == groups)
    }
}
