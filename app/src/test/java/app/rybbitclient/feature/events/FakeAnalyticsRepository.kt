package app.rybbitclient.feature.events

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
import app.rybbitclient.core.network.ApiException
import app.rybbitclient.core.query.AnalyticsQuery
import app.rybbitclient.core.query.TimeBucket

/**
 * Shared test fake for the events and errors ViewModels. Set the fields a test needs; any call to a
 * method outside this list fails loudly. [failure], when set, is thrown by every fake call.
 * Calls are recorded so tests can assert on the queries and cursors that were sent.
 */
internal class FakeAnalyticsRepository : AnalyticsRepository {

    var failure: ApiException? = null

    var eventNamesResult: List<EventName> = emptyList()
    var eventTimeSeriesResult: List<EventTimePoint> = emptyList()

    /** Event pages keyed by the `beforeCursor` that requests them. The first page is keyed by null. */
    var eventPages: Map<String?, EventsResponse> = emptyMap()
    val eventQueries = mutableListOf<AnalyticsQuery>()
    val eventCursors = mutableListOf<String?>()

    var propertyRows: List<EventPropertyCount> = emptyList()
    val propertyEventNames = mutableListOf<String>()

    var errorNamesResult: List<ErrorName> = emptyList()
    var errorTimeSeriesResult: List<ErrorTimePoint> = emptyList()
    var errorEventsResult: List<ErrorEvent> = emptyList()
    val errorEventMessages = mutableListOf<String>()

    override suspend fun isFeatureAvailable(feature: Feature): Boolean = true

    override suspend fun events(siteId: String, query: AnalyticsQuery, pageSize: Int, beforeCursor: String?): EventsResponse {
        failure?.let { throw it }
        eventQueries += query
        eventCursors += beforeCursor
        return eventPages[beforeCursor] ?: EventsResponse()
    }

    override suspend fun eventNames(siteId: String, query: AnalyticsQuery): List<EventName> {
        failure?.let { throw it }
        return eventNamesResult
    }

    override suspend fun eventProperties(siteId: String, query: AnalyticsQuery, eventName: String): List<EventPropertyCount> {
        failure?.let { throw it }
        propertyEventNames += eventName
        return propertyRows
    }

    override suspend fun eventsTimeSeries(siteId: String, query: AnalyticsQuery, bucket: TimeBucket): List<EventTimePoint> {
        failure?.let { throw it }
        return eventTimeSeriesResult
    }

    override suspend fun errorNames(siteId: String, query: AnalyticsQuery, limit: Int): List<ErrorName> {
        failure?.let { throw it }
        return errorNamesResult
    }

    override suspend fun errorEvents(siteId: String, query: AnalyticsQuery, errorMessage: String, limit: Int): List<ErrorEvent> {
        failure?.let { throw it }
        errorEventMessages += errorMessage
        return errorEventsResult
    }

    override suspend fun errorTimeSeries(siteId: String, query: AnalyticsQuery, bucket: TimeBucket, errorMessage: String?): List<ErrorTimePoint> {
        failure?.let { throw it }
        return errorTimeSeriesResult
    }

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
    override suspend fun goals(siteId: String, query: AnalyticsQuery, page: Int, pageSize: Int): GoalsResponse = unused()
    override suspend fun goalSessions(siteId: String, query: AnalyticsQuery, goalId: Int, page: Int, limit: Int): List<SessionSummary> = unused()
    override suspend fun funnels(siteId: String): List<Funnel> = unused()
    override suspend fun analyzeFunnel(siteId: String, query: AnalyticsQuery, steps: List<FunnelStep>): List<FunnelStepResult> = unused()
    override suspend fun users(siteId: String, query: AnalyticsQuery, page: Int, pageSize: Int): UsersResponse = unused()
    override suspend fun userDetail(siteId: String, userId: String): UserDetail = unused()
    override suspend fun retention(siteId: String, mode: String, range: Int): RetentionData = unused()
    override suspend fun performanceOverview(siteId: String, query: AnalyticsQuery): PerformanceOverview = unused()
    override suspend fun pages(siteId: String, query: AnalyticsQuery, limit: Int): List<PageRow> = unused()

    private fun unused(): Nothing = error("Not used by the events or errors ViewModels under test")
}
