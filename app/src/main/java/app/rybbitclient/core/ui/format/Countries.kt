package app.rybbitclient.core.ui.format

import java.util.Locale

private const val REGIONAL_INDICATOR_A = 0x1F1E6
private const val ASCII_A = 'A'
private const val ASCII_Z = 'Z'
private const val ISO_CODE_LENGTH = 2

/**
 * ISO 3166-1 alpha-2 code to a flag emoji built from regional indicator symbols.
 * `"de"` -> `🇩🇪`. Returns an empty string when the code is not two ASCII letters.
 */
fun countryFlagEmoji(countryCode: String): String {
    val code = normalizeCountryCode(countryCode) ?: return ""
    return buildString {
        code.forEach { letter ->
            appendCodePoint(REGIONAL_INDICATOR_A + (letter - ASCII_A))
        }
    }
}

/**
 * Localized country name, e.g. `DE` in English -> `Germany`, in German -> `Deutschland`.
 * Falls back to the upper-cased code when the JVM has no name for it.
 */
fun countryDisplayName(countryCode: String, locale: Locale = Locale.getDefault()): String {
    val code = normalizeCountryCode(countryCode) ?: return countryCode.trim()
    val name = Locale.Builder().setRegion(code).build().getDisplayCountry(locale)
    return if (name.isBlank() || name == code) code else name
}

/** Flag plus localized name, e.g. `🇩🇪 Germany`. Falls back to the raw value when invalid. */
fun countryLabel(countryCode: String, locale: Locale = Locale.getDefault()): String {
    val flag = countryFlagEmoji(countryCode)
    val name = countryDisplayName(countryCode, locale)
    return if (flag.isEmpty()) name else "$flag $name"
}

private fun normalizeCountryCode(countryCode: String): String? {
    val code = countryCode.trim().uppercase(Locale.ROOT)
    val isValid = code.length == ISO_CODE_LENGTH && code.all { it in ASCII_A..ASCII_Z }
    return if (isValid) code else null
}
