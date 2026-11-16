package app.rybbitclient.feature.sites

import app.rybbitclient.core.data.AnalyticsRepository
import app.rybbitclient.core.data.Feature
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
import app.rybbitclient.core.query.AnalyticsQuery
import app.rybbitclient.core.query.TimeBucket

/**
 * Fake for the Sites ViewModel. Only organizations, sites and live counts are scripted; every other call
 * fails loudly if a ViewModel reaches it by mistake.
 */
class FakeSitesAnalytics(
    var organizationsResult: () -> List<Organization> = { emptyList() },
    var sitesResult: (organizationId: String) -> List<Site> = { emptyList() },
    var liveCountResult: (siteId: String) -> Int = { 0 },
) : AnalyticsRepository {

    var organizationsCalls = 0
    val liveCountRequests = mutableListOf<String>()

    override suspend fun organizations(): List<Organization> {
        organizationsCalls += 1
        return organizationsResult()
    }

    override suspend fun sites(organizationId: String): List<Site> = sitesResult(organizationId)

    override suspend fun liveUserCount(siteId: String, minutes: Int): Int {
        liveCountRequests += siteId
        return liveCountResult(siteId)
    }

    override suspend fun isFeatureAvailable(feature: Feature): Boolean = unused()
    override suspend fun site(siteId: String): Site = unused()
    override suspend fun overview(siteId: String, query: AnalyticsQuery): Overview = unused()
    override suspend fun overviewTimeSeries(siteId: String, query: AnalyticsQuery, bucket: TimeBucket): List<OverviewPoint> = unused()
    override suspend fun metric(siteId: String, query: AnalyticsQuery, parameter: String, limit: Int, page: Int): MetricPage = unused()
    override suspend fun sessions(siteId: String, query: AnalyticsQuery, page: Int, limit: Int): List<SessionSummary> = unused()
    override suspend fun sessionDetail(siteId: String, sessionId: String): SessionDetailResponse = unused()
    override suspend fun sessionLocations(siteId: String, query: AnalyticsQuery): List<SessionLocation> = unused()
    override suspend fun events(siteId: String, query: AnalyticsQuery, pageSize: Int, beforeCursor: String?, beforeTimestamp: String?): EventsResponse = unused()
    override suspend fun eventNames(siteId: String, query: AnalyticsQuery): List<EventName> = unused()
    override suspend fun eventProperties(siteId: String, query: AnalyticsQuery, eventName: String): List<EventPropertyCount> = unused()
    override suspend fun eventsTimeSeries(siteId: String, query: AnalyticsQuery, bucket: TimeBucket): List<EventTimePoint> = unused()
    override suspend fun errorNames(siteId: String, query: AnalyticsQuery, limit: Int): List<ErrorName> = unused()
    override suspend fun errorEvents(siteId: String, query: AnalyticsQuery, errorMessage: String, limit: Int): List<ErrorEvent> = unused()
    override suspend fun errorTimeSeries(siteId: String, query: AnalyticsQuery, bucket: TimeBucket, errorMessage: String): List<ErrorTimePoint> = unused()
    override suspend fun goals(siteId: String, query: AnalyticsQuery, page: Int, pageSize: Int): GoalsResponse = unused()
    override suspend fun goalSessions(siteId: String, query: AnalyticsQuery, goalId: Int, page: Int, limit: Int): List<SessionSummary> = unused()
    override suspend fun funnels(siteId: String): List<Funnel> = unused()
    override suspend fun analyzeFunnel(siteId: String, query: AnalyticsQuery, steps: List<FunnelStep>): List<FunnelStepResult> = unused()
    override suspend fun users(siteId: String, query: AnalyticsQuery, page: Int, pageSize: Int): UsersResponse = unused()
    override suspend fun userDetail(siteId: String, userId: String): UserDetail = unused()
    override suspend fun retention(siteId: String, mode: String, range: Int): RetentionData = unused()
    override suspend fun performanceOverview(siteId: String, query: AnalyticsQuery): PerformanceOverview = unused()
    override suspend fun pages(siteId: String, query: AnalyticsQuery, limit: Int): List<PageRow> = unused()

    private fun unused(): Nothing = error("Not used by SitesViewModel")
}

/** A site with a numeric analytics id, as the list shows it. */
fun testSite(analyticsId: Long, name: String, domain: String? = "$name.example.com"): Site = Site(
    id = "hex$analyticsId",
    siteId = analyticsId,
    name = name,
    domain = domain,
)

/** An organization with its embedded sites. */
fun testOrganization(id: String, name: String, sites: List<Site> = emptyList()): Organization =
    Organization(id = id, name = name, sites = sites)
