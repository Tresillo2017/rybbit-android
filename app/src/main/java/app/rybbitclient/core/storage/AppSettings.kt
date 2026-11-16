package app.rybbitclient.core.storage

/** Light/dark behaviour: follow the system or force one. */
enum class ThemeMode(val label: String) {
    System("System"),
    Light("Light"),
    Dark("Dark"),
}

/**
 * Accent colours offered when dynamic (wallpaper) colour is off or unavailable. [hue] is the HSL hue in degrees
 * that the palette generator builds a full tonal scheme around; [Emerald] is Rybbit's own brand scheme.
 */
enum class AccentColor(val label: String, val hue: Float) {
    Emerald("Emerald", 160f),
    Ocean("Ocean", 205f),
    Indigo("Indigo", 245f),
    Violet("Violet", 275f),
    Rose("Rose", 340f),
    Amber("Amber", 35f),
}

/** Everything the user can personalise. Defaults are what a fresh install gets. */
data class AppSettings(
    val themeMode: ThemeMode = ThemeMode.System,
    val dynamicColor: Boolean = true,
    val accent: AccentColor = AccentColor.Emerald,
    val pureBlack: Boolean = false,
    val hapticsEnabled: Boolean = true,
    val reduceMotion: Boolean = false,
    val smoothCharts: Boolean = true,
    /** Key of the [app.rybbitclient.feature.overview.RANGE_PRESETS] entry used when the app opens. */
    val defaultRangeKey: String = DEFAULT_RANGE_KEY,
) {
    companion object {
        const val DEFAULT_RANGE_KEY = "7d"
    }
}
