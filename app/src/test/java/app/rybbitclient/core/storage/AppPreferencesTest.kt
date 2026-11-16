package app.rybbitclient.core.storage

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class AppPreferencesTest {

    private val store = InMemoryKeyValueStore()
    private val preferences = AppPreferences(store)

    @Test
    fun `fresh install gets the defaults`() = runBlocking {
        assertEquals(AppSettings(), preferences.current())
    }

    @Test
    fun `each setter changes only its own field`() = runBlocking {
        preferences.setThemeMode(ThemeMode.Dark)
        preferences.setAccent(AccentColor.Rose)
        preferences.setPureBlack(true)
        preferences.setDefaultRange("30d")

        val settings = preferences.current()

        assertEquals(ThemeMode.Dark, settings.themeMode)
        assertEquals(AccentColor.Rose, settings.accent)
        assertEquals(true, settings.pureBlack)
        assertEquals("30d", settings.defaultRangeKey)
        assertEquals(AppSettings().dynamicColor, settings.dynamicColor)
        assertEquals(AppSettings().hapticsEnabled, settings.hapticsEnabled)
    }

    @Test
    fun `switches round trip both values`() = runBlocking {
        preferences.setHapticsEnabled(false)
        preferences.setReduceMotion(true)
        preferences.setSmoothCharts(false)
        preferences.setDynamicColor(false)

        val settings = preferences.current()

        assertEquals(false, settings.hapticsEnabled)
        assertEquals(true, settings.reduceMotion)
        assertEquals(false, settings.smoothCharts)
        assertEquals(false, settings.dynamicColor)
    }

    @Test
    fun `unrecognised stored values fall back to defaults`() = runBlocking {
        store.write("pref.theme_mode", "Sepia")
        store.write("pref.accent", "Chartreuse")
        store.write("pref.pure_black", "maybe")
        store.write("pref.default_range", "  ")

        assertEquals(AppSettings(), preferences.current())
    }

    @Test
    fun `reset returns every preference to its default`() = runBlocking {
        preferences.setThemeMode(ThemeMode.Light)
        preferences.setAccent(AccentColor.Amber)
        preferences.setHapticsEnabled(false)
        preferences.setDefaultRange("90d")

        preferences.resetToDefaults()

        assertEquals(AppSettings(), preferences.current())
    }
}
