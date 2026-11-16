package app.rybbitclient.core.ui.components

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertTrue
import org.junit.Test

class RetentionContrastTest {

    @Test
    fun `mid-ramp heat cell gets the text colour with the better contrast`() {
        // Luminance about 0.41 (the light-theme cell at half intensity): white text would be about 2.3:1.
        val background = Color(0xFF78BAA6)

        val chosen = textColorFor(background)

        assertTrue(contrastRatio(chosen, background) >= AA_TEXT_CONTRAST)
    }

    @Test
    fun `chosen text colour is never worse than the alternative`() {
        val samples = (0..20).map { step -> Color(red = step / 20f, green = 0.5f, blue = 0.3f) }

        samples.forEach { background ->
            val chosen = contrastRatio(textColorFor(background), background)
            assertTrue(chosen >= contrastRatio(Color.White, background) - EPSILON)
            assertTrue(chosen >= contrastRatio(Color(0xFF0F1A16), background) - EPSILON)
        }
    }

    private companion object {
        const val AA_TEXT_CONTRAST = 4.5
        const val EPSILON = 1e-9
    }
}
