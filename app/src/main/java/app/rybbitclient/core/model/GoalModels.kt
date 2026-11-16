package app.rybbitclient.core.model

import app.rybbitclient.core.network.LenientDoubleSerializer
import app.rybbitclient.core.network.LenientIntSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

/** A conversion goal. `config` varies by [goalType] (e.g. `{"eventName":...}` or `{"pathPattern":...}`). */
@Serializable
data class Goal(
    val goalId: Int,
    @Serializable(with = LenientIntSerializer::class) val siteId: Int? = null,
    val name: String,
    val goalType: String? = null,
    val config: JsonElement? = null,
    val createdAt: String? = null,
    @SerialName("total_conversions") @Serializable(with = LenientIntSerializer::class) val totalConversions: Int? = null,
    @SerialName("total_sessions") @Serializable(with = LenientIntSerializer::class) val totalSessions: Int? = null,
    @SerialName("conversion_rate") @Serializable(with = LenientDoubleSerializer::class) val conversionRate: Double? = null,
)

/** `GET /goals`: a page of goals plus `meta`. */
@Serializable
data class GoalsResponse(
    val data: List<Goal> = emptyList(),
    val meta: PageMeta? = null,
)
