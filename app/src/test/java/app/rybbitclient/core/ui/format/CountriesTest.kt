package app.rybbitclient.core.ui.format

import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Test

class CountriesTest {

    private val germanyFlag = "🇩🇪"
    private val usFlag = "🇺🇸"

    @Test
    fun `flag is built from regional indicators`() {
        assertEquals(germanyFlag, countryFlagEmoji("DE"))
    }

    @Test
    fun `flag accepts lowercase and surrounding whitespace`() {
        assertEquals(usFlag, countryFlagEmoji(" us "))
    }

    @Test
    fun `flag is empty for invalid codes`() {
        assertEquals("", countryFlagEmoji("1A"))
        assertEquals("", countryFlagEmoji("DEU"))
        assertEquals("", countryFlagEmoji(""))
    }

    @Test
    fun `display name is localized to the requested locale`() {
        assertEquals("Germany", countryDisplayName("DE", Locale.ENGLISH))
        assertEquals("Deutschland", countryDisplayName("de", Locale.GERMAN))
    }

    @Test
    fun `display name falls back to the code for invalid input`() {
        assertEquals("1A", countryDisplayName("1A", Locale.ENGLISH))
    }

    @Test
    fun `label combines flag and localized name`() {
        assertEquals("$usFlag United States", countryLabel("US", Locale.US))
    }

    @Test
    fun `label falls back to raw value when code is invalid`() {
        assertEquals("1A", countryLabel("1A", Locale.US))
    }
}
