package app.rybbitclient.core.model

import app.rybbitclient.core.network.LenientIntSerializer
import kotlinx.serialization.Serializable

/**
 * One cohort of retention: its size and the retained percentage per period.
 * A `null` entry is a period that has not happened yet (after the end of the range), not a zero.
 */
@Serializable
data class RetentionCohort(
    @Serializable(with = LenientIntSerializer::class) val size: Int? = null,
    val percentages: List<Double?> = emptyList(),
)

/** `GET /retention` `data` object. [cohorts] is keyed by the cohort start date. */
@Serializable
data class RetentionData(
    val cohorts: Map<String, RetentionCohort> = emptyMap(),
    @Serializable(with = LenientIntSerializer::class) val maxPeriods: Int? = null,
    val mode: String? = null,
    @Serializable(with = LenientIntSerializer::class) val range: Int? = null,
)
