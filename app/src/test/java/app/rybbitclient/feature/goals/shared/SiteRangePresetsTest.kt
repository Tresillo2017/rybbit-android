package app.rybbitclient.feature.goals.shared

import app.rybbitclient.core.query.DateRange
import app.rybbitclient.core.query.Filter
import app.rybbitclient.core.query.FilterType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate
import java.util.Locale

class SiteRangePresetsTest {

    @Test
    fun `a preset range maps to its chip key and back`() {
        // Act and Assert
        assertEquals("7d", presetKeyFor(DateRange.Last7Days))
        assertEquals(DateRange.Last7Days, presetRangeFor("7d"))
        assertNull(presetRangeFor("unknown"))
    }

    @Test
    fun `a custom range has no preset chip and a dated label`() {
        // Arrange
        val custom = DateRange.Custom(LocalDate.of(2026, 11, 3), LocalDate.of(2026, 11, 9))

        // Act and Assert
        assertNull(presetKeyFor(custom))
        assertEquals("3 Nov – 9 Nov", customRangeLabel(custom, Locale.ENGLISH))
        assertNull(customRangeLabel(DateRange.Last30Days, Locale.ENGLISH))
    }

    @Test
    fun `picker millis round trip to the same calendar day`() {
        // Arrange
        val day = LocalDate.of(2026, 11, 16)

        // Act and Assert
        assertEquals(day, millisToLocalDate(localDateToMillis(day)))
    }

    @Test
    fun `a picker range reported with swapped ends is ordered`() {
        // Arrange
        val start = localDateToMillis(LocalDate.of(2026, 11, 9))
        val end = localDateToMillis(LocalDate.of(2026, 11, 3))

        // Act
        val range = customRangeFromMillis(start, end)

        // Assert
        assertEquals(LocalDate.of(2026, 11, 3), range.start)
        assertEquals(LocalDate.of(2026, 11, 9), range.end)
    }

    @Test
    fun `a filter becomes a chip keyed by its parameter`() {
        // Arrange
        val filter = Filter(parameter = "country", type = FilterType.Equals, values = listOf("ES", "FR"))

        // Act
        val chip = activeFilterOf(filter)

        // Assert
        assertEquals("country", chip.key)
        assertEquals("country: ES, FR", chip.label)
    }
}
