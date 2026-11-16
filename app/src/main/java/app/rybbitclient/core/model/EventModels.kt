package app.rybbitclient.core.model

import app.rybbitclient.core.network.FlexibleStringSerializer
import app.rybbitclient.core.network.LenientDoubleSerializer
import app.rybbitclient.core.network.LenientIntSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

/**
 * A row of `GET /events` (pageviews, custom events, outbound clicks). `properties` is a JSON
 * string here; decode it on demand. Timestamps are UTC.
 */
@Serializable
data class Event(
    val timestamp: String? = null,
    @SerialName("event_name") val eventName: String? = null,
    val properties: String? = null,
    @SerialName("session_id") val sessionId: String? = null,
    @SerialName("user_id") val userId: String? = null,
    @SerialName("identified_user_id") val identifiedUserId: String? = null,
    val pathname: String? = null,
    val querystring: String? = null,
    val hostname: String? = null,
    @SerialName("page_title") val pageTitle: String? = null,
    val referrer: String? = null,
    val type: String? = null,
    val browser: String? = null,
    @SerialName("browser_version") @Serializable(with = FlexibleStringSerializer::class) val browserVersion: String? = null,
    @SerialName("operating_system") val operatingSystem: String? = null,
    @SerialName("operating_system_version") @Serializable(with = FlexibleStringSerializer::class) val operatingSystemVersion: String? = null,
    val language: String? = null,
    val country: String? = null,
    val region: String? = null,
    val city: String? = null,
    @SerialName("device_type") val deviceType: String? = null,
    @SerialName("screen_width") @Serializable(with = LenientIntSerializer::class) val screenWidth: Int? = null,
    @SerialName("screen_height") @Serializable(with = LenientIntSerializer::class) val screenHeight: Int? = null,
    @Serializable(with = LenientDoubleSerializer::class) val lat: Double? = null,
    @Serializable(with = LenientDoubleSerializer::class) val lon: Double? = null,
    val traits: JsonElement? = null,
)

/**
 * Cursor block of the events list. Scroll back with [oldestCursor] as `before_cursor` while
 * [hasMore]. Polling uses [newestCursor] or [nextCursor] with `since_cursor`.
 */
@Serializable
data class EventCursor(
    val hasMore: Boolean = false,
    val oldestTimestamp: String? = null,
    val oldestCursor: String? = null,
    val newestCursor: String? = null,
    val nextCursor: String? = null,
)

/** `GET /events` response: a page of events plus its cursor. */
@Serializable
data class EventsResponse(
    val data: List<Event> = emptyList(),
    val cursor: EventCursor? = null,
)

/** One row of `GET /events/names`. */
@Serializable
data class EventName(
    val eventName: String,
    @Serializable(with = LenientIntSerializer::class) val count: Int? = null,
)

/** One row of `GET /events/properties`: a property key/value with its occurrence count. */
@Serializable
data class EventPropertyCount(
    val propertyKey: String,
    val propertyValue: String? = null,
    @Serializable(with = LenientIntSerializer::class) val count: Int? = null,
)

/** One bucket of `GET /events/time-series` for one event name. */
@Serializable
data class EventTimePoint(
    val time: String,
    @SerialName("event_name") val eventName: String? = null,
    @SerialName("event_count") @Serializable(with = LenientIntSerializer::class) val eventCount: Int? = null,
)
