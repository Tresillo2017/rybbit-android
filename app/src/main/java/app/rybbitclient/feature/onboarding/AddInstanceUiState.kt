package app.rybbitclient.feature.onboarding

import app.rybbitclient.core.network.UrlNormalizer

/** How the user proves who they are. Labels are shown on the segmented buttons. */
enum class AuthMethod(val label: String) {
    ApiKey("API key"),
    Password("Email & password"),
}

/** Result of the "Check server" step for the current address. Any edit to the address resets it. */
enum class ServerCheck { NotChecked, Checking, Reachable, Failed }

/**
 * Everything the add-server form shows. Immutable: the ViewModel replaces it with `copy`.
 * [baseUrl] is the normalized address (fixed for Cloud), or null while the typed address is blank or invalid.
 */
data class AddInstanceUiState(
    val isCloud: Boolean,
    val serverInput: String = "",
    val baseUrl: String? = null,
    val urlError: String? = null,
    val serverCheck: ServerCheck = ServerCheck.NotChecked,
    val serverProblem: String? = null,
    val authMethod: AuthMethod = AuthMethod.ApiKey,
    val apiKey: String = "",
    val isApiKeyVisible: Boolean = false,
    val email: String = "",
    val password: String = "",
    val isPasswordVisible: Boolean = false,
    val displayName: String = "",
    val isDisplayNameEdited: Boolean = false,
    val isSubmitting: Boolean = false,
    val progress: String? = null,
    val formError: String? = null,
) {
    /** Plain-http addresses must show the insecure-traffic banner. */
    val isInsecure: Boolean get() = baseUrl?.startsWith("http://") == true

    /** Live preview of the normalized address, shown under the server field. */
    val previewText: String? get() = baseUrl?.takeIf { !isCloud }?.let { "Will connect to $it" }

    companion object {
        /** The fixed Rybbit Cloud base URL. */
        const val CLOUD_BASE_URL = UrlNormalizer.CLOUD_BASE_URL
        const val CLOUD_DISPLAY_NAME = "Rybbit Cloud"
    }
}
