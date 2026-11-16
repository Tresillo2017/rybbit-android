package app.rybbitclient.core.network

import kotlinx.serialization.json.Json

/** Lenient JSON configuration shared by every Rybbit request and response. Older servers differ. */
object RybbitJson {
    val json: Json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
        explicitNulls = false
    }
}
