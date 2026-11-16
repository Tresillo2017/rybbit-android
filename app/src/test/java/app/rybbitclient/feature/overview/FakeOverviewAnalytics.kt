package app.rybbitclient.feature.overview

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

/** One recorded `metric` call. */
data class MetricCall(val query: AnalyticsQuery, val parameter: String, val limit: Int, val page: Int)

/**
 * Hand-written [AnalyticsRepository] for Overview and Realtime tests. Every handler defaults to a
 * fixed answer; calls are recorded so tests can assert on request counts and parameters. Methods the
 * screens don't use throw, so an accidental call fails the test loudly.
 */
class FakeOverviewAnalytics : AnalyticsRepository {

    val overviewQueries = mutableListOf<AnalyticsQuery>()
    val seriesQueries = mutableListOf<AnalyticsQuery>()
    val metricCalls = mutableListOf<MetricCall>()
    val eventCalls = mutableListOf<AnalyticsQuery>()
    var liveCalls = 0
    var trendCalls = 0

    var overviewHandler: suspend (AnalyticsQuery) -> Overview = { overviewOf(users = 10) }
    var seriesHandler: suspend (AnalyticsQuery, TimeBucket) -> List<OverviewPoint> = { _, _ -> emptyList() }
    var metricHandler: suspend (MetricCall) -> MetricPage = { MetricPage() }
    var liveHandler: suspend () -> Int = { 0 }
    var eventsHandler: suspend (AnalyticsQuery) -> EventsResponse = { EventsResponse() }

    override suspend fun overview(siteId: String, query: AnalyticsQuery): Overview {
        overviewQueries += query
        return overviewHandler(query)
    }

    override suspend fun overviewTimeSeries(siteId: String, query: AnalyticsQuery, bucket: TimeBucket): List<OverviewPoint> {
        seriesQueries += query
        return seriesHandler(query, bucket)
    }

    override suspend fun metric(
        siteId: String,
        query: AnalyticsQuery,
        parameter: String,
        limit: Int,
        page: Int,
    ): MetricPage {
        val call = MetricCall(query, parameter, limit, page)
        metricCalls += call
        return metricHandler(call)
    }

    override suspend fun liveUserCount(siteId: String, minutes: Int): Int {
        liveCalls += 1
        return liveHandler()
    }

    override suspend fun events(siteId: String, query: AnalyticsQuery, pageSize: Int, beforeCursor: String?, beforeTimestamp: String?): EventsResponse {
        eventCalls += query
        return eventsHandler(query)
    }

    override suspend fun isFeatureAvailable(feature: Feature): Boolean = unused()
    override suspend fun organizations(): List<Organization> = unused()
    override suspend fun sites(organizationId: String): List<Site> = unused()
    override suspend fun site(siteId: String): Site = unused()
    override suspend fun sessions(siteId: String, query: AnalyticsQuery, page: Int, limit: Int): List<SessionSummary> = unused()
    override suspend fun sessionDetail(siteId: String, sessionId: String): SessionDetailResponse = unused()
    override suspend fun sessionLocations(siteId: String, query: AnalyticsQuery): List<SessionLocation> = unused()
    override suspend fun eventNames(siteId: String, query: AnalyticsQuery): List<EventName> = unused()
    override suspend fun eventProperties(siteId: String, query: AnalyticsQuery, eventName: String): List<EventPropertyCount> = unused()
    override suspend fun eventsTimeSeries(siteId: String, query: AnalyticsQuery, bucket: TimeBucket): List<EventTimePoint> = unused()
    override suspend fun errorNames(siteId: String, query: AnalyticsQuery, limit: Int): List<ErrorName> = unused()
    override suspend fun errorEvents(siteId: String, query: AnalyticsQuery, errorMessage: String, limit: Int): List<ErrorEvent> = unused()
    override suspend fun errorTimeSeries(siteId: String, query: AnalyticsQuery, bucket: TimeBucket, errorMessage: String): List<ErrorTimePoint> =
        unused()
    override suspend fun goals(siteId: String, query: AnalyticsQuery, page: Int, pageSize: Int): GoalsResponse = unused()
    override suspend fun goalSessions(siteId: String, query: AnalyticsQuery, goalId: Int, page: Int, limit: Int): List<SessionSummary> =
        unused()
    override suspend fun funnels(siteId: String): List<Funnel> = unused()
    override suspend fun analyzeFunnel(siteId: String, query: AnalyticsQuery, steps: List<FunnelStep>): List<FunnelStepResult> = unused()
    override suspend fun users(siteId: String, query: AnalyticsQuery, page: Int, pageSize: Int): UsersResponse = unused()
    override suspend fun userDetail(siteId: String, userId: String): UserDetail = unused()
    override suspend fun retention(siteId: String, mode: String, range: Int): RetentionData = unused()
    override suspend fun performanceOverview(siteId: String, query: AnalyticsQuery): PerformanceOverview = unused()
    override suspend fun pages(siteId: String, query: AnalyticsQuery, limit: Int): List<PageRow> = unused()

    private fun unused(): Nothing = error("Not used by the Overview or Realtime ViewModels")
}

/** Shorthand for a full [Overview] with the fields the screens read. */
fun overviewOf(
    users: Int = 0,
    sessions: Int = 0,
    pageviews: Int = 0,
    pagesPerSession: Double = 0.0,
    bounceRate: Double = 0.0,
    sessionDuration: Double = 0.0,
): Overview = Overview(
    sessions = sessions,
    pagesPerSession = pagesPerSession,
    bounceRate = bounceRate,
    sessionDuration = sessionDuration,
    pageviews = pageviews,
    users = users,
)

fun point(time: String, users: Int = 0, sessions: Int = 0, pageviews: Int = 0): OverviewPoint = OverviewPoint(
    time = time,
    sessions = sessions,
    users = users,
    pageviews = pageviews,
)

fun metricRow(value: String?, count: Int): app.rybbitclient.core.model.MetricRow =
    app.rybbitclient.core.model.MetricRow(value = value, count = count, percentage = 0.0)
