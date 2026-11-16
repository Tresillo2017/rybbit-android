package app.rybbitclient.feature.events

import app.rybbitclient.core.query.DateRange
import app.rybbitclient.core.query.Filter
import app.rybbitclient.core.query.FilterType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class QueryPresetsTest {

    @Test
    fun `custom range is ordered when the picker reports the ends backwards`() {
        // Arrange
        val start = millisOf(LocalDate.of(2026, 11, 9))
        val end = millisOf(LocalDate.of(2026, 11, 3))

        // Act
        val range = customRangeFor(start, end)

        // Assert
        assertEquals(DateRange.Custom(LocalDate.of(2026, 11, 3), LocalDate.of(2026, 11, 9)), range)
    }

    @Test
    fun `picker millis map back to the same calendar day`() {
        // Arrange
        val day = LocalDate.of(2026, 11, 16)

        // Act
        val range = customRangeFor(millisOf(day), millisOf(day))

        // Assert
        assertEquals(DateRange.Custom(day, day), range)
    }

    @Test
    fun `preset keys round trip and custom ranges have no key`() {
        // Arrange
        val day = LocalDate.of(2026, 11, 16)

        // Act
        val key = DateRangePresets.keyFor(DateRange.Last7Days)

        // Assert
        assertEquals("7d", key)
        assertEquals(DateRange.Last7Days, DateRangePresets.rangeFor(key!!))
        assertNull(DateRangePresets.keyFor(DateRange.Custom(day, day)))
    }

    @Test
    fun `filter chips show the parameter as words followed by its values`() {
        // Arrange
        val filter = Filter(parameter = "operating_system", type = FilterType.Equals, values = listOf("Windows", "macOS"))

        // Act
        val label = filterLabel(filter)

        // Assert
        assertEquals("Operating system: Windows, macOS", label)
    }

    @Test
    fun `filters without values show their operator instead`() {
        // Arrange
        val filter = Filter(parameter = "country", type = FilterType.IsNull, values = emptyList())

        // Act
        val label = filterLabel(filter)

        // Assert
        assertEquals("Country: is null", label)
    }
}
