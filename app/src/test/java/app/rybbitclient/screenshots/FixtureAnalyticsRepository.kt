package app.rybbitclient.screenshots

import app.rybbitclient.core.data.AnalyticsRepository
import app.rybbitclient.core.data.Feature
import app.rybbitclient.core.model.ErrorName
import app.rybbitclient.core.model.EventName
import app.rybbitclient.core.model.EventPropertyCount
import app.rybbitclient.core.model.EventTimePoint
import app.rybbitclient.core.model.EventsResponse
import app.rybbitclient.core.model.ErrorEvent
import app.rybbitclient.core.model.ErrorTimePoint
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
import app.rybbitclient.testsupport.FixtureFiles
import app.rybbitclient.testsupport.apiReplying
import io.ktor.http.HttpStatusCode

/**
 * Analytics repository that answers every call from the real captured responses in docs/fixtures.
 * Each call goes through the production [app.rybbitclient.core.network.RybbitApi] decoders, so the
 * screens get exactly the models the live app would decode.
 *
 * Overview time series are trimmed to the screenshot range (Nov 10-16), where the fixtures have events.
 * Metrics without a fixture return an empty page. `pages` replays the server's 404 so the feature
 * is reported as unavailable, as it is on self-hosted servers.
 */
class FixtureAnalyticsRepository : AnalyticsRepository {

    private fun fixtureApi(fileName: String, status: HttpStatusCode = HttpStatusCode.OK) =
        apiReplying(FixtureFiles.read(fileName), status = status)

    override suspend fun isFeatureAvailable(feature: Feature): Boolean = feature != Feature.Pages

    override suspend fun organizations(): List<Organization> = fixtureApi("organizations.json").organizations()

    override suspend fun sites(organizationId: String): List<Site> =
        fixtureApi("org_sites.json").orgSites(organizationId).sites

    override suspend fun site(siteId: String): Site = fixtureApi("site.json").site(siteId)

    override suspend fun overview(siteId: String, query: AnalyticsQuery): Overview =
        fixtureApi("overview.json").overview(siteId, query)

    override suspend fun overviewTimeSeries(siteId: String, query: AnalyticsQuery, bucket: TimeBucket): List<OverviewPoint> {
        val file = if (bucket == TimeBucket.Hour) "overview_timeseries_hour.json" else "overview_timeseries_day.json"
        return fixtureApi(file).overviewTimeSeries(siteId, query, bucket)
            .filter { it.time.orEmpty() in SCREENSHOT_RANGE_START..<SCREENSHOT_RANGE_END }
    }

    override suspend fun metric(siteId: String, query: AnalyticsQuery, parameter: String, limit: Int, page: Int): MetricPage {
        val body = METRIC_FIXTURES[parameter]?.let { FixtureFiles.read(it) } ?: EMPTY_METRIC_BODY
        return apiReplying(body).metric(siteId, query, parameter, limit, page)
    }

    override suspend fun liveUserCount(siteId: String, minutes: Int): Int =
        fixtureApi("live_user_count.json").liveUserCount(siteId, minutes).count

    override suspend fun sessions(siteId: String, query: AnalyticsQuery, page: Int, limit: Int): List<SessionSummary> =
        fixtureApi("sessions.json").sessions(siteId, query, page, limit)

    override suspend fun sessionDetail(siteId: String, sessionId: String): SessionDetailResponse =
        fixtureApi("session_detail.json").sessionDetail(siteId, sessionId)

    override suspend fun userSessions(
        siteId: String,
        query: AnalyticsQuery,
        userId: String,
        page: Int,
        limit: Int,
    ): List<SessionSummary> = fixtureApi("sessions.json").userSessions(siteId, query, userId, page, limit)

    override suspend fun sessionLocations(siteId: String, query: AnalyticsQuery): List<SessionLocation> =
        fixtureApi("session_locations.json").sessionLocations(siteId, query)

    override suspend fun events(
        siteId: String,
        query: AnalyticsQuery,
        pageSize: Int,
        beforeCursor: String?,
        beforeTimestamp: String?,
    ): EventsResponse = fixtureApi("events.json").events(siteId, query, pageSize, beforeCursor, beforeTimestamp)

    override suspend fun eventNames(siteId: String, query: AnalyticsQuery): List<EventName> =
        fixtureApi("event_names.json").eventNames(siteId, query)

    override suspend fun eventProperties(siteId: String, query: AnalyticsQuery, eventName: String): List<EventPropertyCount> =
        fixtureApi("event_properties.json").eventProperties(siteId, query, eventName)

    override suspend fun eventsTimeSeries(siteId: String, query: AnalyticsQuery, bucket: TimeBucket): List<EventTimePoint> =
        fixtureApi("events_timeseries.json").eventsTimeSeries(siteId, query, bucket)

    override suspend fun errorNames(siteId: String, query: AnalyticsQuery, limit: Int): List<ErrorName> =
        fixtureApi("errors_names.json").errorNames(siteId, query, limit)

    override suspend fun errorEvents(siteId: String, query: AnalyticsQuery, errorMessage: String, limit: Int): List<ErrorEvent> =
        fixtureApi("errors_events.json").errorEvents(siteId, query, errorMessage, limit)

    override suspend fun errorTimeSeries(
        siteId: String,
        query: AnalyticsQuery,
        bucket: TimeBucket,
        errorMessage: String,
    ): List<ErrorTimePoint> = fixtureApi("errors_timeseries.json").errorTimeSeries(siteId, query, bucket, errorMessage)

    override suspend fun goals(siteId: String, query: AnalyticsQuery, page: Int, pageSize: Int): GoalsResponse =
        fixtureApi("goals.json").goals(siteId, query, page, pageSize)

    override suspend fun goalSessions(siteId: String, query: AnalyticsQuery, goalId: Int, page: Int, limit: Int): List<SessionSummary> =
        fixtureApi("goal_sessions.json").goalSessions(siteId, query, goalId, page, limit)

    override suspend fun funnels(siteId: String): List<Funnel> = fixtureApi("funnels.json").funnels(siteId)

    override suspend fun analyzeFunnel(siteId: String, query: AnalyticsQuery, steps: List<FunnelStep>): List<FunnelStepResult> =
        fixtureApi("funnel_analyze.json").analyzeFunnel(siteId, query, steps)

    override suspend fun users(siteId: String, query: AnalyticsQuery, page: Int, pageSize: Int): UsersResponse =
        fixtureApi("users.json").users(siteId, query, page, pageSize)

    override suspend fun usersFiltered(
        siteId: String,
        query: AnalyticsQuery,
        page: Int,
        pageSize: Int,
        identifiedOnly: Boolean,
        search: String?,
    ): UsersResponse = fixtureApi("users.json").usersFiltered(siteId, query, page, pageSize, identifiedOnly, search)

    override suspend fun userDetail(siteId: String, userId: String): UserDetail =
        fixtureApi("user_detail.json").userDetail(siteId, userId)

    override suspend fun retention(siteId: String, mode: String, range: Int): RetentionData =
        fixtureApi("retention.json").retention(siteId, mode, range)

    override suspend fun performanceOverview(siteId: String, query: AnalyticsQuery): PerformanceOverview =
        fixtureApi("performance_overview.json").performanceOverview(siteId, query)

    override suspend fun pages(siteId: String, query: AnalyticsQuery, limit: Int): List<PageRow> =
        fixtureApi("pages.json", HttpStatusCode.NotFound).pages(siteId, query, limit)

    private companion object {
        /** Screenshot window: the fixtures only hold events on 2026-11-16. */
        const val SCREENSHOT_RANGE_START = "2026-11-10"
        const val SCREENSHOT_RANGE_END = "2026-11-17"

        const val EMPTY_METRIC_BODY = """{"data":{"data":[],"totalCount":0}}"""

        val METRIC_FIXTURES = mapOf(
            "browser" to "metric_browser.json",
            "channel" to "metric_channel.json",
            "country" to "metric_country.json",
            "device_type" to "metric_device_type.json",
            "operating_system" to "metric_operating_system.json",
            "pathname" to "metric_pathname.json",
            "referrer" to "metric_referrer.json",
        )
    }
}
