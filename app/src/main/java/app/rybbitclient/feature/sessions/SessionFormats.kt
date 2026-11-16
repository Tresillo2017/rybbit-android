package app.rybbitclient.feature.sessions

import app.rybbitclient.core.network.RybbitTimestamps
import app.rybbitclient.core.ui.format.EMPTY_VALUE
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private const val SHORT_ID_LENGTH = 8
private val LOCAL_DATE_TIME: DateTimeFormatter = DateTimeFormatter.ofPattern("d MMM yyyy, HH:mm:ss", Locale.getDefault())

/** Parses a UTC analytics timestamp to epoch millis, or null when it is missing or malformed. */
fun utcMillisOrNull(value: String?): Long? = RybbitTimestamps.parseUtcOrNull(value)?.toEpochMilli()

/** Local wall time in [zone], e.g. `16 Nov 2026, 09:41:49`. */
fun formatLocalDateTime(instant: Instant, zone: ZoneId): String = instant.atZone(zone).format(LOCAL_DATE_TIME)

/** `Safari 17`, `Chrome`, or an em dash when both parts are missing. */
fun browserLabel(browser: String?, version: String?): String = joinNonBlank(" ", browser, version)

/** `macOS 14.5`, `iOS`, or an em dash when both parts are missing. */
fun operatingSystemLabel(system: String?, version: String?): String = joinNonBlank(" ", system, version)

/** `Harlesden, Region, GB` style location: the non-blank parts joined with commas, or an em dash. */
fun locationLabel(city: String?, region: String?, country: String?): String =
    joinNonBlank(", ", city, region, country)

/** Screen size as `1440 × 900`, or an em dash when a side is missing or zero. */
fun screenLabel(width: Int?, height: Int?): String =
    if (width == null || height == null || width <= 0 || height <= 0) EMPTY_VALUE else "$width × $height"

/** The identifier to show for a user: the identified ID when set, otherwise a short anonymous ID. */
fun userDisplayName(userId: String?, identifiedUserId: String?): String {
    identifiedUserId?.takeIf(String::isNotBlank)?.let { return it }
    val anonymous = userId?.takeIf(String::isNotBlank) ?: return EMPTY_VALUE
    return "Anonymous ${anonymous.take(SHORT_ID_LENGTH)}"
}

/** Blank or missing values render as an em dash. */
fun String?.orDash(): String = this?.takeIf(String::isNotBlank) ?: EMPTY_VALUE

private fun joinNonBlank(separator: String, vararg parts: String?): String {
    val present = parts.mapNotNull { it?.takeIf(String::isNotBlank) }
    return if (present.isEmpty()) EMPTY_VALUE else present.joinToString(separator)
}
