package app.rybbitclient.feature.overview

import app.rybbitclient.core.model.MetricPage
import app.rybbitclient.core.model.MetricRow
import app.rybbitclient.core.network.RybbitTimestamps
import app.rybbitclient.core.query.DateRange
import app.rybbitclient.core.query.DateRangeResolver
import app.rybbitclient.core.query.Filter
import app.rybbitclient.core.query.FilterType
import app.rybbitclient.core.query.ResolvedRange
import app.rybbitclient.core.query.TimeBucket
import app.rybbitclient.core.ui.components.BarItem
import app.rybbitclient.core.ui.format.countryLabel
import app.rybbitclient.core.ui.format.formatFullNumber
import app.rybbitclient.core.ui.format.formatPercent
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale

/** A date-range preset shown in the range bar. [key] is stable so the bar can highlight it. */
data class RangePreset(val key: String, val label: String, val range: DateRange)

val RANGE_PRESETS: List<RangePreset> = listOf(
    RangePreset("today", "Today", DateRange.Today),
    RangePreset("yesterday", "Yesterday", DateRange.Yesterday),
    RangePreset("24h", "24h", DateRange.Last24Hours),
    RangePreset("7d", "7d", DateRange.Last7Days),
    RangePreset("30d", "30d", DateRange.Last30Days),
    RangePreset("90d", "90d", DateRange.Last90Days),
    RangePreset("this_month", "This month", DateRange.ThisMonth),
    RangePreset("last_month", "Last month", DateRange.LastMonth),
    RangePreset("this_year", "This year", DateRange.ThisYear),
    RangePreset("all", "All time", DateRange.AllTime),
)

fun presetKeyFor(range: DateRange): String? = RANGE_PRESETS.firstOrNull { it.range == range }?.key

fun rangeForPreset(key: String): DateRange? = RANGE_PRESETS.firstOrNull { it.key == key }?.range

/** The custom range picker reports UTC-midnight millis. Reversed input is swapped instead of rejected. */
fun customRangeFromMillis(startMillis: Long, endMillis: Long): DateRange.Custom {
    val start = utcDate(startMillis)
    val end = utcDate(endMillis)
    return if (end.isBefore(start)) DateRange.Custom(end, start) else DateRange.Custom(start, end)
}

/** UTC-midnight millis for [date], the form the date picker expects. */
fun millisOf(date: LocalDate): Long = date.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()

/** The equal-length window before [range] as a date range. Null when there is none (all time, rolling windows). */
fun previousRangeOf(range: DateRange, clock: Clock, zone: ZoneId): DateRange.Custom? {
    val previous = DateRangeResolver(clock, zone).previousPeriod(range) as? ResolvedRange.Dates ?: return null
    return DateRange.Custom(previous.start, previous.end)
}

/** Chip text for a custom range, e.g. "3 Nov - 9 Nov". */
fun customRangeLabel(range: DateRange.Custom, locale: Locale = Locale.getDefault()): String {
    val formatter = DateTimeFormatter.ofPattern("d MMM", locale)
    return "${range.start.format(formatter)} - ${range.end.format(formatter)}"
}

/** Axis text for each point of [trend]: time for hourly buckets, date for daily and longer. */
fun axisLabels(bucket: TimeBucket, times: List<String>, locale: Locale = Locale.getDefault()): List<String> {
    val formatter = DateTimeFormatter.ofPattern(axisPattern(bucket), locale)
    return times.map { time -> RybbitTimestamps.parseLocalOrNull(time)?.format(formatter) ?: time }
}

private fun axisPattern(bucket: TimeBucket): String = when (bucket) {
    TimeBucket.Minute, TimeBucket.FiveMinutes, TimeBucket.TenMinutes, TimeBucket.FifteenMinutes, TimeBucket.Hour -> "HH:mm"
    TimeBucket.Day, TimeBucket.Week -> "MMM d"
    TimeBucket.Month -> "MMM yyyy"
}

private fun utcDate(millis: Long): LocalDate = Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate()

private const val REFERRER = "referrer"
private const val COUNTRY = "country"
private const val UNKNOWN_LABEL = "Unknown"
private const val DIRECT_LABEL = "Direct / none"

/** Display text for a breakdown value. Blank values get a readable placeholder; countries get flag and name. */
fun displayValue(parameter: String, raw: String?): String = when {
    raw.isNullOrBlank() && parameter == REFERRER -> DIRECT_LABEL
    raw.isNullOrBlank() -> UNKNOWN_LABEL
    parameter == COUNTRY -> countryLabel(raw)
    else -> raw
}

/** Ranked bars for a breakdown tab. The item key is the raw server value, so tapping it can build a filter. */
fun barItemsFor(parameter: String, rows: List<MetricRow>): List<BarItem> = rows.map { row ->
    val count = (row.count ?: 0).toDouble()
    BarItem(
        key = row.value.orEmpty(),
        label = displayValue(parameter, row.value),
        value = count,
        valueText = formatFullNumber(count),
        percentText = row.percentage?.let { formatPercent(it) },
    )
}

/** Human name of a filtered parameter, used in the filter chips. */
fun parameterLabel(parameter: String): String = when (parameter) {
    "pathname" -> "Page"
    "page_title" -> "Title"
    "entry_page" -> "Entry page"
    "exit_page" -> "Exit page"
    "referrer" -> "Referrer"
    "channel" -> "Channel"
    "utm_source" -> "Source"
    "utm_medium" -> "Medium"
    "utm_campaign" -> "Campaign"
    "country" -> "Country"
    "region" -> "Region"
    "city" -> "City"
    "browser" -> "Browser"
    "operating_system" -> "OS"
    "device_type" -> "Device"
    "language" -> "Language"
    else -> parameter
}

/** Chip text for an active filter, e.g. "Browser: Firefox". */
fun filterChipLabel(filter: Filter): String {
    val value = filter.values.firstOrNull().orEmpty()
    return "${parameterLabel(filter.parameter)}: ${displayValue(filter.parameter, value)}"
}

/** Equality filter for a tapped breakdown row. */
fun equalsFilter(parameter: String, rawValue: String): Filter =
    Filter(parameter = parameter, type = FilterType.Equals, values = listOf(rawValue))

/**
 * Adds one page to a full-list sheet. [totalCount] from the server wins when present; otherwise a
 * full page means there may be more.
 */
fun appendPage(list: FullListState, page: MetricPage, pageNumber: Int, pageSize: Int): FullListState {
    val rows = list.rows + page.data
    val total = page.totalCount ?: list.totalCount
    val hasMore = when {
        page.data.isEmpty() -> false
        total != null -> rows.size < total
        else -> page.data.size >= pageSize
    }
    return list.copy(
        rows = rows,
        totalCount = total,
        page = pageNumber,
        hasMore = hasMore,
        isLoadingMore = false,
        error = null,
    )
}
