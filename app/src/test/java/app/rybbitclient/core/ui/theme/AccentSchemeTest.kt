package app.rybbitclient.core.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import app.rybbitclient.core.storage.AccentColor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class AccentSchemeTest {

    @Test
    fun `hsl converts the primary hues`() {
        assertEquals(Color(1f, 0f, 0f), hsl(0f, 1f, 0.5f))
        assertEquals(Color(0f, 1f, 0f), hsl(120f, 1f, 0.5f))
        assertEquals(Color(0f, 0f, 1f), hsl(240f, 1f, 0.5f))
    }

    @Test
    fun `hsl wraps hues outside zero to 360`() {
        assertEquals(hsl(20f, 0.5f, 0.5f), hsl(380f, 0.5f, 0.5f))
        assertEquals(hsl(340f, 0.5f, 0.5f), hsl(-20f, 0.5f, 0.5f))
    }

    @Test
    fun `lightness extremes give white and black regardless of hue`() {
        assertEquals(Color.White, hsl(200f, 0.8f, 1f))
        assertEquals(Color.Black, hsl(200f, 0.8f, 0f))
    }

    @Test
    fun `every accent has a distinct primary in light and dark`() {
        val light = AccentColor.entries.map { it.previewColor(dark = false) }
        val dark = AccentColor.entries.map { it.previewColor(dark = true) }
        assertEquals(AccentColor.entries.size, light.toSet().size)
        assertEquals(AccentColor.entries.size, dark.toSet().size)
    }

    @Test
    fun `dark primaries are lighter than light primaries so they read on dark surfaces`() {
        AccentColor.entries.forEach { accent ->
            assertTrue(
                "${accent.label} dark primary should be lighter",
                accent.previewColor(dark = true).luminance() > accent.previewColor(dark = false).luminance(),
            )
        }
    }

    @Test
    fun `text on primary keeps readable contrast for every generated accent`() {
        AccentColor.entries.filter { it != AccentColor.Emerald }.forEach { accent ->
            listOf(false, true).forEach { dark ->
                val scheme = accentColorScheme(accent.hue, dark)
                val ratio = contrast(scheme.onPrimary, scheme.primary)
                assertTrue("${accent.label} dark=$dark contrast $ratio", ratio >= MIN_TEXT_CONTRAST)
                val containerRatio = contrast(scheme.onPrimaryContainer, scheme.primaryContainer)
                assertTrue("${accent.label} dark=$dark container contrast $containerRatio", containerRatio >= MIN_TEXT_CONTRAST)
            }
        }
    }

    @Test
    fun `pure black sets the base surfaces to true black and keeps cards distinguishable`() {
        val scheme = accentColorScheme(AccentColor.Ocean.hue, dark = true).withPureBlack()
        assertEquals(Color.Black, scheme.background)
        assertEquals(Color.Black, scheme.surface)
        assertNotEquals(scheme.background, scheme.surfaceContainerLow)
        assertTrue(scheme.surfaceContainerHighest.luminance() > scheme.surfaceContainerLow.luminance())
    }

    @Test
    fun `brand scheme gets tinted surface containers instead of the baseline purple`() {
        val tinted = RybbitLightScheme.surfaceContainerLow
        // Baseline M3 surfaceContainerLow is a purple-tinted value where red and blue exceed green.
        assertTrue(tinted.green >= tinted.red && tinted.green >= tinted.blue - ALLOWED_BLUE_LEAD)
    }

    private fun contrast(a: Color, b: Color): Float {
        val l1 = maxOf(a.luminance(), b.luminance())
        val l2 = minOf(a.luminance(), b.luminance())
        return (l1 + CONTRAST_OFFSET) / (l2 + CONTRAST_OFFSET)
    }

    private companion object {
        const val MIN_TEXT_CONTRAST = 4.5f
        const val CONTRAST_OFFSET = 0.05f
        const val ALLOWED_BLUE_LEAD = 0.02f
    }
}
