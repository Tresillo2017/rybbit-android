package app.rybbitclient.core.model

import app.rybbitclient.core.network.FlexibleStringSerializer
import app.rybbitclient.core.network.LenientDoubleSerializer
import app.rybbitclient.core.network.LenientIntSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject

/**
 * A session as listed by `/sessions` and `/goals/{id}/sessions`. Timestamps are UTC strings;
 * parse with `RybbitTimestamps.parseUtc`.
 */
@Serializable
data class SessionSummary(
    @SerialName("session_id") val sessionId: String,
    @SerialName("user_id") val userId: String? = null,
    @SerialName("identified_user_id") val identifiedUserId: String? = null,
    val country: String? = null,
    val region: String? = null,
    val city: String? = null,
    val language: String? = null,
    @SerialName("device_type") val deviceType: String? = null,
    val browser: String? = null,
    @SerialName("browser_version") @Serializable(with = FlexibleStringSerializer::class) val browserVersion: String? = null,
    @SerialName("operating_system") val operatingSystem: String? = null,
    @SerialName("operating_system_version") @Serializable(with = FlexibleStringSerializer::class) val operatingSystemVersion: String? = null,
    @SerialName("screen_width") @Serializable(with = LenientIntSerializer::class) val screenWidth: Int? = null,
    @SerialName("screen_height") @Serializable(with = LenientIntSerializer::class) val screenHeight: Int? = null,
    val referrer: String? = null,
    val channel: String? = null,
    val hostname: String? = null,
    @SerialName("page_title") val pageTitle: String? = null,
    val querystring: String? = null,
    @SerialName("entry_page") val entryPage: String? = null,
    @SerialName("exit_page") val exitPage: String? = null,
    @SerialName("session_start") val sessionStart: String? = null,
    @SerialName("session_end") val sessionEnd: String? = null,
    @SerialName("session_duration") @Serializable(with = LenientDoubleSerializer::class) val sessionDuration: Double? = null,
    @Serializable(with = LenientIntSerializer::class) val pageviews: Int? = null,
    @Serializable(with = LenientIntSerializer::class) val events: Int? = null,
    @Serializable(with = LenientIntSerializer::class) val errors: Int? = null,
    val ip: String? = null,
    @Serializable(with = LenientDoubleSerializer::class) val lat: Double? = null,
    @Serializable(with = LenientDoubleSerializer::class) val lon: Double? = null,
    val timezone: String? = null,
    val tag: String? = null,
    val traits: JsonElement? = null,
    @SerialName("utm_source") val utmSource: String? = null,
    @SerialName("utm_medium") val utmMedium: String? = null,
    @SerialName("utm_campaign") val utmCampaign: String? = null,
    @SerialName("utm_content") val utmContent: String? = null,
    @SerialName("utm_term") val utmTerm: String? = null,
)

/** `GET /sessions/{id}` `data` object: the session, its events and pagination. */
@Serializable
data class SessionDetailResponse(
    val session: SessionDetail? = null,
    val events: List<SessionDetailEvent> = emptyList(),
    val pagination: SessionPagination? = null,
)

/** The session header inside a session detail response. */
@Serializable
data class SessionDetail(
    @SerialName("session_id") val sessionId: String,
    @SerialName("user_id") val userId: String? = null,
    val country: String? = null,
    val region: String? = null,
    val city: String? = null,
    val language: String? = null,
    @SerialName("device_type") val deviceType: String? = null,
    val browser: String? = null,
    @SerialName("browser_version") @Serializable(with = FlexibleStringSerializer::class) val browserVersion: String? = null,
    @SerialName("operating_system") val operatingSystem: String? = null,
    @SerialName("operating_system_version") @Serializable(with = FlexibleStringSerializer::class) val operatingSystemVersion: String? = null,
    @SerialName("screen_width") @Serializable(with = LenientIntSerializer::class) val screenWidth: Int? = null,
    @SerialName("screen_height") @Serializable(with = LenientIntSerializer::class) val screenHeight: Int? = null,
    val referrer: String? = null,
    val channel: String? = null,
    @SerialName("session_start") val sessionStart: String? = null,
    @SerialName("session_end") val sessionEnd: String? = null,
    @SerialName("session_duration") @Serializable(with = LenientDoubleSerializer::class) val sessionDuration: Double? = null,
    @Serializable(with = LenientIntSerializer::class) val pageviews: Int? = null,
    @Serializable(with = LenientIntSerializer::class) val events: Int? = null,
    @SerialName("entry_page") val entryPage: String? = null,
    @SerialName("exit_page") val exitPage: String? = null,
    val ip: String? = null,
)

/** One event inside a session detail. `props` is an object here (unlike the events list, which uses a string). */
@Serializable
data class SessionDetailEvent(
    val timestamp: String? = null,
    val pathname: String? = null,
    val hostname: String? = null,
    val querystring: String? = null,
    @SerialName("page_title") val pageTitle: String? = null,
    val referrer: String? = null,
    val type: String? = null,
    @SerialName("event_name") val eventName: String? = null,
    val props: JsonObject? = null,
)

/** Pagination block of a session detail response. */
@Serializable
data class SessionPagination(
    @Serializable(with = LenientIntSerializer::class) val total: Int? = null,
    @Serializable(with = LenientIntSerializer::class) val limit: Int? = null,
    @Serializable(with = LenientIntSerializer::class) val offset: Int? = null,
    val hasMore: Boolean? = null,
)

/** One row of `GET /sessions/locations`. */
@Serializable
data class SessionLocation(
    @Serializable(with = LenientDoubleSerializer::class) val lat: Double? = null,
    @Serializable(with = LenientDoubleSerializer::class) val lon: Double? = null,
    val city: String? = null,
    val country: String? = null,
    @Serializable(with = LenientIntSerializer::class) val count: Int? = null,
)
