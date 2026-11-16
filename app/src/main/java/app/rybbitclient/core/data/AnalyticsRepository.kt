package app.rybbitclient.core.data

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
import app.rybbitclient.core.model.OverviewPoint
import app.rybbitclient.core.model.Overview
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
 * All analytics reads for the active instance. Every method takes the site id and a query, and throws
 * [app.rybbitclient.core.network.ApiException] subtypes on failure. Writes are not exposed.
 */
interface AnalyticsRepository {

    /** Whether [feature] is known to exist on the active instance. Unknown features count as available. */
    suspend fun isFeatureAvailable(feature: Feature): Boolean

    suspend fun organizations(): List<Organization>

    suspend fun sites(organizationId: String): List<Site>

    suspend fun site(siteId: String): Site

    suspend fun overview(siteId: String, query: AnalyticsQuery): Overview

    suspend fun overviewTimeSeries(siteId: String, query: AnalyticsQuery, bucket: TimeBucket): List<OverviewPoint>

    suspend fun metric(
        siteId: String,
        query: AnalyticsQuery,
        parameter: String,
        limit: Int = DEFAULT_LIMIT,
        page: Int = 1,
    ): MetricPage

    suspend fun liveUserCount(siteId: String, minutes: Int = DEFAULT_LIVE_MINUTES): Int

    suspend fun sessions(siteId: String, query: AnalyticsQuery, page: Int = 1, limit: Int = DEFAULT_LIMIT): List<SessionSummary>

    suspend fun sessionDetail(siteId: String, sessionId: String): SessionDetailResponse

    /**
     * One page of a session's events, oldest first. Continue while `pagination.hasMore` is true.
     * The default falls back to [sessionDetail], which returns only the server's first page.
     */
    suspend fun sessionEventsPage(siteId: String, sessionId: String, offset: Int, limit: Int): SessionDetailResponse =
        sessionDetail(siteId, sessionId)

    /** Sessions of one user, newest first. [userId] is the identified ID or the anonymous device ID. */
    suspend fun userSessions(
        siteId: String,
        query: AnalyticsQuery,
        userId: String,
        page: Int = 1,
        limit: Int = DEFAULT_LIMIT,
    ): List<SessionSummary> = throw UnsupportedOperationException("userSessions is not implemented by this repository")

    suspend fun sessionLocations(siteId: String, query: AnalyticsQuery): List<SessionLocation>

    suspend fun events(
        siteId: String,
        query: AnalyticsQuery,
        pageSize: Int = DEFAULT_LIMIT,
        beforeCursor: String? = null,
        beforeTimestamp: String? = null,
    ): EventsResponse

    suspend fun eventNames(siteId: String, query: AnalyticsQuery): List<EventName>

    suspend fun eventProperties(siteId: String, query: AnalyticsQuery, eventName: String): List<EventPropertyCount>

    suspend fun eventsTimeSeries(siteId: String, query: AnalyticsQuery, bucket: TimeBucket): List<EventTimePoint>

    suspend fun errorNames(siteId: String, query: AnalyticsQuery, limit: Int = DEFAULT_LIMIT): List<ErrorName>

    suspend fun errorEvents(siteId: String, query: AnalyticsQuery, errorMessage: String, limit: Int = DEFAULT_LIMIT): List<ErrorEvent>

    suspend fun errorTimeSeries(siteId: String, query: AnalyticsQuery, bucket: TimeBucket, errorMessage: String): List<ErrorTimePoint>

    suspend fun goals(siteId: String, query: AnalyticsQuery, page: Int = 1, pageSize: Int = DEFAULT_GOALS_PAGE_SIZE): GoalsResponse

    suspend fun goalSessions(siteId: String, query: AnalyticsQuery, goalId: Int, page: Int = 1, limit: Int = DEFAULT_LIMIT): List<SessionSummary>

    suspend fun funnels(siteId: String): List<Funnel>

    suspend fun analyzeFunnel(siteId: String, query: AnalyticsQuery, steps: List<FunnelStep>): List<FunnelStepResult>

    suspend fun users(siteId: String, query: AnalyticsQuery, page: Int = 1, pageSize: Int = DEFAULT_USERS_PAGE_SIZE): UsersResponse

    /**
     * Users list with optional search (matched on the listed user ID) and identified-only filter.
     * The default ignores both filters and falls back to [users].
     */
    suspend fun usersFiltered(
        siteId: String,
        query: AnalyticsQuery,
        page: Int = 1,
        pageSize: Int = DEFAULT_USERS_PAGE_SIZE,
        identifiedOnly: Boolean = false,
        search: String? = null,
    ): UsersResponse = users(siteId, query, page, pageSize)

    suspend fun userDetail(siteId: String, userId: String): UserDetail

    suspend fun retention(siteId: String, mode: String = "day", range: Int = DEFAULT_RETENTION_RANGE): RetentionData

    suspend fun performanceOverview(siteId: String, query: AnalyticsQuery): PerformanceOverview

    /** Cloud-only report. Throws FeatureUnavailable on self-hosted and records that for the instance. */
    suspend fun pages(siteId: String, query: AnalyticsQuery, limit: Int = DEFAULT_LIMIT): List<PageRow>

    companion object {
        const val DEFAULT_LIMIT = 50
        const val DEFAULT_LIVE_MINUTES = 5
        const val DEFAULT_GOALS_PAGE_SIZE = 10
        const val DEFAULT_USERS_PAGE_SIZE = 100
        const val DEFAULT_RETENTION_RANGE = 30
    }
}
