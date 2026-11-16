package app.rybbitclient.feature.goals.shared

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
 * Hand-written [AnalyticsRepository] for ViewModel tests. Each handler defaults to failing loudly, so a
 * test only stubs the calls its ViewModel makes. Handlers are `var` so a test can change them mid-test.
 */
class FakeSiteAnalytics(
    var goalsHandler: (AnalyticsQuery, Int) -> GoalsResponse = { _, _ -> unstubbed("goals") },
    var goalSessionsHandler: (AnalyticsQuery, Int) -> List<SessionSummary> = { _, _ -> unstubbed("goalSessions") },
    var funnelsHandler: () -> List<Funnel> = { unstubbed("funnels") },
    var analyzeHandler: (AnalyticsQuery, List<FunnelStep>) -> List<FunnelStepResult> = { _, _ -> unstubbed("analyzeFunnel") },
    var retentionHandler: (String, Int) -> RetentionData = { _, _ -> unstubbed("retention") },
    var performanceHandler: (AnalyticsQuery) -> PerformanceOverview = { unstubbed("performanceOverview") },
    var performanceAvailable: Boolean = true,
) : AnalyticsRepository {

    override suspend fun isFeatureAvailable(feature: Feature): Boolean = performanceAvailable

    override suspend fun goals(siteId: String, query: AnalyticsQuery, page: Int, pageSize: Int): GoalsResponse =
        goalsHandler(query, page)

    override suspend fun goalSessions(siteId: String, query: AnalyticsQuery, goalId: Int, page: Int, limit: Int): List<SessionSummary> =
        goalSessionsHandler(query, goalId)

    override suspend fun funnels(siteId: String): List<Funnel> = funnelsHandler()

    override suspend fun analyzeFunnel(siteId: String, query: AnalyticsQuery, steps: List<FunnelStep>): List<FunnelStepResult> =
        analyzeHandler(query, steps)

    override suspend fun retention(siteId: String, mode: String, range: Int): RetentionData = retentionHandler(mode, range)

    override suspend fun performanceOverview(siteId: String, query: AnalyticsQuery): PerformanceOverview = performanceHandler(query)

    override suspend fun organizations(): List<Organization> = unstubbed("organizations")
    override suspend fun sites(organizationId: String): List<Site> = unstubbed("sites")
    override suspend fun site(siteId: String): Site = unstubbed("site")
    override suspend fun overview(siteId: String, query: AnalyticsQuery): Overview = unstubbed("overview")
    override suspend fun overviewTimeSeries(siteId: String, query: AnalyticsQuery, bucket: TimeBucket): List<OverviewPoint> =
        unstubbed("overviewTimeSeries")
    override suspend fun metric(siteId: String, query: AnalyticsQuery, parameter: String, limit: Int, page: Int): MetricPage =
        unstubbed("metric")
    override suspend fun liveUserCount(siteId: String, minutes: Int): Int = unstubbed("liveUserCount")
    override suspend fun sessions(siteId: String, query: AnalyticsQuery, page: Int, limit: Int): List<SessionSummary> =
        unstubbed("sessions")
    override suspend fun sessionDetail(siteId: String, sessionId: String): SessionDetailResponse = unstubbed("sessionDetail")
    override suspend fun sessionLocations(siteId: String, query: AnalyticsQuery): List<SessionLocation> =
        unstubbed("sessionLocations")
    override suspend fun events(siteId: String, query: AnalyticsQuery, pageSize: Int, beforeCursor: String?): EventsResponse =
        unstubbed("events")
    override suspend fun eventNames(siteId: String, query: AnalyticsQuery): List<EventName> = unstubbed("eventNames")
    override suspend fun eventProperties(siteId: String, query: AnalyticsQuery, eventName: String): List<EventPropertyCount> =
        unstubbed("eventProperties")
    override suspend fun eventsTimeSeries(siteId: String, query: AnalyticsQuery, bucket: TimeBucket): List<EventTimePoint> =
        unstubbed("eventsTimeSeries")
    override suspend fun errorNames(siteId: String, query: AnalyticsQuery, limit: Int): List<ErrorName> = unstubbed("errorNames")
    override suspend fun errorEvents(siteId: String, query: AnalyticsQuery, errorMessage: String, limit: Int): List<ErrorEvent> =
        unstubbed("errorEvents")
    override suspend fun errorTimeSeries(siteId: String, query: AnalyticsQuery, bucket: TimeBucket, errorMessage: String?): List<ErrorTimePoint> =
        unstubbed("errorTimeSeries")
    override suspend fun users(siteId: String, query: AnalyticsQuery, page: Int, pageSize: Int): UsersResponse = unstubbed("users")
    override suspend fun userDetail(siteId: String, userId: String): UserDetail = unstubbed("userDetail")
    override suspend fun pages(siteId: String, query: AnalyticsQuery, limit: Int): List<PageRow> = unstubbed("pages")

    private companion object {
        fun unstubbed(name: String): Nothing = error("FakeSiteAnalytics.$name was not stubbed for this test")
    }
}
