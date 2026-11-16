package app.rybbitclient.core.model

import app.rybbitclient.core.network.FlexibleStringSerializer
import app.rybbitclient.core.network.LenientIntSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

/** A row of `GET /users`. Paged with `totalCount`, `page`, `pageSize` at the top level. */
@Serializable
data class UserSummary(
    @SerialName("user_id") val userId: String,
    @SerialName("effective_user_id") val effectiveUserId: String? = null,
    @SerialName("identified_user_id") val identifiedUserId: String? = null,
    val country: String? = null,
    val region: String? = null,
    val city: String? = null,
    val language: String? = null,
    val browser: String? = null,
    @SerialName("browser_version") @Serializable(with = FlexibleStringSerializer::class) val browserVersion: String? = null,
    @SerialName("operating_system") val operatingSystem: String? = null,
    @SerialName("operating_system_version") @Serializable(with = FlexibleStringSerializer::class) val operatingSystemVersion: String? = null,
    @SerialName("device_type") val deviceType: String? = null,
    @SerialName("screen_width") @Serializable(with = LenientIntSerializer::class) val screenWidth: Int? = null,
    @SerialName("screen_height") @Serializable(with = LenientIntSerializer::class) val screenHeight: Int? = null,
    val referrer: String? = null,
    val channel: String? = null,
    val hostname: String? = null,
    @Serializable(with = LenientIntSerializer::class) val sessions: Int? = null,
    @Serializable(with = LenientIntSerializer::class) val pageviews: Int? = null,
    @Serializable(with = LenientIntSerializer::class) val events: Int? = null,
    @SerialName("first_seen") val firstSeen: String? = null,
    @SerialName("last_seen") val lastSeen: String? = null,
    val tag: String? = null,
    val traits: JsonElement? = null,
)

/** `GET /users` response. */
@Serializable
data class UsersResponse(
    val data: List<UserSummary> = emptyList(),
    @Serializable(with = LenientIntSerializer::class) val totalCount: Int? = null,
    @Serializable(with = LenientIntSerializer::class) val page: Int? = null,
    @Serializable(with = LenientIntSerializer::class) val pageSize: Int? = null,
)

/** Where a user has been seen, from `GET /users/{id}`. */
@Serializable
data class UserLocation(
    val country: String? = null,
    val region: String? = null,
    val city: String? = null,
    @Serializable(with = LenientIntSerializer::class) val sessions: Int? = null,
    @SerialName("last_seen") val lastSeen: String? = null,
)

/** A device a user has been seen on, from `GET /users/{id}`. */
@Serializable
data class UserDevice(
    @SerialName("device_type") val deviceType: String? = null,
    val browser: String? = null,
    @SerialName("operating_system") val operatingSystem: String? = null,
    @SerialName("browser_version") @Serializable(with = FlexibleStringSerializer::class) val browserVersion: String? = null,
    @SerialName("operating_system_version") @Serializable(with = FlexibleStringSerializer::class) val operatingSystemVersion: String? = null,
    @SerialName("screen_width") @Serializable(with = LenientIntSerializer::class) val screenWidth: Int? = null,
    @SerialName("screen_height") @Serializable(with = LenientIntSerializer::class) val screenHeight: Int? = null,
    @Serializable(with = LenientIntSerializer::class) val sessions: Int? = null,
    @SerialName("last_seen") val lastSeen: String? = null,
)

/** `GET /users/{id}` `data` object. */
@Serializable
data class UserDetail(
    @Serializable(with = LenientIntSerializer::class) val sessions: Int? = null,
    @Serializable(with = LenientIntSerializer::class) val duration: Int? = null,
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
    @SerialName("first_seen") val firstSeen: String? = null,
    @SerialName("last_seen") val lastSeen: String? = null,
    @Serializable(with = LenientIntSerializer::class) val pageviews: Int? = null,
    @Serializable(with = LenientIntSerializer::class) val events: Int? = null,
    val timezone: String? = null,
    @SerialName("first_referrer") val firstReferrer: String? = null,
    @SerialName("first_channel") val firstChannel: String? = null,
    @SerialName("first_entry_page") val firstEntryPage: String? = null,
    @SerialName("last_referrer") val lastReferrer: String? = null,
    @SerialName("last_channel") val lastChannel: String? = null,
    val traits: JsonElement? = null,
    @SerialName("linked_devices") val linkedDevices: JsonElement? = null,
    val vitals: JsonElement? = null,
    val locations: List<UserLocation> = emptyList(),
    val devices: List<UserDevice> = emptyList(),
)
