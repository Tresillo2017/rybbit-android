package app.rybbitclient.feature.overview

import app.rybbitclient.core.model.Overview
import app.rybbitclient.core.model.OverviewPoint
import app.rybbitclient.core.ui.components.ChartSeries
import app.rybbitclient.core.ui.components.KpiItem
import app.rybbitclient.core.ui.format.EMPTY_VALUE
import app.rybbitclient.core.ui.format.formatBounceRate
import app.rybbitclient.core.ui.format.formatCompactNumber
import app.rybbitclient.core.ui.format.formatDuration
import java.util.Locale

private const val PERCENT = 100.0
private const val PREVIOUS_SERIES_LABEL = "Previous period"

/** The six headline metrics. [isInverse] means lower is better (bounce rate). */
enum class KpiMetric(val label: String, val isInverse: Boolean) {
    Visitors("Unique visitors", false),
    Sessions("Sessions", false),
    Pageviews("Pageviews", false),
    PagesPerSession("Pages / session", false),
    BounceRate("Bounce rate", true),
    SessionDuration("Session duration", false);

    fun valueIn(overview: Overview): Double? = when (this) {
        Visitors -> overview.users?.toDouble()
        Sessions -> overview.sessions?.toDouble()
        Pageviews -> overview.pageviews?.toDouble()
        PagesPerSession -> overview.pagesPerSession
        BounceRate -> overview.bounceRate
        SessionDuration -> overview.sessionDuration
    }

    fun valueIn(point: OverviewPoint): Double? = when (this) {
        Visitors -> point.users?.toDouble()
        Sessions -> point.sessions?.toDouble()
        Pageviews -> point.pageviews?.toDouble()
        PagesPerSession -> point.pagesPerSession
        BounceRate -> point.bounceRate
        SessionDuration -> point.sessionDuration
    }

    /** Display text for a value of this metric. Missing values read as an em dash. */
    fun format(value: Double?): String = when {
        value == null || !value.isFinite() -> EMPTY_VALUE
        this == PagesPerSession -> String.format(Locale.getDefault(), "%.1f", value)
        this == BounceRate -> formatBounceRate(value)
        this == SessionDuration -> formatDuration(value)
        else -> formatCompactNumber(value)
    }
}

/**
 * Percent change from [previous] to [current]. Null when either side is missing or the previous
 * value is zero, because a change from zero has no meaningful percentage.
 */
fun deltaPercent(current: Double?, previous: Double?): Double? {
    if (current == null || previous == null || previous == 0.0) return null
    return ((current - previous) / previous * PERCENT).takeIf { it.isFinite() }
}

/** One tile per [KpiMetric], in display order. The item key is the enum name, used for selection. */
fun kpiItems(data: KpiData): List<KpiItem> = KpiMetric.entries.map { metric ->
    val current = metric.valueIn(data.current)
    val previous = data.previous?.let { metric.valueIn(it) }
    KpiItem(
        key = metric.name,
        label = metric.label,
        value = metric.format(current),
        deltaPercent = deltaPercent(current, previous),
        isInverse = metric.isInverse,
    )
}

/** Chart series for the selected metric: the current period, plus a dashed previous period when present. */
fun chartSeries(trend: TrendData, metric: KpiMetric): List<ChartSeries> = buildList {
    add(ChartSeries(label = metric.label, values = trend.current.map { metric.valueIn(it) ?: 0.0 }))
    if (trend.previous.isNotEmpty()) {
        add(
            ChartSeries(
                label = PREVIOUS_SERIES_LABEL,
                values = trend.previous.map { metric.valueIn(it) ?: 0.0 },
                isPrevious = true,
            ),
        )
    }
}
