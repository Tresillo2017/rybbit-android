package app.rybbitclient.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.rybbitclient.core.model.AuthType
import app.rybbitclient.core.network.ApiException
import app.rybbitclient.core.network.Credential
import app.rybbitclient.core.storage.InstanceRepository
import app.rybbitclient.feature.onboarding.AddServerStage
import app.rybbitclient.feature.onboarding.InstanceAuthenticator
import app.rybbitclient.feature.onboarding.describeAddServerError
import java.io.IOException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Longest display name accepted by rename. */
const val MAX_DISPLAY_NAME_LENGTH = 60

private const val MISSING_KEY = "Enter an API key."

/** Background work flags shown on the screen. */
private data class Operation(val isWorking: Boolean = false, val errorMessage: String? = null)

/**
 * Drives [SettingsScreen]: lists servers, switches the active one, renames, re-authenticates with a new API key
 * and removes servers. The server list is observed from the repository, so every change shows up immediately.
 *
 * [remover] is the app-wide removal (`container::removeInstance`), so the HTTP client cache and the
 * feature-availability record are cleared too. Tests pass a lambda.
 */
class SettingsViewModel(
    private val instances: InstanceRepository,
    private val authenticator: InstanceAuthenticator,
    private val remover: suspend (String) -> Unit,
) : ViewModel() {

    private val operation = MutableStateFlow(Operation())

    val uiState: StateFlow<SettingsUiState> = combine(
        instances.instances,
        instances.activeInstanceId,
        operation,
    ) { list, activeId, op ->
        SettingsUiState.Content(
            instances = list,
            activeInstanceId = activeId,
            isWorking = op.isWorking,
            errorMessage = op.errorMessage,
        )
    }.stateIn(viewModelScope, SharingStarted.Eagerly, SettingsUiState.Loading)

    private val allRemoved = Channel<Unit>(Channel.CONFLATED)

    /** Fires after the last server is removed. The screen then leaves for onboarding. */
    val allInstancesRemoved: Flow<Unit> = allRemoved.receiveAsFlow()

    /** Makes [instanceId] active, then calls [onSwitched] so the caller can leave screens bound to the old server. */
    fun switchTo(instanceId: String, onSwitched: () -> Unit = {}) {
        viewModelScope.launch {
            instances.setActive(instanceId)
            onSwitched()
        }
    }

    /** Renames a server. Blank names are ignored; long names are cut to [MAX_DISPLAY_NAME_LENGTH]. */
    fun rename(instanceId: String, name: String) {
        val trimmed = name.trim().take(MAX_DISPLAY_NAME_LENGTH)
        if (trimmed.isEmpty()) return
        viewModelScope.launch {
            instances.instance(instanceId)?.let { current ->
                instances.update(current.copy(displayName = trimmed))
            }
        }
    }

    fun remove(instanceId: String) {
        viewModelScope.launch {
            operation.update { it.copy(isWorking = true, errorMessage = null) }
            val removed = removeOrReport(instanceId)
            operation.update { it.copy(isWorking = false) }
            if (removed && instances.instances.first().isEmpty()) allRemoved.trySend(Unit)
        }
    }

    private suspend fun removeOrReport(instanceId: String): Boolean = try {
        remover(instanceId)
        true
    } catch (failure: IOException) {
        failWith("Couldn't remove this server. Try again.")
        false
    }

    /**
     * Replaces the server's API key after checking it. [onDone] runs once the new key is saved, so the
     * dialog can close. Failures are reported through the uiState error message.
     */
    fun reauthenticate(instanceId: String, apiKey: String, onDone: () -> Unit) {
        val key = apiKey.trim()
        if (key.isEmpty()) {
            operation.update { it.copy(errorMessage = MISSING_KEY) }
            return
        }
        viewModelScope.launch { verifyAndStore(instanceId, key, onDone) }
    }

    fun clearError() {
        operation.update { it.copy(errorMessage = null) }
    }

    private suspend fun verifyAndStore(instanceId: String, key: String, onDone: () -> Unit) {
        operation.update { it.copy(isWorking = true, errorMessage = null) }
        try {
            val current = instances.instance(instanceId)
                ?: return failWith("That server no longer exists.")
            authenticator.verifyApiKey(current.baseUrl, key)
            instances.updateCredential(instanceId, Credential.ApiKey(key))
            instances.update(current.copy(authType = AuthType.ApiKey))
            operation.update { it.copy(isWorking = false) }
            onDone()
        } catch (error: ApiException) {
            failWith(describeAddServerError(error, AddServerStage.SignIn))
        } catch (error: IllegalArgumentException) {
            failWith("Couldn't save the new key. Try again.")
        } catch (error: IOException) {
            failWith("Couldn't save the new key. Try again.")
        }
    }

    private fun failWith(message: String) {
        operation.update { it.copy(isWorking = false, errorMessage = message) }
    }
}
