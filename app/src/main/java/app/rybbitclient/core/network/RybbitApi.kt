package app.rybbitclient.core.network

import app.rybbitclient.core.model.AnalyzeFunnelRequest
import app.rybbitclient.core.model.ApiKeyCreated
import app.rybbitclient.core.model.CreateApiKeyRequest
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
import app.rybbitclient.core.model.LiveUserCount
import app.rybbitclient.core.model.MetricPage
import app.rybbitclient.core.model.Organization
import app.rybbitclient.core.model.OrganizationSites
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
import app.rybbitclient.core.model.Envelope
import app.rybbitclient.core.query.AnalyticsQuery
import app.rybbitclient.core.query.TimeBucket
import io.ktor.client.HttpClient
import io.ktor.client.request.parameter
import io.ktor.client.request.request
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.serializer
import java.net.URLEncoder
import java.time.ZoneId

/**
 * Typed Rybbit API. One method per endpoint the app uses. Every method throws an [ApiException]
 * subtype on failure. [client] must already carry the credential (see [HttpClientFactory]).
 */
class RybbitApi(
    private val client: HttpClient,
    val baseUrl: String,
) {

    suspend fun healthCheck() {
        execute(HttpMethod.Get, "/api/health", emptyMap(), body = null)
    }

    suspend fun organizations(): List<Organization> =
        get("/api/organizations", emptyMap(), serializer<List<Organization>>())

    suspend fun orgSites(organizationId: String): OrganizationSites =
        get("/api/organizations/${seg(organizationId)}/sites", emptyMap(), serializer<OrganizationSites>())

    suspend fun site(siteId: String): Site =
        get("/api/sites/${seg(siteId)}", emptyMap(), serializer<Site>())

    suspend fun overview(siteId: String, query: AnalyticsQuery): Overview =
        data(siteId, "overview", params(query))

    suspend fun overviewTimeSeries(siteId: String, query: AnalyticsQuery, bucket: TimeBucket): List<OverviewPoint> =
        data(siteId, "overview/time-series", params(query, "bucket" to bucket.apiValue))

    suspend fun metric(
        siteId: String,
        query: AnalyticsQuery,
        parameter: String,
        limit: Int = DEFAULT_METRIC_LIMIT,
        page: Int = 1,
    ): MetricPage = data(siteId, "metric", params(query, "parameter" to parameter, "limit" to "$limit", "page" to "$page"))

    suspend fun liveUserCount(siteId: String, minutes: Int = DEFAULT_LIVE_MINUTES): LiveUserCount =
        get(siteRoute(siteId, "live-user-count"), mapOf("minutes" to "$minutes"), serializer<LiveUserCount>())

    suspend fun sessions(siteId: String, query: AnalyticsQuery, page: Int = 1, limit: Int = DEFAULT_LIMIT): List<SessionSummary> =
        data(siteId, "sessions", params(query, "page" to "$page", "limit" to "$limit"))

    suspend fun sessionDetail(siteId: String, sessionId: String): SessionDetailResponse =
        data(siteId, "sessions/${seg(sessionId)}", emptyMap())

    /** One page of a session's events (oldest first). Continue while `pagination.hasMore` is true. */
    suspend fun sessionEventsPage(siteId: String, sessionId: String, offset: Int, limit: Int): SessionDetailResponse =
        data(siteId, "sessions/${seg(sessionId)}", mapOf("offset" to "$offset", "limit" to "$limit"))

    /** Sessions of one user. `user_id` matches the identified ID, or the device ID of anonymous sessions. */
    suspend fun userSessions(siteId: String, query: AnalyticsQuery, userId: String, page: Int = 1, limit: Int = DEFAULT_LIMIT): List<SessionSummary> =
        data(siteId, "sessions", params(query, "page" to "$page", "limit" to "$limit", "user_id" to userId))

    suspend fun sessionLocations(siteId: String, query: AnalyticsQuery): List<SessionLocation> =
        data(siteId, "sessions/locations", params(query))

    /**
     * Newest first. Pass `cursor.oldestCursor` from the previous page as [beforeCursor] to scroll back. Self-hosted
     * servers may omit `oldestCursor`; then [beforeTimestamp] (legacy, whole-second, UTC) pages older rows instead.
     */
    suspend fun events(
        siteId: String,
        query: AnalyticsQuery,
        pageSize: Int = DEFAULT_LIMIT,
        beforeCursor: String? = null,
        beforeTimestamp: String? = null,
    ): EventsResponse = get(
        siteRoute(siteId, "events"),
        params(query, "page_size" to "$pageSize", "before_cursor" to beforeCursor, "before_timestamp" to beforeTimestamp),
        serializer<EventsResponse>(),
    )

    suspend fun eventNames(siteId: String, query: AnalyticsQuery): List<EventName> =
        data(siteId, "events/names", params(query))

    suspend fun eventProperties(siteId: String, query: AnalyticsQuery, eventName: String): List<EventPropertyCount> =
        data(siteId, "events/properties", params(query, EVENT_NAME_PARAMETER to eventName))

    suspend fun eventsTimeSeries(
        siteId: String,
        query: AnalyticsQuery,
        bucket: TimeBucket,
        limit: Int = DEFAULT_TOP_EVENTS,
    ): List<EventTimePoint> = data(siteId, "events/time-series", params(query, "bucket" to bucket.apiValue, "limit" to "$limit"))

    /** Returns a flat array when no `page` is sent (paged responses nest one level deeper). */
    suspend fun errorNames(siteId: String, query: AnalyticsQuery, limit: Int = DEFAULT_LIMIT): List<ErrorName> =
        data(siteId, "errors/names", params(query, "limit" to "$limit"))

    suspend fun errorEvents(
        siteId: String,
        query: AnalyticsQuery,
        errorMessage: String,
        limit: Int = DEFAULT_LIMIT,
    ): List<ErrorEvent> = data(siteId, "errors/events", params(query, "errorMessage" to errorMessage, "limit" to "$limit"))

    suspend fun errorTimeSeries(
        siteId: String,
        query: AnalyticsQuery,
        bucket: TimeBucket,
        errorMessage: String,
    ): List<ErrorTimePoint> = data(siteId, "errors/time-series", params(query, "bucket" to bucket.apiValue, "errorMessage" to errorMessage))

    suspend fun goals(siteId: String, query: AnalyticsQuery, page: Int = 1, pageSize: Int = DEFAULT_GOALS_PAGE_SIZE): GoalsResponse =
        get(siteRoute(siteId, "goals"), params(query, "page" to "$page", "page_size" to "$pageSize"), serializer<GoalsResponse>())

    suspend fun goalSessions(
        siteId: String,
        query: AnalyticsQuery,
        goalId: Int,
        page: Int = 1,
        limit: Int = DEFAULT_LIMIT,
    ): List<SessionSummary> = data(siteId, "goals/${seg("$goalId")}/sessions", params(query, "page" to "$page", "limit" to "$limit"))

    suspend fun funnels(siteId: String): List<Funnel> = data(siteId, "funnels", emptyMap())

    suspend fun analyzeFunnel(siteId: String, query: AnalyticsQuery, steps: List<FunnelStep>): List<FunnelStepResult> {
        val body = RybbitJson.json.encodeToString(AnalyzeFunnelRequest.serializer(), AnalyzeFunnelRequest(steps))
        val text = execute(HttpMethod.Post, siteRoute(siteId, "funnels/analyze"), params(query), body)
        return decode(Envelope.serializer(serializer<List<FunnelStepResult>>()), text).data
    }

    suspend fun users(siteId: String, query: AnalyticsQuery, page: Int = 1, pageSize: Int = DEFAULT_USERS_PAGE_SIZE): UsersResponse =
        get(siteRoute(siteId, "users"), params(query, "page" to "$page", "page_size" to "$pageSize"), serializer<UsersResponse>())

    /** Users list with the optional search (matched on the listed user ID) and identified-only filters. */
    suspend fun usersFiltered(
        siteId: String,
        query: AnalyticsQuery,
        page: Int,
        pageSize: Int,
        identifiedOnly: Boolean,
        search: String?,
    ): UsersResponse = get(
        siteRoute(siteId, "users"),
        params(
            query,
            "page" to "$page",
            "page_size" to "$pageSize",
            "identified_only" to if (identifiedOnly) "true" else null,
            "search" to search,
            "search_field" to search?.let { "user_id" },
        ),
        serializer<UsersResponse>(),
    )

    suspend fun userDetail(siteId: String, userId: String): UserDetail = data(siteId, "users/${seg(userId)}", emptyMap())

    /** Cohorts are bucketed in [timeZone] (IANA id), so weeks and days match the device's calendar. */
    suspend fun retention(
        siteId: String,
        mode: String = "day",
        range: Int = DEFAULT_RETENTION_RANGE,
        timeZone: String = ZoneId.systemDefault().id,
    ): RetentionData = data(siteId, "retention", mapOf("mode" to mode, "range" to "$range", "time_zone" to timeZone))

    suspend fun performanceOverview(siteId: String, query: AnalyticsQuery): PerformanceOverview =
        data(siteId, "performance/overview", params(query))

    /** Cloud-only. Self-hosted instances answer 404 `API_ROUTE_NOT_FOUND` (mapped to [FeatureUnavailable]). */
    suspend fun pages(siteId: String, query: AnalyticsQuery, limit: Int = DEFAULT_LIMIT): List<PageRow> =
        data(siteId, "pages", params(query, "limit" to "$limit"))

    /** Session-auth only: creates an API key for the signed-in user. */
    suspend fun createApiKey(name: String): ApiKeyCreated {
        val body = RybbitJson.json.encodeToString(CreateApiKeyRequest.serializer(), CreateApiKeyRequest(name))
        val text = execute(HttpMethod.Post, "/api/user/api-keys", emptyMap(), body)
        return decode(ApiKeyCreated.serializer(), text)
    }

    private suspend inline fun <reified T> data(
        siteId: String,
        route: String,
        params: Map<String, String>,
    ): T = get(siteRoute(siteId, route), params, Envelope.serializer(serializer<T>())).data

    private suspend fun <T> get(path: String, params: Map<String, String>, serializer: KSerializer<T>): T =
        decode(serializer, execute(HttpMethod.Get, path, params, body = null))

    private suspend fun execute(
        method: HttpMethod,
        path: String,
        params: Map<String, String>,
        body: String?,
    ): String {
        val response = send(method, path, params, body)
        val text = readBody(response)
        if (!response.status.isSuccess()) {
            throw ApiErrorMapper.map(response.status.value, text, response.headers[HttpHeaders.RetryAfter])
        }
        return text
    }

    private suspend fun send(method: HttpMethod, path: String, params: Map<String, String>, body: String?): HttpResponse =
        try {
            client.request("$baseUrl$path") {
                this.method = method
                params.forEach { (key, value) -> parameter(key, value) }
                if (body != null) {
                    contentType(ContentType.Application.Json)
                    setBody(body)
                }
            }
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (failure: Exception) {
            throw Network(failure)
        }

    private suspend fun readBody(response: HttpResponse): String = try {
        response.bodyAsText()
    } catch (cancellation: CancellationException) {
        throw cancellation
    } catch (failure: Exception) {
        throw Network(failure)
    }

    private fun <T> decode(serializer: KSerializer<T>, text: String): T = try {
        RybbitJson.json.decodeFromString(serializer, text)
    } catch (failure: SerializationException) {
        throw InvalidResponse(failure)
    } catch (failure: IllegalArgumentException) {
        throw InvalidResponse(failure)
    }

    private fun siteRoute(siteId: String, route: String): String = "/api/sites/${seg(siteId)}/$route"

    private fun params(query: AnalyticsQuery, vararg extra: Pair<String, String?>): Map<String, String> =
        query.toParams() + extra.mapNotNull { (key, value) -> value?.let { key to it } }

    private fun seg(value: String): String = URLEncoder.encode(value, Charsets.UTF_8.name()).replace("+", "%20")

    private companion object {
        const val DEFAULT_LIMIT = 50
        const val DEFAULT_METRIC_LIMIT = 100
        const val DEFAULT_TOP_EVENTS = 5
        const val DEFAULT_LIVE_MINUTES = 5
        const val DEFAULT_GOALS_PAGE_SIZE = 10
        const val DEFAULT_USERS_PAGE_SIZE = 100
        const val DEFAULT_RETENTION_RANGE = 30
        const val EVENT_NAME_PARAMETER = "event_name"
    }
}
