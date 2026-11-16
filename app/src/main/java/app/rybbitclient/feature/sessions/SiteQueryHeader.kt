package app.rybbitclient.feature.sessions

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import app.rybbitclient.core.query.AnalyticsQuery
import app.rybbitclient.core.query.DateRange
import app.rybbitclient.core.query.Filter
import app.rybbitclient.core.ui.components.ActiveFilter
import app.rybbitclient.core.ui.components.DateRangeBar
import app.rybbitclient.core.ui.components.DateRangeOption
import app.rybbitclient.core.ui.components.FilterChipsRow
import app.rybbitclient.core.ui.theme.Spacing
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale

private const val CUSTOM_LABEL = "Custom"
private const val RANGE_SEPARATOR = " – "
private val RANGE_DAY_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("d MMM", Locale.getDefault())

/** Preset chips, in display order, each paired with the range it selects. */
private val PRESETS: List<Pair<DateRangeOption, DateRange>> = listOf(
    DateRangeOption("today", "Today") to DateRange.Today,
    DateRangeOption("24h", "24 hours") to DateRange.Last24Hours,
    DateRangeOption("7d", "7 days") to DateRange.Last7Days,
    DateRangeOption("30d", "30 days") to DateRange.Last30Days,
    DateRangeOption("90d", "90 days") to DateRange.Last90Days,
    DateRangeOption("month", "This month") to DateRange.ThisMonth,
    DateRangeOption("last_month", "Last month") to DateRange.LastMonth,
    DateRangeOption("year", "This year") to DateRange.ThisYear,
    DateRangeOption("all", "All time") to DateRange.AllTime,
)

val DATE_RANGE_OPTIONS: List<DateRangeOption> = PRESETS.map { it.first }

/** The preset key for [range], or null for a custom range (which the picker chip shows instead). */
fun presetKeyFor(range: DateRange): String? = PRESETS.firstOrNull { it.second == range }?.first?.key

fun rangeForPreset(key: String): DateRange? = PRESETS.firstOrNull { it.first.key == key }?.second

/** Turns the picker's UTC-midnight millis into a custom range. An end before the start collapses to one day. */
fun customRangeFromMillis(startMillis: Long, endMillis: Long): DateRange.Custom {
    val start = Instant.ofEpochMilli(startMillis).atZone(ZoneOffset.UTC).toLocalDate()
    val end = Instant.ofEpochMilli(endMillis).atZone(ZoneOffset.UTC).toLocalDate()
    return DateRange.Custom(start = start, end = if (end.isBefore(start)) start else end)
}

fun customRangeLabel(range: DateRange.Custom): String =
    listOf(range.start, range.end).joinToString(RANGE_SEPARATOR) { it.format(RANGE_DAY_FORMAT) }

/** Millis at UTC midnight for [date], the form the date picker expects. */
fun utcMidnightMillis(date: LocalDate): Long = date.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()

/** Stable key for a filter, used to find it again from a chip's key. */
fun filterKey(filter: Filter): String = "${filter.parameter}|${filter.type.apiValue}|${filter.values.joinToString(",")}"

/** `Browser: Firefox`, or `Browser not equals: Firefox` for negated filters. */
fun filterLabel(filter: Filter): String {
    val name = filter.parameter.replace('_', ' ').replaceFirstChar { it.uppercaseChar() }
    val comparison = if (filter.type.apiValue == "equals") "" else " " + filter.type.apiValue.replace('_', ' ')
    return "$name$comparison: ${filter.values.joinToString(", ")}"
}

fun activeFilterChips(filters: List<Filter>): List<ActiveFilter> =
    filters.map { ActiveFilter(key = filterKey(it), label = filterLabel(it)) }

/**
 * The date range and filter controls shared by every site tab, bound to the shared query state through the
 * callbacks. Renders the filter chips only while a filter is applied.
 */
@Composable
fun SiteQueryHeader(
    query: AnalyticsQuery,
    onRangeSelected: (DateRange) -> Unit,
    onRemoveFilter: (Filter) -> Unit,
    onClearFilters: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val custom = query.range as? DateRange.Custom
    Column(modifier = modifier.padding(vertical = Spacing.sm)) {
        DateRangeBar(
            options = DATE_RANGE_OPTIONS,
            selectedKey = presetKeyFor(query.range),
            onPresetSelected = { key -> rangeForPreset(key)?.let(onRangeSelected) },
            onCustomRangeSelected = { start, end -> onRangeSelected(customRangeFromMillis(start, end)) },
            customLabel = custom?.let(::customRangeLabel) ?: CUSTOM_LABEL,
            customSelected = custom != null,
            initialStartMillis = custom?.let { utcMidnightMillis(it.start) },
            initialEndMillis = custom?.let { utcMidnightMillis(it.end) },
        )
        FilterChipsRow(
            filters = activeFilterChips(query.filters),
            onRemove = { key -> query.filters.firstOrNull { filterKey(it) == key }?.let(onRemoveFilter) },
            onClearAll = onClearFilters,
        )
    }
}
