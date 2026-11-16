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
import app.rybbitclient.core.network.FeatureUnavailable
import app.rybbitclient.core.network.RybbitApi
import app.rybbitclient.core.query.AnalyticsQuery
import app.rybbitclient.core.query.TimeBucket
import app.rybbitclient.core.storage.InstanceRepository
import kotlinx.coroutines.flow.first

/** Thrown by every read when no instance is selected. The UI routes to the instance picker. */
class NoActiveInstanceException : IllegalStateException("No Rybbit instance is selected.")

/**
 * [AnalyticsRepository] that resolves the active instance on every call, so switching instances
 * takes effect immediately. Feature-gated calls short-circuit once the server has reported them missing.
 */
class DefaultAnalyticsRepository(
    private val instances: InstanceRepository,
    private val apis: RybbitApiProvider,
    private val features: FeatureAvailability,
) : AnalyticsRepository {

    override suspend fun isFeatureAvailable(feature: Feature): Boolean =
        features.isAvailable(activeInstanceId(), feature)

    override suspend fun organizations(): List<Organization> = onActive { organizations() }

    override suspend fun sites(organizationId: String): List<Site> = onActive { orgSites(organizationId).sites }

    override suspend fun site(siteId: String): Site = onActive { site(siteId) }

    override suspend fun overview(siteId: String, query: AnalyticsQuery): Overview = onActive { overview(siteId, query) }

    override suspend fun overviewTimeSeries(siteId: String, query: AnalyticsQuery, bucket: TimeBucket): List<OverviewPoint> =
        onActive { overviewTimeSeries(siteId, query, bucket) }

    override suspend fun metric(siteId: String, query: AnalyticsQuery, parameter: String, limit: Int, page: Int): MetricPage =
        onActive { metric(siteId, query, parameter, limit, page) }

    override suspend fun liveUserCount(siteId: String, minutes: Int): Int = onActive { liveUserCount(siteId, minutes).count }

    override suspend fun sessions(siteId: String, query: AnalyticsQuery, page: Int, limit: Int): List<SessionSummary> =
        onActive { sessions(siteId, query, page, limit) }

    override suspend fun sessionDetail(siteId: String, sessionId: String): SessionDetailResponse =
        onActive { sessionDetail(siteId, sessionId) }

    override suspend fun sessionEventsPage(siteId: String, sessionId: String, offset: Int, limit: Int): SessionDetailResponse =
        onActive { sessionEventsPage(siteId, sessionId, offset, limit) }

    override suspend fun userSessions(
        siteId: String,
        query: AnalyticsQuery,
        userId: String,
        page: Int,
        limit: Int,
    ): List<SessionSummary> = onActive { userSessions(siteId, query, userId, page, limit) }

    override suspend fun sessionLocations(siteId: String, query: AnalyticsQuery): List<SessionLocation> =
        onActive { sessionLocations(siteId, query) }

    override suspend fun events(siteId: String, query: AnalyticsQuery, pageSize: Int, beforeCursor: String?): EventsResponse =
        onActive { events(siteId, query, pageSize, beforeCursor) }

    override suspend fun eventNames(siteId: String, query: AnalyticsQuery): List<EventName> = onActive { eventNames(siteId, query) }

    override suspend fun eventProperties(siteId: String, query: AnalyticsQuery, eventName: String): List<EventPropertyCount> =
        onActive { eventProperties(siteId, query, eventName) }

    override suspend fun eventsTimeSeries(siteId: String, query: AnalyticsQuery, bucket: TimeBucket): List<EventTimePoint> =
        onActive { eventsTimeSeries(siteId, query, bucket) }

    override suspend fun errorNames(siteId: String, query: AnalyticsQuery, limit: Int): List<ErrorName> =
        onActive { errorNames(siteId, query, limit) }

    override suspend fun errorEvents(siteId: String, query: AnalyticsQuery, errorMessage: String, limit: Int): List<ErrorEvent> =
        onActive { errorEvents(siteId, query, errorMessage, limit) }

    override suspend fun errorTimeSeries(siteId: String, query: AnalyticsQuery, bucket: TimeBucket, errorMessage: String?): List<ErrorTimePoint> =
        onActive { errorTimeSeries(siteId, query, bucket, errorMessage) }

    override suspend fun goals(siteId: String, query: AnalyticsQuery, page: Int, pageSize: Int): GoalsResponse =
        onActive { goals(siteId, query, page, pageSize) }

    override suspend fun goalSessions(siteId: String, query: AnalyticsQuery, goalId: Int, page: Int, limit: Int): List<SessionSummary> =
        onActive { goalSessions(siteId, query, goalId, page, limit) }

    override suspend fun funnels(siteId: String): List<Funnel> = onActive { funnels(siteId) }

    override suspend fun analyzeFunnel(siteId: String, query: AnalyticsQuery, steps: List<FunnelStep>): List<FunnelStepResult> =
        onActive { analyzeFunnel(siteId, query, steps) }

    override suspend fun users(siteId: String, query: AnalyticsQuery, page: Int, pageSize: Int): UsersResponse =
        onActive { users(siteId, query, page, pageSize) }

    override suspend fun usersFiltered(
        siteId: String,
        query: AnalyticsQuery,
        page: Int,
        pageSize: Int,
        identifiedOnly: Boolean,
        search: String?,
    ): UsersResponse = onActive { usersFiltered(siteId, query, page, pageSize, identifiedOnly, search) }

    override suspend fun userDetail(siteId: String, userId: String): UserDetail = onActive { userDetail(siteId, userId) }

    override suspend fun retention(siteId: String, mode: String, range: Int): RetentionData =
        onActive { retention(siteId, mode, range) }

    override suspend fun performanceOverview(siteId: String, query: AnalyticsQuery): PerformanceOverview =
        onActive { performanceOverview(siteId, query) }

    override suspend fun pages(siteId: String, query: AnalyticsQuery, limit: Int): List<PageRow> =
        gated(Feature.Pages) { pages(siteId, query, limit) }

    private suspend fun activeInstanceId(): String = instances.activeInstanceId.first() ?: throw NoActiveInstanceException()

    private suspend fun <T> onActive(block: suspend RybbitApi.() -> T): T = apis.forInstance(activeInstanceId()).block()

    /** Skips the request when the feature is already known to be missing, and records it when the server says so. */
    private suspend fun <T> gated(feature: Feature, block: suspend RybbitApi.() -> T): T {
        val instanceId = activeInstanceId()
        if (!features.isAvailable(instanceId, feature)) {
            throw FeatureUnavailable(code = null, serverMessage = null, resolution = null)
        }
        return try {
            apis.forInstance(instanceId).block()
        } catch (unavailable: FeatureUnavailable) {
            features.markUnavailable(instanceId, feature)
            throw unavailable
        }
    }
}
