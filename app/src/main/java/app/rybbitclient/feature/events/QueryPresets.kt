package app.rybbitclient.feature.events

import app.rybbitclient.core.query.DateRange
import app.rybbitclient.core.query.Filter
import app.rybbitclient.core.ui.components.DateRangeOption
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale

private data class DatePreset(val key: String, val label: String, val range: DateRange)

private val DATE_PRESETS = listOf(
    DatePreset("today", "Today", DateRange.Today),
    DatePreset("yesterday", "Yesterday", DateRange.Yesterday),
    DatePreset("24h", "Last 24 h", DateRange.Last24Hours),
    DatePreset("7d", "Last 7 days", DateRange.Last7Days),
    DatePreset("30d", "Last 30 days", DateRange.Last30Days),
    DatePreset("90d", "Last 90 days", DateRange.Last90Days),
    DatePreset("this-month", "This month", DateRange.ThisMonth),
    DatePreset("last-month", "Last month", DateRange.LastMonth),
    DatePreset("this-year", "This year", DateRange.ThisYear),
    DatePreset("all", "All time", DateRange.AllTime),
)

/** Preset chips for the shared date range. Keys are stable strings; custom ranges have no key. */
object DateRangePresets {

    val options: List<DateRangeOption> = DATE_PRESETS.map { DateRangeOption(it.key, it.label) }

    /** The preset key for [range], or null when the range is custom. */
    fun keyFor(range: DateRange): String? = DATE_PRESETS.firstOrNull { it.range == range }?.key

    /** The range for a preset key, or null for an unknown key. */
    fun rangeFor(key: String): DateRange? = DATE_PRESETS.firstOrNull { it.key == key }?.range
}

/**
 * Builds a custom range from the picker's UTC-midnight millis. The two ends are swapped when the
 * user picked them backwards, so the result is always a valid [DateRange.Custom].
 */
fun customRangeFor(startMillis: Long, endMillis: Long): DateRange.Custom {
    val first = utcDate(startMillis)
    val second = utcDate(endMillis)
    return if (second.isBefore(first)) DateRange.Custom(second, first) else DateRange.Custom(first, second)
}

/** UTC-midnight millis for [date], the form the date picker expects. */
fun millisOf(date: LocalDate): Long = date.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()

/** Label for the Custom chip, e.g. "3 Nov - 9 Nov". */
fun customRangeText(range: DateRange.Custom): String {
    val formatter = DateTimeFormatter.ofPattern(CUSTOM_RANGE_PATTERN, Locale.getDefault())
    return "${formatter.format(range.start)} - ${formatter.format(range.end)}"
}

/** Chip text for an applied filter, e.g. "Browser: Firefox". Parameters are shown as sentence-case words. */
fun filterLabel(filter: Filter): String {
    val title = filter.parameter.replace('_', ' ').replaceFirstChar { it.titlecase() }
    val detail = if (filter.values.isEmpty()) {
        filter.type.apiValue.replace('_', ' ')
    } else {
        filter.values.joinToString(separator = ", ")
    }
    return "$title: $detail"
}

private fun utcDate(millis: Long): LocalDate = Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate()

private const val CUSTOM_RANGE_PATTERN = "d MMM"
