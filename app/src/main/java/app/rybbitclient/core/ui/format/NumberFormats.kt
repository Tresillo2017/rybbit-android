package app.rybbitclient.core.ui.format

import java.math.RoundingMode
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToLong

/** Shown in place of any value that is NaN or infinite. */
const val EMPTY_VALUE = "—"

private const val COMPACT_ONE_DECIMAL_LIMIT = 10.0
private const val COMPACT_ROLLOVER_LIMIT = 1000.0
private const val PERCENT_DIVISOR = 100.0
private const val DEFAULT_PERCENT_DIGITS = 1

private val COMPACT_SUFFIXES = listOf(
    1_000.0 to "k",
    1_000_000.0 to "M",
    1_000_000_000.0 to "B",
    1_000_000_000_000.0 to "T",
)

/** Whole-number grouping in the locale's style, e.g. `1234567` -> `1,234,567` (en). */
fun formatFullNumber(value: Double, locale: Locale = Locale.getDefault()): String {
    if (!value.isFinite()) return EMPTY_VALUE
    val format = NumberFormat.getIntegerInstance(locale).apply {
        roundingMode = RoundingMode.HALF_UP
    }
    return format.format(value.roundToLong())
}

/**
 * Compact count such as `1.2k`, `3.4M`, `12k`. Values under 10 in a unit keep one decimal.
 * Rounding that spills over (999_999 -> 1000k) rolls into the next unit (-> `1M`).
 */
fun formatCompactNumber(value: Double, locale: Locale = Locale.getDefault()): String {
    if (!value.isFinite()) return EMPTY_VALUE
    val unitIndex = resolveCompactUnit(value)
    val rounded = roundCompact(value / compactDivisor(unitIndex))
    val decimals = if (unitIndex >= 0 && abs(rounded) < COMPACT_ONE_DECIMAL_LIMIT) 1 else 0
    val suffix = if (unitIndex < 0) "" else COMPACT_SUFFIXES[unitIndex].second
    return numberFormat(locale, decimals).format(rounded) + suffix
}

/**
 * Percentage from a value already expressed in percent units (Rybbit returns `bounce_rate` 0..100).
 * `12.345` -> `12.3%`.
 */
fun formatPercent(
    percent: Double,
    locale: Locale = Locale.getDefault(),
    fractionDigits: Int = DEFAULT_PERCENT_DIGITS,
): String {
    if (!percent.isFinite()) return EMPTY_VALUE
    val format = NumberFormat.getPercentInstance(locale).apply {
        minimumFractionDigits = fractionDigits
        maximumFractionDigits = fractionDigits
        roundingMode = RoundingMode.HALF_UP
    }
    return format.format(percent / PERCENT_DIVISOR)
}

/**
 * Signed change with an explicit sign: `+12.3%`, `-4.0%`. A change that rounds to zero is
 * rendered unsigned (`0.0%`) so it never reads as "+0.0%" or "-0.0%".
 */
fun formatDeltaPercent(
    deltaPercent: Double,
    locale: Locale = Locale.getDefault(),
    fractionDigits: Int = DEFAULT_PERCENT_DIGITS,
): String {
    if (!deltaPercent.isFinite()) return EMPTY_VALUE
    val magnitude = formatPercent(abs(deltaPercent), locale, fractionDigits)
    if (magnitude == formatPercent(0.0, locale, fractionDigits)) return magnitude
    val sign = if (deltaPercent > 0) "+" else "-"
    return sign + magnitude
}

/** Bounce rate in percent units, clamped to 0..100 and shown with one decimal. */
fun formatBounceRate(bouncePercent: Double, locale: Locale = Locale.getDefault()): String {
    if (!bouncePercent.isFinite()) return EMPTY_VALUE
    val clamped = bouncePercent.coerceIn(0.0, PERCENT_DIVISOR)
    return formatPercent(clamped, locale, DEFAULT_PERCENT_DIGITS)
}

private fun resolveCompactUnit(value: Double): Int {
    var index = COMPACT_SUFFIXES.indexOfLast { abs(value) >= it.first }
    while (index < COMPACT_SUFFIXES.lastIndex &&
        abs(roundCompact(value / compactDivisor(index))) >= COMPACT_ROLLOVER_LIMIT
    ) {
        index++
    }
    return index
}

private fun compactDivisor(unitIndex: Int): Double =
    if (unitIndex < 0) 1.0 else COMPACT_SUFFIXES[unitIndex].first

private fun roundCompact(scaled: Double): Double =
    if (abs(scaled) < COMPACT_ONE_DECIMAL_LIMIT) {
        (scaled * 10.0).roundToLong() / 10.0
    } else {
        scaled.roundToLong().toDouble()
    }

private fun numberFormat(locale: Locale, decimals: Int): NumberFormat =
    NumberFormat.getNumberInstance(locale).apply {
        minimumFractionDigits = 0
        maximumFractionDigits = decimals
        roundingMode = RoundingMode.HALF_UP
    }
