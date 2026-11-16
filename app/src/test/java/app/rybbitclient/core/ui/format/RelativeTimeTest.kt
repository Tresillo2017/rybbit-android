package app.rybbitclient.core.ui.format

import org.junit.Assert.assertEquals
import org.junit.Test

class RelativeTimeTest {

    private val now = 1_800_000_000_000L
    private val minuteMillis = 60_000L
    private val hourMillis = 60 * minuteMillis
    private val dayMillis = 24 * hourMillis

    @Test
    fun `recent timestamps read as just now`() {
        assertEquals("just now", formatRelativeTime(now - 30_000L, now))
        assertEquals("just now", formatRelativeTime(now, now))
    }

    @Test
    fun `future timestamps from clock skew read as just now`() {
        assertEquals("just now", formatRelativeTime(now + 5_000L, now))
    }

    @Test
    fun `minutes are shown in minutes`() {
        assertEquals("3 min ago", formatRelativeTime(now - 3 * minuteMillis, now))
    }

    @Test
    fun `hours are shown in hours`() {
        assertEquals("2 h ago", formatRelativeTime(now - 2 * hourMillis, now))
    }

    @Test
    fun `days and months use day and month units`() {
        assertEquals("5 d ago", formatRelativeTime(now - 5 * dayMillis, now))
        assertEquals("2 mo ago", formatRelativeTime(now - 60 * dayMillis, now))
    }

    @Test
    fun `very old timestamps are shown in years`() {
        assertEquals("1 y ago", formatRelativeTime(now - 400 * dayMillis, now))
    }
}
