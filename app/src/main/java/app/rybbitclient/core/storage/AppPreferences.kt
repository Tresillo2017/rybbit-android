package app.rybbitclient.core.storage

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first

/**
 * User-facing display preferences, persisted through a [KeyValueStore]. Missing or unrecognised stored values
 * fall back to the [AppSettings] defaults, so a fresh install (or a value written by a newer build) never breaks.
 */
class AppPreferences(private val store: KeyValueStore) {

    /** The current settings; emits again whenever any single preference changes. */
    val settings: Flow<AppSettings> = combine(KEYS.map { store.observe(it) }) { raw ->
        val values = KEYS.zip(raw.toList()).toMap()
        decode(values)
    }

    suspend fun current(): AppSettings = settings.first()

    suspend fun setThemeMode(mode: ThemeMode) = store.write(THEME_MODE, mode.name)

    suspend fun setDynamicColor(enabled: Boolean) = store.write(DYNAMIC_COLOR, enabled.toString())

    suspend fun setAccent(accent: AccentColor) = store.write(ACCENT, accent.name)

    suspend fun setPureBlack(enabled: Boolean) = store.write(PURE_BLACK, enabled.toString())

    suspend fun setHapticsEnabled(enabled: Boolean) = store.write(HAPTICS, enabled.toString())

    suspend fun setReduceMotion(enabled: Boolean) = store.write(REDUCE_MOTION, enabled.toString())

    suspend fun setSmoothCharts(enabled: Boolean) = store.write(SMOOTH_CHARTS, enabled.toString())

    suspend fun setDefaultRange(key: String) = store.write(DEFAULT_RANGE, key)

    /** Returns every preference to its default. Servers and credentials are stored elsewhere and untouched. */
    suspend fun resetToDefaults() = KEYS.forEach { store.write(it, null) }

    private fun decode(values: Map<String, String?>): AppSettings {
        val defaults = AppSettings()
        return AppSettings(
            themeMode = values[THEME_MODE].toEnum(defaults.themeMode),
            dynamicColor = values[DYNAMIC_COLOR].toFlag(defaults.dynamicColor),
            accent = values[ACCENT].toEnum(defaults.accent),
            pureBlack = values[PURE_BLACK].toFlag(defaults.pureBlack),
            hapticsEnabled = values[HAPTICS].toFlag(defaults.hapticsEnabled),
            reduceMotion = values[REDUCE_MOTION].toFlag(defaults.reduceMotion),
            smoothCharts = values[SMOOTH_CHARTS].toFlag(defaults.smoothCharts),
            defaultRangeKey = values[DEFAULT_RANGE]?.takeIf { it.isNotBlank() } ?: defaults.defaultRangeKey,
        )
    }

    private fun String?.toFlag(default: Boolean): Boolean = this?.toBooleanStrictOrNull() ?: default

    private inline fun <reified E : Enum<E>> String?.toEnum(default: E): E =
        enumValues<E>().firstOrNull { it.name == this } ?: default

    private companion object {
        const val THEME_MODE = "pref.theme_mode"
        const val DYNAMIC_COLOR = "pref.dynamic_color"
        const val ACCENT = "pref.accent"
        const val PURE_BLACK = "pref.pure_black"
        const val HAPTICS = "pref.haptics_enabled"
        const val REDUCE_MOTION = "pref.reduce_motion"
        const val SMOOTH_CHARTS = "pref.smooth_charts"
        const val DEFAULT_RANGE = "pref.default_range"

        val KEYS = listOf(
            THEME_MODE, DYNAMIC_COLOR, ACCENT, PURE_BLACK, HAPTICS, REDUCE_MOTION, SMOOTH_CHARTS, DEFAULT_RANGE,
        )
    }
}
