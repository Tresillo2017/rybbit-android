package app.rybbitclient.core.ui.format

import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Test

class NumberFormatsTest {

    @Test
    fun `full number groups thousands in locale style`() {
        assertEquals("1,234,567", formatFullNumber(1_234_567.0, Locale.US))
    }

    @Test
    fun `full number rounds half up and handles zero`() {
        assertEquals("3", formatFullNumber(2.5, Locale.US))
        assertEquals("0", formatFullNumber(0.0, Locale.US))
    }

    @Test
    fun `compact number below one thousand is a whole number`() {
        assertEquals("999", formatCompactNumber(999.0, Locale.US))
        assertEquals("0", formatCompactNumber(0.0, Locale.US))
    }

    @Test
    fun `compact number keeps one decimal below ten units`() {
        assertEquals("1.2k", formatCompactNumber(1_234.0, Locale.US))
        assertEquals("3.4M", formatCompactNumber(3_400_000.0, Locale.US))
    }

    @Test
    fun `compact number drops decimal at ten units and above`() {
        assertEquals("12k", formatCompactNumber(12_345.0, Locale.US))
        assertEquals("123k", formatCompactNumber(123_456.0, Locale.US))
    }

    @Test
    fun `compact number rolls over into the next unit on rounding`() {
        assertEquals("1k", formatCompactNumber(999.7, Locale.US))
        assertEquals("1M", formatCompactNumber(999_999.0, Locale.US))
    }

    @Test
    fun `compact number keeps sign for negative values`() {
        assertEquals("-1.5k", formatCompactNumber(-1_500.0, Locale.US))
    }

    @Test
    fun `compact number uses locale decimal separator`() {
        assertEquals("1,2k", formatCompactNumber(1_234.0, Locale.GERMANY))
    }

    @Test
    fun `compact number shows dash for non-finite input`() {
        assertEquals(EMPTY_VALUE, formatCompactNumber(Double.NaN, Locale.US))
        assertEquals(EMPTY_VALUE, formatCompactNumber(Double.POSITIVE_INFINITY, Locale.US))
    }

    @Test
    fun `percent formats percent-unit input with one decimal`() {
        assertEquals("12.4%", formatPercent(12.36, Locale.US))
        assertEquals("50.0%", formatPercent(50.0, Locale.US))
    }

    @Test
    fun `percent honors requested fraction digits`() {
        assertEquals("12%", formatPercent(12.36, Locale.US, fractionDigits = 0))
    }

    @Test
    fun `percent shows dash for NaN`() {
        assertEquals(EMPTY_VALUE, formatPercent(Double.NaN, Locale.US))
    }

    @Test
    fun `delta percent carries explicit plus sign for increases`() {
        assertEquals("+12.3%", formatDeltaPercent(12.34, Locale.US))
    }

    @Test
    fun `delta percent carries minus sign for decreases`() {
        assertEquals("-4.0%", formatDeltaPercent(-4.0, Locale.US))
    }

    @Test
    fun `delta percent drops sign when change rounds to zero`() {
        assertEquals("0.0%", formatDeltaPercent(0.01, Locale.US))
        assertEquals("0.0%", formatDeltaPercent(-0.01, Locale.US))
    }

    @Test
    fun `bounce rate formats with one decimal`() {
        assertEquals("41.2%", formatBounceRate(41.23, Locale.US))
    }

    @Test
    fun `bounce rate clamps to zero through one hundred`() {
        assertEquals("100.0%", formatBounceRate(150.0, Locale.US))
        assertEquals("0.0%", formatBounceRate(-3.0, Locale.US))
    }

    @Test
    fun `bounce rate shows dash for infinite input`() {
        assertEquals(EMPTY_VALUE, formatBounceRate(Double.POSITIVE_INFINITY, Locale.US))
    }
}
