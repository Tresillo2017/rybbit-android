package app.rybbitclient.core.model

import app.rybbitclient.core.network.LenientDoubleSerializer
import app.rybbitclient.core.network.LenientIntSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * `GET /performance/overview` `data` object. Web vitals are null on self-hosted builds.
 * Percentile values are in the metric's own units (ms for LCP/INP/FCP/TTFB, unitless for CLS).
 */
@Serializable
data class PerformanceOverview(
    @SerialName("lcp_p50") @Serializable(with = LenientDoubleSerializer::class) val lcpP50: Double? = null,
    @SerialName("lcp_p75") @Serializable(with = LenientDoubleSerializer::class) val lcpP75: Double? = null,
    @SerialName("lcp_p90") @Serializable(with = LenientDoubleSerializer::class) val lcpP90: Double? = null,
    @SerialName("lcp_p99") @Serializable(with = LenientDoubleSerializer::class) val lcpP99: Double? = null,
    @SerialName("cls_p50") @Serializable(with = LenientDoubleSerializer::class) val clsP50: Double? = null,
    @SerialName("cls_p75") @Serializable(with = LenientDoubleSerializer::class) val clsP75: Double? = null,
    @SerialName("cls_p90") @Serializable(with = LenientDoubleSerializer::class) val clsP90: Double? = null,
    @SerialName("cls_p99") @Serializable(with = LenientDoubleSerializer::class) val clsP99: Double? = null,
    @SerialName("inp_p50") @Serializable(with = LenientDoubleSerializer::class) val inpP50: Double? = null,
    @SerialName("inp_p75") @Serializable(with = LenientDoubleSerializer::class) val inpP75: Double? = null,
    @SerialName("inp_p90") @Serializable(with = LenientDoubleSerializer::class) val inpP90: Double? = null,
    @SerialName("inp_p99") @Serializable(with = LenientDoubleSerializer::class) val inpP99: Double? = null,
    @SerialName("fcp_p50") @Serializable(with = LenientDoubleSerializer::class) val fcpP50: Double? = null,
    @SerialName("fcp_p75") @Serializable(with = LenientDoubleSerializer::class) val fcpP75: Double? = null,
    @SerialName("fcp_p90") @Serializable(with = LenientDoubleSerializer::class) val fcpP90: Double? = null,
    @SerialName("fcp_p99") @Serializable(with = LenientDoubleSerializer::class) val fcpP99: Double? = null,
    @SerialName("ttfb_p50") @Serializable(with = LenientDoubleSerializer::class) val ttfbP50: Double? = null,
    @SerialName("ttfb_p75") @Serializable(with = LenientDoubleSerializer::class) val ttfbP75: Double? = null,
    @SerialName("ttfb_p90") @Serializable(with = LenientDoubleSerializer::class) val ttfbP90: Double? = null,
    @SerialName("ttfb_p99") @Serializable(with = LenientDoubleSerializer::class) val ttfbP99: Double? = null,
    @SerialName("total_performance_events") @Serializable(with = LenientIntSerializer::class) val totalPerformanceEvents: Int? = null,
)
