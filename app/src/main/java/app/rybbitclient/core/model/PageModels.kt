package app.rybbitclient.core.model

import app.rybbitclient.core.network.LenientDoubleSerializer
import app.rybbitclient.core.network.LenientIntSerializer
import kotlinx.serialization.Serializable

/**
 * One row of the cloud-only `GET /pages` report. No fixture exists for this endpoint (self-hosted
 * returns `API_ROUTE_NOT_FOUND`), so every field is nullable and unknown keys are ignored.
 */
@Serializable
data class PageRow(
    val value: String? = null,
    @Serializable(with = LenientIntSerializer::class) val count: Int? = null,
    @Serializable(with = LenientIntSerializer::class) val pageviews: Int? = null,
    @Serializable(with = LenientIntSerializer::class) val entries: Int? = null,
    @Serializable(with = LenientIntSerializer::class) val exits: Int? = null,
    @Serializable(with = LenientDoubleSerializer::class) val timeOnPageSeconds: Double? = null,
    @Serializable(with = LenientDoubleSerializer::class) val bounceRate: Double? = null,
)
