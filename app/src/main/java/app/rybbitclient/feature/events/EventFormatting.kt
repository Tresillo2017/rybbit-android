package app.rybbitclient.feature.events

import app.rybbitclient.core.network.RybbitTimestamps
import app.rybbitclient.core.ui.format.EMPTY_VALUE
import app.rybbitclient.core.ui.format.formatRelativeTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private const val EVENT_TIME_PATTERN = "d MMM, HH:mm"

/** A UTC event timestamp in the device zone, e.g. "16 Nov, 08:41". Unparseable input shows the empty value. */
fun formatEventTime(rawUtc: String?): String {
    val instant = RybbitTimestamps.parseUtcOrNull(rawUtc) ?: return EMPTY_VALUE
    return DateTimeFormatter.ofPattern(EVENT_TIME_PATTERN, Locale.getDefault())
        .format(instant.atZone(ZoneId.systemDefault()))
}

/** A UTC event timestamp as a short relative time, e.g. "5 min ago". */
fun relativeEventTime(rawUtc: String?, nowMillis: Long = System.currentTimeMillis()): String {
    val instant = RybbitTimestamps.parseUtcOrNull(rawUtc) ?: return EMPTY_VALUE
    return formatRelativeTime(instant.toEpochMilli(), nowMillis)
}
