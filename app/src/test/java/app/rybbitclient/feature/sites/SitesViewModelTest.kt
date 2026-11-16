package app.rybbitclient.feature.sites

import app.rybbitclient.core.model.AuthType
import app.rybbitclient.core.model.Instance
import app.rybbitclient.core.model.InstanceKind
import app.rybbitclient.core.model.Organization
import app.rybbitclient.core.network.Credential
import app.rybbitclient.core.network.Server
import app.rybbitclient.core.network.Unauthorized
import app.rybbitclient.core.storage.InMemoryKeyValueStore
import app.rybbitclient.core.storage.InMemorySecretStore
import app.rybbitclient.core.storage.InstanceRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/** ViewModel tests for the Sites list: grouping, live counts, search, auto-open and instance switching. */
@OptIn(ExperimentalCoroutinesApi::class)
class SitesViewModelTest {

    private val dispatcher = UnconfinedTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `groups sites under their organizations in order`() = runTest(dispatcher) {
        // Arrange
        val analytics = FakeSitesAnalytics(
            organizationsResult = {
                listOf(
                    testOrganization("org-a", "Acme", listOf(testSite(1, "Shop"), testSite(2, "Blog"))),
                    testOrganization("org-b", "Beta", listOf(testSite(3, "Docs"))),
                )
            },
        )
        val repository = repositoryWith(instance("one"))

        // Act
        val viewModel = SitesViewModel(analytics, repository, AutoOpenGate())

        // Assert
        val content = viewModel.uiState.value as SitesUiState.Content
        assertEquals(listOf("Acme", "Beta"), content.organizations.map { it.name })
        assertEquals(listOf("Shop", "Blog"), content.organizations[0].sites.map { it.name })
        assertEquals(listOf("3"), content.organizations[1].sites.map { it.analyticsId })
        assertEquals(3, content.totalSiteCount)
    }

    @Test
    fun `asks for an organization's sites when its embedded list is empty`() = runTest(dispatcher) {
        // Arrange
        val analytics = FakeSitesAnalytics(
            organizationsResult = { listOf(testOrganization("org-b", "Beta")) },
            sitesResult = { organizationId -> if (organizationId == "org-b") listOf(testSite(9, "Late")) else emptyList() },
        )

        // Act
        val viewModel = SitesViewModel(analytics, repositoryWith(instance("one")), AutoOpenGate())

        // Assert
        val content = viewModel.uiState.value as SitesUiState.Content
        assertEquals(listOf("Late"), content.organizations.single().sites.map { it.name })
    }

    @Test
    fun `organizations without sites are not shown`() = runTest(dispatcher) {
        // Arrange
        val analytics = FakeSitesAnalytics(
            organizationsResult = {
                listOf(
                    testOrganization("org-empty", "Empty"),
                    testOrganization("org-a", "Acme", listOf(testSite(1, "Shop"))),
                )
            },
        )

        // Act
        val viewModel = SitesViewModel(analytics, repositoryWith(instance("one")), AutoOpenGate())

        // Assert
        val content = viewModel.uiState.value as SitesUiState.Content
        assertEquals(listOf("Acme"), content.organizations.map { it.name })
    }

    @Test
    fun `live counts arrive per site and a failing site is left out without an error`() = runTest(dispatcher) {
        // Arrange
        val analytics = FakeSitesAnalytics(
            organizationsResult = {
                listOf(testOrganization("org-a", "Acme", listOf(testSite(1, "One"), testSite(2, "Two"), testSite(3, "Three"))))
            },
            liveCountResult = { siteId ->
                if (siteId == "2") throw Server(status = 500, code = null, serverMessage = null, resolution = null)
                siteId.toInt() * 10
            },
        )

        // Act
        val viewModel = SitesViewModel(analytics, repositoryWith(instance("one")), AutoOpenGate())

        // Assert
        val content = viewModel.uiState.value as SitesUiState.Content
        assertEquals(mapOf("1" to 10, "3" to 30), content.liveCounts)
        assertEquals(listOf("1", "2", "3"), analytics.liveCountRequests.sorted())
    }

    @Test
    fun `search filters by name or domain and shows a no-match state`() = runTest(dispatcher) {
        // Arrange
        val analytics = FakeSitesAnalytics(
            organizationsResult = { listOf(testOrganization("org-a", "Acme", sevenSites())) },
        )
        val viewModel = SitesViewModel(analytics, repositoryWith(instance("one")), AutoOpenGate())
        assertTrue((viewModel.uiState.value as SitesUiState.Content).showSearch)

        // Act
        viewModel.onQueryChanged("BLOG")
        val matched = viewModel.uiState.value as SitesUiState.Content
        viewModel.onQueryChanged("nothing-here")
        val unmatched = viewModel.uiState.value as SitesUiState.Content

        // Assert
        assertEquals(listOf("Blog"), matched.visibleOrganizations.single().sites.map { it.name })
        assertTrue(unmatched.visibleOrganizations.isEmpty())
        assertEquals(7, unmatched.totalSiteCount)
        assertEquals(1, analytics.organizationsCalls)
    }

    @Test
    fun `search is hidden for six sites or fewer`() = runTest(dispatcher) {
        // Arrange
        val sixSites = (1L..6L).map { testSite(it, "Site $it") }
        val analytics = FakeSitesAnalytics(organizationsResult = { listOf(testOrganization("org-a", "Acme", sixSites)) })

        // Act
        val viewModel = SitesViewModel(analytics, repositoryWith(instance("one")), AutoOpenGate())

        // Assert
        assertFalse((viewModel.uiState.value as SitesUiState.Content).showSearch)
    }

    @Test
    fun `no sites at all is an empty content state, not an error`() = runTest(dispatcher) {
        // Arrange
        val analytics = FakeSitesAnalytics(organizationsResult = { listOf(Organization(id = "org-a", name = "Acme")) })

        // Act
        val viewModel = SitesViewModel(analytics, repositoryWith(instance("one")), AutoOpenGate())

        // Assert
        val content = viewModel.uiState.value as SitesUiState.Content
        assertTrue(content.organizations.isEmpty())
        assertEquals(0, content.totalSiteCount)
    }

    @Test
    fun `a rejected credential becomes an error state`() = runTest(dispatcher) {
        // Arrange
        val analytics = FakeSitesAnalytics(
            organizationsResult = { throw Unauthorized(code = null, serverMessage = null, resolution = null) },
        )

        // Act
        val viewModel = SitesViewModel(analytics, repositoryWith(instance("one")), AutoOpenGate())

        // Assert
        assertTrue(viewModel.uiState.value is SitesUiState.Error)
    }

    @Test
    fun `remembered site is offered once per gate`() = runTest(dispatcher) {
        // Arrange
        val repository = repositoryWith(instance("one"))
        repository.setLastSelectedSiteId("one", "2")
        val analytics = FakeSitesAnalytics(
            organizationsResult = { listOf(testOrganization("org-a", "Acme", listOf(testSite(1, "One"), testSite(2, "Two")))) },
        )
        val gate = AutoOpenGate()
        val offered = mutableListOf<SiteItem>()

        // Act
        val first = SitesViewModel(analytics, repository, gate)
        val collector = launch { first.autoOpenSite.collect { offered += it } }
        val second = SitesViewModel(analytics, repository, gate)
        val secondOffers = mutableListOf<SiteItem>()
        val secondCollector = launch { second.autoOpenSite.collect { secondOffers += it } }

        // Assert
        assertEquals(listOf("2"), offered.map { it.analyticsId })
        assertTrue(secondOffers.isEmpty())
        collector.cancel()
        secondCollector.cancel()
    }

    @Test
    fun `tapping a site remembers it on the active instance`() = runTest(dispatcher) {
        // Arrange
        val repository = repositoryWith(instance("one"))
        val analytics = FakeSitesAnalytics(
            organizationsResult = { listOf(testOrganization("org-a", "Acme", listOf(testSite(4, "Four")))) },
        )
        val viewModel = SitesViewModel(analytics, repository, AutoOpenGate())

        // Act
        viewModel.onSiteTapped(viewModel.uiState.value.let { (it as SitesUiState.Content).organizations.single().sites.single() })

        // Assert
        assertEquals("4", repository.lastSelectedSiteId("one").first())
    }

    @Test
    fun `selecting another instance reloads the list from it`() = runTest(dispatcher) {
        // Arrange
        val repository = repositoryWith(instance("one"), instance("two"))
        val analytics = FakeSitesAnalytics(organizationsResult = { listOf(testOrganization("org-a", "Acme", listOf(testSite(1, "One")))) })
        val viewModel = SitesViewModel(analytics, repository, AutoOpenGate())
        val callsBefore = analytics.organizationsCalls

        // Act
        viewModel.selectInstance("two")

        // Assert
        assertEquals("two", viewModel.switcher.value.activeId)
        assertEquals(callsBefore + 1, analytics.organizationsCalls)
    }

    private fun sevenSites(): List<app.rybbitclient.core.model.Site> = listOf(
        testSite(1, "Shop"),
        testSite(2, "Blog", domain = "news.example.com"),
        testSite(3, "Docs"),
        testSite(4, "Status"),
        testSite(5, "Forum"),
        testSite(6, "Wiki"),
        testSite(7, "Store"),
    )

    private fun instance(id: String) = Instance(
        id = id,
        displayName = "Server $id",
        baseUrl = "https://$id.example.com",
        kind = InstanceKind.SelfHosted,
        authType = AuthType.ApiKey,
        createdAtMillis = 0L,
    )

    /** Repository with [instances] added in order; the first one is active. */
    private suspend fun repositoryWith(vararg instances: Instance): InstanceRepository {
        val repository = InstanceRepository(store = InMemoryKeyValueStore(), secrets = InMemorySecretStore())
        instances.forEach { repository.add(it, Credential.ApiKey("key-${it.id}")) }
        return repository
    }
}
