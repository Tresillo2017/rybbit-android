package app.rybbitclient.core.ui.format

import org.junit.Assert.assertEquals
import org.junit.Test

class DurationsTest {

    @Test
    fun `sub-minute durations show seconds only`() {
        assertEquals("45s", formatDuration(45.0))
        assertEquals("0s", formatDuration(0.0))
    }

    @Test
    fun `minute durations pad seconds to two digits`() {
        assertEquals("1m 05s", formatDuration(65.0))
        assertEquals("12m 00s", formatDuration(720L))
    }

    @Test
    fun `hour durations pad minutes and drop seconds`() {
        assertEquals("2h 03m", formatDuration(7_380L))
        assertEquals("1h 00m", formatDuration(3_600.0))
    }

    @Test
    fun `fractional seconds round to the nearest second`() {
        assertEquals("1m 00s", formatDuration(59.6))
        assertEquals("59s", formatDuration(59.4))
    }

    @Test
    fun `negative and non-finite input clamps to zero seconds`() {
        assertEquals("0s", formatDuration(-12.0))
        assertEquals("0s", formatDuration(Double.NaN))
        assertEquals("0s", formatDuration(-5L))
    }
}
