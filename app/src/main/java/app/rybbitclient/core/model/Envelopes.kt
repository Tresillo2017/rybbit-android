package app.rybbitclient.core.model

import app.rybbitclient.core.network.LenientIntSerializer
import kotlinx.serialization.Serializable

/** The common `{"data": ...}` wrapper. Generic so each endpoint decodes its payload type. */
@Serializable
data class Envelope<T>(val data: T)

/** Page metadata used by list endpoints such as goals: `{"meta":{"total":"2","page":1,...}}`. */
@Serializable
data class PageMeta(
    @Serializable(with = LenientIntSerializer::class) val total: Int? = null,
    @Serializable(with = LenientIntSerializer::class) val page: Int? = null,
    @Serializable(with = LenientIntSerializer::class) val pageSize: Int? = null,
    @Serializable(with = LenientIntSerializer::class) val totalPages: Int? = null,
)
