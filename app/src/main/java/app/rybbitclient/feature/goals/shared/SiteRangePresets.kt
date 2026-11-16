package app.rybbitclient.feature.goals.shared

import app.rybbitclient.core.query.DateRange
import app.rybbitclient.core.query.Filter
import app.rybbitclient.core.ui.components.ActiveFilter
import app.rybbitclient.core.ui.components.DateRangeOption
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale

private const val CUSTOM_LABEL_PATTERN = "d MMM"
private const val RANGE_SEPARATOR = " – "

private data class RangePreset(val key: String, val label: String, val range: DateRange)

private val rangePresets = listOf(
    RangePreset("today", "Today", DateRange.Today),
    RangePreset("yesterday", "Yesterday", DateRange.Yesterday),
    RangePreset("24h", "Last 24h", DateRange.Last24Hours),
    RangePreset("7d", "7 days", DateRange.Last7Days),
    RangePreset("30d", "30 days", DateRange.Last30Days),
    RangePreset("90d", "90 days", DateRange.Last90Days),
    RangePreset("month", "This month", DateRange.ThisMonth),
    RangePreset("lastMonth", "Last month", DateRange.LastMonth),
    RangePreset("year", "This year", DateRange.ThisYear),
    RangePreset("all", "All time", DateRange.AllTime),
)

/** Preset chips shown by the shared site date range bar. A custom range is added by the picker. */
val SiteRangeOptions: List<DateRangeOption> = rangePresets.map { DateRangeOption(it.key, it.label) }

/** The preset key for [range], or null when [range] is a custom range (no chip is highlighted then). */
fun presetKeyFor(range: DateRange): String? = rangePresets.firstOrNull { it.range == range }?.key

/** The preset range behind a chip key, or null for an unknown key. */
fun presetRangeFor(key: String): DateRange? = rangePresets.firstOrNull { it.key == key }?.range

/** Chip label for a custom range such as "3 Nov – 9 Nov"; null for presets. */
fun customRangeLabel(range: DateRange, locale: Locale = Locale.getDefault()): String? {
    val custom = range as? DateRange.Custom ?: return null
    val formatter = DateTimeFormatter.ofPattern(CUSTOM_LABEL_PATTERN, locale)
    return custom.start.format(formatter) + RANGE_SEPARATOR + custom.end.format(formatter)
}

/** The date picker reports UTC midnight millis; this maps them back to the calendar day they name. */
fun millisToLocalDate(epochMillis: Long): LocalDate =
    Instant.ofEpochMilli(epochMillis).atZone(ZoneOffset.UTC).toLocalDate()

/** UTC midnight millis for [date], the form the date picker expects. */
fun localDateToMillis(date: LocalDate): Long = date.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()

/** A custom range from the picker. The picker can report the ends swapped, so they are ordered here. */
fun customRangeFromMillis(startMillis: Long, endMillis: Long): DateRange.Custom {
    val start = millisToLocalDate(startMillis)
    val end = millisToLocalDate(endMillis)
    return if (end.isBefore(start)) DateRange.Custom(end, start) else DateRange.Custom(start, end)
}

/** A removable chip for [filter]. The chip key is the filtered parameter, which is unique per query. */
fun activeFilterOf(filter: Filter): ActiveFilter =
    ActiveFilter(key = filter.parameter, label = "${filter.parameter}: ${filter.values.joinToString(", ")}")
