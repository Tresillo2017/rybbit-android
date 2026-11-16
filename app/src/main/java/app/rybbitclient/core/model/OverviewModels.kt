package app.rybbitclient.core.model

import app.rybbitclient.core.network.LenientDoubleSerializer
import app.rybbitclient.core.network.LenientIntSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** `GET /overview` (unwrapped from `{"data":{...}}`). */
@Serializable
data class Overview(
    @Serializable(with = LenientIntSerializer::class) val sessions: Int? = null,
    @SerialName("pages_per_session") @Serializable(with = LenientDoubleSerializer::class) val pagesPerSession: Double? = null,
    @SerialName("bounce_rate") @Serializable(with = LenientDoubleSerializer::class) val bounceRate: Double? = null,
    @SerialName("session_duration") @Serializable(with = LenientDoubleSerializer::class) val sessionDuration: Double? = null,
    @Serializable(with = LenientIntSerializer::class) val pageviews: Int? = null,
    @Serializable(with = LenientIntSerializer::class) val users: Int? = null,
)

/** One bucket of `GET /overview/time-series`. [time] is wall-clock in the requested zone. */
@Serializable
data class OverviewPoint(
    val time: String,
    @Serializable(with = LenientIntSerializer::class) val sessions: Int? = null,
    @SerialName("pages_per_session") @Serializable(with = LenientDoubleSerializer::class) val pagesPerSession: Double? = null,
    @SerialName("bounce_rate") @Serializable(with = LenientDoubleSerializer::class) val bounceRate: Double? = null,
    @SerialName("session_duration") @Serializable(with = LenientDoubleSerializer::class) val sessionDuration: Double? = null,
    @Serializable(with = LenientIntSerializer::class) val pageviews: Int? = null,
    @Serializable(with = LenientIntSerializer::class) val users: Int? = null,
)

/** One row of `GET /metric`. `value` is the dimension value (page path, country, browser...). */
@Serializable
data class MetricRow(
    val value: String? = null,
    val hostname: String? = null,
    @Serializable(with = LenientIntSerializer::class) val count: Int? = null,
    @Serializable(with = LenientDoubleSerializer::class) val percentage: Double? = null,
    @Serializable(with = LenientIntSerializer::class) val pageviews: Int? = null,
    @SerialName("pageviews_percentage") @Serializable(with = LenientDoubleSerializer::class) val pageviewsPercentage: Double? = null,
    @SerialName("time_on_page_seconds") @Serializable(with = LenientDoubleSerializer::class) val timeOnPageSeconds: Double? = null,
    @SerialName("bounce_rate") @Serializable(with = LenientDoubleSerializer::class) val bounceRate: Double? = null,
)

/** Metric breakdown page, unwrapped from `{"data":{"data":[...],"totalCount":N}}`. */
@Serializable
data class MetricPage(
    val data: List<MetricRow> = emptyList(),
    @Serializable(with = LenientIntSerializer::class) val totalCount: Int? = null,
)

/** `GET /live-user-count` returns `{"count":N}` at the top level (not wrapped in `data`). */
@Serializable
data class LiveUserCount(
    @Serializable(with = LenientIntSerializer::class) val count: Int = 0,
)
