package app.rybbitclient.core.network

import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeFormatterBuilder
import java.time.temporal.ChronoField

/**
 * Rybbit timestamps look like `2026-11-16 08:41:49` with optional fractional seconds (1 to 9 digits)
 * and never carry a `T` or a zone. Event and session timestamps are UTC; time-series bucket values
 * are wall-clock times in the requested time zone, so they must be parsed as [LocalDateTime].
 */
object RybbitTimestamps {

    private val formatter: DateTimeFormatter = DateTimeFormatterBuilder()
        .appendPattern("yyyy-MM-dd HH:mm:ss")
        .optionalStart()
        .appendFraction(ChronoField.NANO_OF_SECOND, 1, 9, true)
        .optionalEnd()
        .toFormatter()

    /** Parses an event or session timestamp, which Rybbit stores in UTC. */
    fun parseUtc(value: String): Instant = parseLocal(value).toInstant(ZoneOffset.UTC)

    /** Parses a time-series bucket value, which is already a wall-clock time in the requested zone. */
    fun parseLocal(value: String): LocalDateTime = LocalDateTime.parse(value.trim(), formatter)

    /** Like [parseUtc] but returns null instead of throwing, for display code that must not crash. */
    fun parseUtcOrNull(value: String?): Instant? = value?.let { runCatching { parseUtc(it) }.getOrNull() }

    /** Like [parseLocal] but returns null instead of throwing. */
    fun parseLocalOrNull(value: String?): LocalDateTime? = value?.let { runCatching { parseLocal(it) }.getOrNull() }
}
