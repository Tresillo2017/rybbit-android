package app.rybbitclient.feature.more

import app.rybbitclient.core.data.AnalyticsRepository
import app.rybbitclient.core.data.Feature
import app.rybbitclient.core.model.AuthType
import app.rybbitclient.core.model.ErrorEvent
import app.rybbitclient.core.model.ErrorName
import app.rybbitclient.core.model.ErrorTimePoint
import app.rybbitclient.core.model.EventName
import app.rybbitclient.core.model.EventPropertyCount
import app.rybbitclient.core.model.EventTimePoint
import app.rybbitclient.core.model.EventsResponse
import app.rybbitclient.core.model.Funnel
import app.rybbitclient.core.model.FunnelStep
import app.rybbitclient.core.model.FunnelStepResult
import app.rybbitclient.core.model.GoalsResponse
import app.rybbitclient.core.model.Instance
import app.rybbitclient.core.model.InstanceKind
import app.rybbitclient.core.model.MetricPage
import app.rybbitclient.core.model.Organization
import app.rybbitclient.core.model.Overview
import app.rybbitclient.core.model.OverviewPoint
import app.rybbitclient.core.model.PageRow
import app.rybbitclient.core.model.PerformanceOverview
import app.rybbitclient.core.model.RetentionData
import app.rybbitclient.core.model.SessionDetailResponse
import app.rybbitclient.core.model.SessionLocation
import app.rybbitclient.core.model.SessionSummary
import app.rybbitclient.core.model.Site
import app.rybbitclient.core.model.UserDetail
import app.rybbitclient.core.model.UsersResponse
import app.rybbitclient.core.network.Credential
import app.rybbitclient.core.network.Server
import app.rybbitclient.core.query.AnalyticsQuery
import app.rybbitclient.core.query.TimeBucket
import app.rybbitclient.core.storage.InMemoryKeyValueStore
import app.rybbitclient.core.storage.InMemorySecretStore
import app.rybbitclient.core.storage.InstanceRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import kotlinx.coroutines.ExperimentalCoroutinesApi

/** Template for feature ViewModel tests: a hand-written fake repository, Main swapped for an unconfined dispatcher. */
@OptIn(ExperimentalCoroutinesApi::class)
class MoreViewModelTest {

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
    fun `loads funnel count and hides web vitals on self-hosted`() = runTest(dispatcher) {
        // Arrange
        val instances = instanceRepositoryWith(InstanceKind.SelfHosted)
        val analytics = FakeAnalytics(funnelsResult = { listOf(funnel(1), funnel(2)) })

        // Act
        val viewModel = MoreViewModel(SITE_ID, analytics, instances)

        // Assert
        assertEquals(MoreUiState.Content(funnelCount = 2, webVitalsAvailable = false), viewModel.uiState.value)
    }

    @Test
    fun `shows web vitals on cloud when the feature is available`() = runTest(dispatcher) {
        // Arrange
        val instances = instanceRepositoryWith(InstanceKind.Cloud)
        val analytics = FakeAnalytics(performanceAvailable = true)

        // Act
        val viewModel = MoreViewModel(SITE_ID, analytics, instances)

        // Assert
        assertEquals(MoreUiState.Content(funnelCount = 0, webVitalsAvailable = true), viewModel.uiState.value)
    }

    @Test
    fun `reports an error state when the funnel request fails`() = runTest(dispatcher) {
        // Arrange
        val instances = instanceRepositoryWith(InstanceKind.Cloud)
        val analytics = FakeAnalytics(funnelsResult = { throw serverError() })

        // Act
        val viewModel = MoreViewModel(SITE_ID, analytics, instances)

        // Assert
        assertTrue(viewModel.uiState.value is MoreUiState.Error)
    }

    @Test
    fun `refresh recovers from an error and returns to content`() = runTest(dispatcher) {
        // Arrange
        val instances = instanceRepositoryWith(InstanceKind.Cloud)
        var calls = 0
        val analytics = FakeAnalytics(funnelsResult = {
            calls += 1
            if (calls == 1) throw serverError() else listOf(funnel(7))
        })
        val viewModel = MoreViewModel(SITE_ID, analytics, instances)

        // Act
        viewModel.refresh()

        // Assert: the cloud instance with the feature available keeps web vitals visible after refresh
        assertEquals(MoreUiState.Content(funnelCount = 1, webVitalsAvailable = true), viewModel.uiState.value)
    }

    private suspend fun instanceRepositoryWith(kind: InstanceKind): InstanceRepository {
        val repository = InstanceRepository(store = InMemoryKeyValueStore(), secrets = InMemorySecretStore())
        repository.add(
            Instance(
                id = "instance-1",
                displayName = "Test instance",
                baseUrl = "https://analytics.example.com",
                kind = kind,
                authType = AuthType.ApiKey,
                createdAtMillis = 0L,
            ),
            Credential.ApiKey("test-key"),
        )
        return repository
    }

    private fun funnel(id: Int) = Funnel(id = id, name = "Funnel $id")

    private fun serverError() = Server(status = 500, code = null, serverMessage = null, resolution = null)

    private companion object {
        const val SITE_ID = "1"
    }
}

/** Fake repository. Only the calls MoreViewModel makes are real; everything else fails loudly if reached. */
private class FakeAnalytics(
    private val funnelsResult: () -> List<Funnel> = { emptyList() },
    private val performanceAvailable: Boolean = true,
) : AnalyticsRepository {

    override suspend fun isFeatureAvailable(feature: Feature): Boolean = performanceAvailable

    override suspend fun funnels(siteId: String): List<Funnel> = funnelsResult()

    override suspend fun organizations(): List<Organization> = unused()
    override suspend fun sites(organizationId: String): List<Site> = unused()
    override suspend fun site(siteId: String): Site = unused()
    override suspend fun overview(siteId: String, query: AnalyticsQuery): Overview = unused()
    override suspend fun overviewTimeSeries(siteId: String, query: AnalyticsQuery, bucket: TimeBucket): List<OverviewPoint> = unused()
    override suspend fun metric(siteId: String, query: AnalyticsQuery, parameter: String, limit: Int, page: Int): MetricPage = unused()
    override suspend fun liveUserCount(siteId: String, minutes: Int): Int = unused()
    override suspend fun sessions(siteId: String, query: AnalyticsQuery, page: Int, limit: Int): List<SessionSummary> = unused()
    override suspend fun sessionDetail(siteId: String, sessionId: String): SessionDetailResponse = unused()
    override suspend fun sessionLocations(siteId: String, query: AnalyticsQuery): List<SessionLocation> = unused()
    override suspend fun events(siteId: String, query: AnalyticsQuery, pageSize: Int, beforeCursor: String?): EventsResponse = unused()
    override suspend fun eventNames(siteId: String, query: AnalyticsQuery): List<EventName> = unused()
    override suspend fun eventProperties(siteId: String, query: AnalyticsQuery, eventName: String): List<EventPropertyCount> = unused()
    override suspend fun eventsTimeSeries(siteId: String, query: AnalyticsQuery, bucket: TimeBucket): List<EventTimePoint> = unused()
    override suspend fun errorNames(siteId: String, query: AnalyticsQuery, limit: Int): List<ErrorName> = unused()
    override suspend fun errorEvents(siteId: String, query: AnalyticsQuery, errorMessage: String, limit: Int): List<ErrorEvent> = unused()
    override suspend fun errorTimeSeries(siteId: String, query: AnalyticsQuery, bucket: TimeBucket, errorMessage: String?): List<ErrorTimePoint> = unused()
    override suspend fun goals(siteId: String, query: AnalyticsQuery, page: Int, pageSize: Int): GoalsResponse = unused()
    override suspend fun goalSessions(siteId: String, query: AnalyticsQuery, goalId: Int, page: Int, limit: Int): List<SessionSummary> = unused()
    override suspend fun analyzeFunnel(siteId: String, query: AnalyticsQuery, steps: List<FunnelStep>): List<FunnelStepResult> = unused()
    override suspend fun users(siteId: String, query: AnalyticsQuery, page: Int, pageSize: Int): UsersResponse = unused()
    override suspend fun userDetail(siteId: String, userId: String): UserDetail = unused()
    override suspend fun retention(siteId: String, mode: String, range: Int): RetentionData = unused()
    override suspend fun performanceOverview(siteId: String, query: AnalyticsQuery): PerformanceOverview = unused()
    override suspend fun pages(siteId: String, query: AnalyticsQuery, limit: Int): List<PageRow> = unused()

    private fun unused(): Nothing = error("Not used by MoreViewModel")
}
