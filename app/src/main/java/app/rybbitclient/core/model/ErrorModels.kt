package app.rybbitclient.core.model

import app.rybbitclient.core.network.FlexibleStringSerializer
import app.rybbitclient.core.network.LenientDoubleSerializer
import app.rybbitclient.core.network.LenientIntSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** One distinct error message from `GET /errors/names`. Use [value] as `errorMessage` for drill-down. */
@Serializable
data class ErrorName(
    val value: String,
    val errorName: String? = null,
    @Serializable(with = LenientIntSerializer::class) val count: Int? = null,
    @Serializable(with = LenientIntSerializer::class) val sessionCount: Int? = null,
    @Serializable(with = LenientDoubleSerializer::class) val percentage: Double? = null,
)

/** One occurrence from `GET /errors/events`. */
@Serializable
data class ErrorEvent(
    val timestamp: String? = null,
    @SerialName("session_id") val sessionId: String? = null,
    @SerialName("user_id") val userId: String? = null,
    val pathname: String? = null,
    val hostname: String? = null,
    @SerialName("page_title") val pageTitle: String? = null,
    val referrer: String? = null,
    val browser: String? = null,
    @SerialName("browser_version") @Serializable(with = FlexibleStringSerializer::class) val browserVersion: String? = null,
    @SerialName("operating_system") val operatingSystem: String? = null,
    @SerialName("operating_system_version") @Serializable(with = FlexibleStringSerializer::class) val operatingSystemVersion: String? = null,
    @SerialName("device_type") val deviceType: String? = null,
    val country: String? = null,
    val region: String? = null,
    val city: String? = null,
    val message: String? = null,
    val stack: String? = null,
    val fileName: String? = null,
    @Serializable(with = LenientIntSerializer::class) val lineNumber: Int? = null,
    @Serializable(with = LenientIntSerializer::class) val columnNumber: Int? = null,
)

/** One bucket of `GET /errors/time-series`. */
@Serializable
data class ErrorTimePoint(
    val time: String,
    @SerialName("error_count") @Serializable(with = LenientIntSerializer::class) val errorCount: Int? = null,
)
