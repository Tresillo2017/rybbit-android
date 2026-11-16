package app.rybbitclient.feature.performance

import app.rybbitclient.core.model.PerformanceOverview
import app.rybbitclient.core.ui.format.EMPTY_VALUE
import java.util.Locale
import kotlin.math.roundToInt

private const val MS_PER_SECOND = 1000.0
private const val SCORE_FORMAT = "%.3f"
private const val SECONDS_FORMAT = "%.2f s"
private const val MILLISECONDS_FORMAT = "%d ms"

/** How a metric is written: milliseconds for timings, a unitless score for CLS. */
enum class VitalUnit { Milliseconds, Score }

/**
 * The Core Web Vitals and the TTFB and FCP timings Rybbit reports. Thresholds are the published
 * "good" and "poor" boundaries: at or below [good] is good, above [poor] is poor, in between needs work.
 */
enum class VitalMetric(
    val shortName: String,
    val title: String,
    val description: String,
    val unit: VitalUnit,
    private val good: Double,
    private val poor: Double,
) {
    Lcp("LCP", "Largest Contentful Paint", "How fast the main content loads", VitalUnit.Milliseconds, 2500.0, 4000.0),
    Inp("INP", "Interaction to Next Paint", "How quickly the page responds to taps and clicks", VitalUnit.Milliseconds, 200.0, 500.0),
    Cls("CLS", "Cumulative Layout Shift", "How much content moves while the page loads", VitalUnit.Score, 0.1, 0.25),
    Fcp("FCP", "First Contentful Paint", "When the first content appears", VitalUnit.Milliseconds, 1800.0, 3000.0),
    Ttfb("TTFB", "Time to First Byte", "How fast the server starts answering", VitalUnit.Milliseconds, 800.0, 1800.0),
    ;

    fun rate(value: Double): VitalRating = when {
        value <= good -> VitalRating.Good
        value > poor -> VitalRating.Poor
        else -> VitalRating.NeedsImprovement
    }
}

enum class VitalRating(val label: String) {
    Good("Good"),
    NeedsImprovement("Needs improvement"),
    Poor("Poor"),
}

/** One metric's percentiles, in the metric's own units. Any percentile may be null. */
data class VitalReading(
    val metric: VitalMetric,
    val p50: Double?,
    val p75: Double?,
    val p90: Double?,
    val p99: Double?,
) {
    /** Rating from the 75th percentile, the value the published thresholds are defined on. */
    val rating: VitalRating? get() = p75?.let(metric::rate)

    fun ratingOf(value: Double?): VitalRating? = value?.let(metric::rate)
}

/** One reading per metric, in display order, taken from the overview's percentile fields. */
fun vitalReadings(overview: PerformanceOverview): List<VitalReading> = listOf(
    VitalReading(VitalMetric.Lcp, overview.lcpP50, overview.lcpP75, overview.lcpP90, overview.lcpP99),
    VitalReading(VitalMetric.Inp, overview.inpP50, overview.inpP75, overview.inpP90, overview.inpP99),
    VitalReading(VitalMetric.Cls, overview.clsP50, overview.clsP75, overview.clsP90, overview.clsP99),
    VitalReading(VitalMetric.Fcp, overview.fcpP50, overview.fcpP75, overview.fcpP90, overview.fcpP99),
    VitalReading(VitalMetric.Ttfb, overview.ttfbP50, overview.ttfbP75, overview.ttfbP90, overview.ttfbP99),
)

/** True when the server sent no percentile at all. Self-hosted builds and sites without data look like this. */
fun hasNoVitalValues(overview: PerformanceOverview): Boolean =
    vitalReadings(overview).all { reading ->
        reading.p50 == null && reading.p75 == null && reading.p90 == null && reading.p99 == null
    }

/** A value with its unit: "870 ms", "2.50 s" (at 1000 ms or more) or "0.084" for CLS. */
fun formatVitalValue(metric: VitalMetric, value: Double?, locale: Locale = Locale.getDefault()): String {
    if (value == null || !value.isFinite()) return EMPTY_VALUE
    return when (metric.unit) {
        VitalUnit.Score -> String.format(locale, SCORE_FORMAT, value)
        VitalUnit.Milliseconds -> if (value >= MS_PER_SECOND) {
            String.format(locale, SECONDS_FORMAT, value / MS_PER_SECOND)
        } else {
            String.format(locale, MILLISECONDS_FORMAT, value.roundToInt())
        }
    }
}
