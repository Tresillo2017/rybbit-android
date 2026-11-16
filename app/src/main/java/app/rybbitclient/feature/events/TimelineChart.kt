package app.rybbitclient.feature.events

import app.rybbitclient.core.model.ErrorTimePoint
import app.rybbitclient.core.model.EventTimePoint
import app.rybbitclient.core.network.RybbitTimestamps
import app.rybbitclient.core.query.TimeBucket
import app.rybbitclient.core.ui.components.ChartSeries
import app.rybbitclient.core.ui.format.formatFullNumber
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Series name used for the single error line in [errorTimeline]. */
const val ERRORS_SERIES_LABEL = "Errors"

/** One plotted count: [series] is the line it belongs to, [time] the bucket start in the requested zone. */
data class TimelineCell(val series: String, val time: LocalDateTime, val count: Double)

/** Chart-ready timeline. [labels] is the shared X axis, one entry per bucket, and every series is aligned with it. */
data class TimelineChart(
    val labels: List<String>,
    val series: List<ChartSeries>,
    val summary: String,
)

/** Turns event time-series rows into cells. Rows without a name or a parseable time are dropped. */
fun eventTimeline(points: List<EventTimePoint>): List<TimelineCell> = points.mapNotNull { point ->
    val name = point.eventName ?: return@mapNotNull null
    val time = RybbitTimestamps.parseLocalOrNull(point.time) ?: return@mapNotNull null
    TimelineCell(series = name, time = time, count = (point.eventCount ?: 0).toDouble())
}

/** Turns error time-series rows into cells on the single [ERRORS_SERIES_LABEL] line. */
fun errorTimeline(points: List<ErrorTimePoint>): List<TimelineCell> = points.mapNotNull { point ->
    val time = RybbitTimestamps.parseLocalOrNull(point.time) ?: return@mapNotNull null
    TimelineCell(series = ERRORS_SERIES_LABEL, time = time, count = (point.errorCount ?: 0).toDouble())
}

/**
 * Builds aligned series from [cells]. Series are ordered by total, highest first. Missing buckets count as zero.
 * [subject] names what is counted, for the screen-reader summary, e.g. "Custom events".
 */
fun buildTimelineChart(cells: List<TimelineCell>, bucket: TimeBucket, subject: String): TimelineChart {
    val times = cells.map { it.time }.distinct().sorted()
    val totals = cells.groupBy { it.series }.mapValues { (_, rows) -> rows.sumOf { it.count } }
    val valueAt = cells.groupBy { it.series to it.time }.mapValues { (_, rows) -> rows.sumOf { it.count } }
    val series = totals.entries
        .sortedByDescending { it.value }
        .map { (name, _) ->
            ChartSeries(label = name, values = times.map { time -> valueAt[name to time] ?: 0.0 })
        }
    return TimelineChart(
        labels = times.map { formatBucketLabel(it, bucket) },
        series = series,
        summary = summarize(subject, series, totals.values.sum()),
    )
}

/** X-axis label for a bucket start: clock time for sub-day buckets, dates for longer ones. */
internal fun formatBucketLabel(time: LocalDateTime, bucket: TimeBucket): String {
    val pattern = when (bucket) {
        TimeBucket.Minute,
        TimeBucket.FiveMinutes,
        TimeBucket.TenMinutes,
        TimeBucket.FifteenMinutes,
        TimeBucket.Hour -> CLOCK_PATTERN
        TimeBucket.Day,
        TimeBucket.Week -> DAY_PATTERN
        TimeBucket.Month -> MONTH_PATTERN
    }
    return DateTimeFormatter.ofPattern(pattern, Locale.getDefault()).format(time)
}

private fun summarize(subject: String, series: List<ChartSeries>, total: Double): String {
    if (series.isEmpty()) return NO_DATA_SUMMARY
    val names = series.joinToString(separator = ", ") { it.label }
    return "$subject over time: $names. Total ${formatFullNumber(total)}."
}

private const val CLOCK_PATTERN = "HH:mm"
private const val DAY_PATTERN = "d MMM"
private const val MONTH_PATTERN = "MMM yyyy"
private const val NO_DATA_SUMMARY = "No data for this period"
