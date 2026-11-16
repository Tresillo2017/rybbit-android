package app.rybbitclient.core.ui.format

import java.util.Locale
import kotlin.math.roundToLong

private const val SECONDS_PER_MINUTE = 60L
private const val SECONDS_PER_HOUR = 3600L

/**
 * Human duration from seconds, rounded to the nearest second:
 * `45` -> `45s`, `65` -> `1m 05s`, `7380` -> `2h 03m`. Negative or non-finite input -> `0s`.
 */
fun formatDuration(seconds: Double): String {
    if (!seconds.isFinite() || seconds <= 0.0) return "0s"
    return formatDurationWhole(seconds.roundToLong())
}

fun formatDuration(seconds: Long): String = formatDurationWhole(seconds.coerceAtLeast(0L))

private fun formatDurationWhole(total: Long): String {
    val hours = total / SECONDS_PER_HOUR
    val minutes = (total % SECONDS_PER_HOUR) / SECONDS_PER_MINUTE
    val secs = total % SECONDS_PER_MINUTE
    return when {
        hours > 0 -> String.format(Locale.ROOT, "%dh %02dm", hours, minutes)
        minutes > 0 -> String.format(Locale.ROOT, "%dm %02ds", minutes, secs)
        else -> String.format(Locale.ROOT, "%ds", secs)
    }
}
