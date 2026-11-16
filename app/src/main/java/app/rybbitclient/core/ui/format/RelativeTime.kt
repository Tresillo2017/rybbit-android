package app.rybbitclient.core.ui.format

private const val MILLIS_PER_SECOND = 1_000L
private const val SECONDS_PER_MINUTE = 60L
private const val MINUTES_PER_HOUR = 60L
private const val HOURS_PER_DAY = 24L
private const val DAYS_PER_MONTH = 30L
private const val DAYS_PER_YEAR = 365L
private const val JUST_NOW_MILLIS = 60L * MILLIS_PER_SECOND

/**
 * Short relative time, English copy: `just now`, `3 min ago`, `2 h ago`, `5 d ago`, `4 mo ago`, `2 y ago`.
 * Timestamps in the future (clock skew) read as `just now`. [nowMillis] is injectable for tests.
 */
fun formatRelativeTime(
    timestampMillis: Long,
    nowMillis: Long = System.currentTimeMillis(),
): String {
    val elapsedMillis = nowMillis - timestampMillis
    if (elapsedMillis < JUST_NOW_MILLIS) return "just now"
    val minutes = elapsedMillis / JUST_NOW_MILLIS
    val hours = minutes / MINUTES_PER_HOUR
    val days = hours / HOURS_PER_DAY
    return when {
        minutes < MINUTES_PER_HOUR -> "$minutes min ago"
        hours < HOURS_PER_DAY -> "$hours h ago"
        days < DAYS_PER_MONTH -> "$days d ago"
        days < DAYS_PER_YEAR -> "${days / DAYS_PER_MONTH} mo ago"
        else -> "${days / DAYS_PER_YEAR} y ago"
    }
}
