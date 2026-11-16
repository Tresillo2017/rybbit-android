package app.rybbitclient.core.model

import app.rybbitclient.core.network.LenientDoubleSerializer
import app.rybbitclient.core.network.LenientIntSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

/** A funnel step. Used both in saved funnels and in the `POST /funnels/analyze` request body. */
@Serializable
data class FunnelStep(
    val type: String,
    val value: String,
    val name: String? = null,
)

/** A saved funnel from `GET /funnels`. */
@Serializable
data class Funnel(
    val id: Int,
    val name: String,
    val steps: List<FunnelStep> = emptyList(),
    val configuration: JsonElement? = null,
    val createdAt: String? = null,
    val updatedAt: String? = null,
    @Serializable(with = LenientDoubleSerializer::class) val conversionRate: Double? = null,
    @Serializable(with = LenientIntSerializer::class) val totalVisitors: Int? = null,
)

/** One step of an analyzed funnel. */
@Serializable
data class FunnelStepResult(
    @SerialName("step_number") @Serializable(with = LenientIntSerializer::class) val stepNumber: Int? = null,
    @SerialName("step_name") val stepName: String? = null,
    @Serializable(with = LenientIntSerializer::class) val sessions: Int? = null,
    @SerialName("conversion_rate") @Serializable(with = LenientDoubleSerializer::class) val conversionRate: Double? = null,
    @SerialName("dropoff_rate") @Serializable(with = LenientDoubleSerializer::class) val dropoffRate: Double? = null,
    @Serializable(with = LenientIntSerializer::class) val visitors: Int? = null,
)

/** Request body for `POST /funnels/analyze`. */
@Serializable
data class AnalyzeFunnelRequest(val steps: List<FunnelStep>)
