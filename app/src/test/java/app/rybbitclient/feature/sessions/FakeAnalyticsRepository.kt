package app.rybbitclient.feature.sessions

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
import app.rybbitclient.core.model.UserSummary
import app.rybbitclient.core.model.UsersResponse
import app.rybbitclient.core.query.AnalyticsQuery
import app.rybbitclient.core.query.TimeBucket

/** A request the fake received for the session or user endpoints, for assertions. */
data class PagedRequest(val page: Int, val limit: Int, val query: AnalyticsQuery, val userId: String? = null)

/**
 * Fake repository for the session and user ViewModels. Each endpoint the screens use is driven by a lambda that
 * tests set; every other method fails loudly if a test reaches it by mistake.
 */
class FakeAnalyticsRepository : AnalyticsRepository {

    val sessionRequests = mutableListOf<PagedRequest>()
    val userSessionRequests = mutableListOf<PagedRequest>()
    val userRequests = mutableListOf<UsersRequest>()

    var sessionsPage: (page: Int) -> List<SessionSummary> = { emptyList() }
    var eventsPage: (offset: Int, limit: Int) -> SessionDetailResponse = { _, _ -> error("eventsPage not set") }
    var userSessionsPage: (page: Int) -> List<SessionSummary> = { emptyList() }
    var usersPage: (page: Int) -> UsersResponse = { UsersResponse() }
    var userDetailResult: () -> UserDetail = { error("userDetailResult not set") }

    /** Return a throwable for a page to make that `sessions` request fail; null lets it succeed. */
    var failSessions: ((page: Int) -> Throwable?) = { null }

    override suspend fun sessions(siteId: String, query: AnalyticsQuery, page: Int, limit: Int): List<SessionSummary> {
        sessionRequests += PagedRequest(page, limit, query)
        failSessions(page)?.let { throw it }
        return sessionsPage(page)
    }

    override suspend fun sessionEventsPage(siteId: String, sessionId: String, offset: Int, limit: Int): SessionDetailResponse =
        eventsPage(offset, limit)

    override suspend fun userSessions(siteId: String, query: AnalyticsQuery, userId: String, page: Int, limit: Int): List<SessionSummary> {
        userSessionRequests += PagedRequest(page, limit, query, userId)
        return userSessionsPage(page)
    }

    override suspend fun usersFiltered(
        siteId: String,
        query: AnalyticsQuery,
        page: Int,
        pageSize: Int,
        identifiedOnly: Boolean,
        search: String?,
    ): UsersResponse {
        userRequests += UsersRequest(page, pageSize, identifiedOnly, search, query)
        return usersPage(page)
    }

    override suspend fun userDetail(siteId: String, userId: String): UserDetail = userDetailResult()

    override suspend fun isFeatureAvailable(feature: Feature): Boolean = true
    override suspend fun organizations(): List<Organization> = unused()
    override suspend fun sites(organizationId: String): List<Site> = unused()
    override suspend fun site(siteId: String): Site = unused()
    override suspend fun overview(siteId: String, query: AnalyticsQuery): Overview = unused()
    override suspend fun overviewTimeSeries(siteId: String, query: AnalyticsQuery, bucket: TimeBucket): List<OverviewPoint> = unused()
    override suspend fun metric(siteId: String, query: AnalyticsQuery, parameter: String, limit: Int, page: Int): MetricPage = unused()
    override suspend fun liveUserCount(siteId: String, minutes: Int): Int = unused()
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
    override suspend fun retention(siteId: String, mode: String, range: Int): RetentionData = unused()
    override suspend fun performanceOverview(siteId: String, query: AnalyticsQuery): PerformanceOverview = unused()
    override suspend fun pages(siteId: String, query: AnalyticsQuery, limit: Int): List<PageRow> = unused()

    private fun unused(): Nothing = error("Not used by the sessions and users ViewModels")
}

/** A `usersFiltered` call the fake received. */
data class UsersRequest(
    val page: Int,
    val pageSize: Int,
    val identifiedOnly: Boolean,
    val search: String?,
    val query: AnalyticsQuery,
)

/** Builds a session summary with only the fields a test cares about. */
fun session(id: String, start: String? = null): SessionSummary = SessionSummary(sessionId = id, sessionStart = start)

/** Builds a user summary with only the fields a test cares about. */
fun user(id: String): UserSummary = UserSummary(userId = id)

/** `count` items built by [make], so page sizes in tests read clearly. */
fun <T> items(count: Int, offset: Int = 0, make: (Int) -> T): List<T> = (offset until offset + count).map(make)
