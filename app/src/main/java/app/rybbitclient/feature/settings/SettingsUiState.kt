package app.rybbitclient.feature.settings

import app.rybbitclient.core.model.Instance

/**
 * UI state for [SettingsScreen]. [Content.isWorking] disables actions while a remove or re-login runs;
 * [Content.errorMessage] is the friendly copy for the last failed re-login.
 */
sealed interface SettingsUiState {

    data object Loading : SettingsUiState

    data class Content(
        val instances: List<Instance>,
        val activeInstanceId: String?,
        val isWorking: Boolean = false,
        val errorMessage: String? = null,
    ) : SettingsUiState
}
