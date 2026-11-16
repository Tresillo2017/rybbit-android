package app.rybbitclient.feature.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.rybbitclient.core.model.AuthType
import app.rybbitclient.core.model.Instance
import app.rybbitclient.core.model.InstanceKind
import app.rybbitclient.core.network.ApiException
import app.rybbitclient.core.network.Credential
import app.rybbitclient.core.network.InvalidUrlException
import app.rybbitclient.core.network.UrlNormalizer
import app.rybbitclient.core.storage.InstanceRepository
import java.io.IOException
import java.util.UUID
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private const val MISSING_ADDRESS = "Enter the address of your Rybbit instance."
private const val MISSING_KEY = "Enter an API key."
private const val MISSING_EMAIL = "Enter the email address of your account."
private const val MISSING_PASSWORD = "Enter your password."
private const val SAVE_FAILED = "Couldn't save this server. Try again."

/** The signed-in result of the credentials step. */
private data class SignedIn(val credential: Credential, val accountLabel: String?)

/** Parsed address: a normalized base URL, or a message explaining why it is not usable. */
private data class ParsedAddress(val baseUrl: String?, val error: String?)

/**
 * Drives [AddInstanceScreen]. Validates input, runs the probe and sign-in steps, then saves the instance
 * and makes it active. Emits one [completed] event on success; navigation decides where to go next.
 *
 * [newId] and [now] are injected so tests get stable instance ids and timestamps.
 */
class AddInstanceViewModel(
    isCloud: Boolean,
    private val authenticator: InstanceAuthenticator,
    private val instances: InstanceRepository,
    private val newId: () -> String = { UUID.randomUUID().toString() },
    private val now: () -> Long = System::currentTimeMillis,
) : ViewModel() {

    private val mutableState = MutableStateFlow(initialState(isCloud))
    val uiState: StateFlow<AddInstanceUiState> = mutableState.asStateFlow()

    private val completions = Channel<Unit>(Channel.CONFLATED)

    /** Fires once after the instance is saved and made active. */
    val completed: Flow<Unit> = completions.receiveAsFlow()

    fun onServerInputChanged(input: String) {
        val parsed = parseAddress(input)
        mutableState.update { state ->
            state.copy(
                serverInput = input,
                baseUrl = parsed.baseUrl,
                urlError = parsed.error,
                serverCheck = ServerCheck.NotChecked,
                serverProblem = null,
                displayName = if (state.isDisplayNameEdited) state.displayName else defaultDisplayName(parsed.baseUrl),
            )
        }
    }

    fun onAuthMethodChanged(method: AuthMethod) = mutableState.update { it.copy(authMethod = method, formError = null) }

    fun onApiKeyChanged(key: String) = mutableState.update { it.copy(apiKey = key, formError = null) }

    fun onToggleApiKeyVisible() = mutableState.update { it.copy(isApiKeyVisible = !it.isApiKeyVisible) }

    fun onEmailChanged(email: String) = mutableState.update { it.copy(email = email, formError = null) }

    fun onPasswordChanged(password: String) = mutableState.update { it.copy(password = password, formError = null) }

    fun onTogglePasswordVisible() = mutableState.update { it.copy(isPasswordVisible = !it.isPasswordVisible) }

    fun onDisplayNameChanged(name: String) = mutableState.update { it.copy(displayName = name, isDisplayNameEdited = true) }

    /** The "Check server" button: probes the address without signing in. */
    fun onCheckServer() {
        val baseUrl = mutableState.value.baseUrl
        if (baseUrl == null) {
            mutableState.update { it.copy(urlError = it.urlError ?: MISSING_ADDRESS) }
            return
        }
        viewModelScope.launch { checkServer(baseUrl) }
    }

    /** The submit button. Validates locally first, then probes (unless already checked), signs in and saves. */
    fun submit() {
        val state = mutableState.value
        if (state.isSubmitting) return
        val baseUrl = state.baseUrl
        if (baseUrl == null) {
            mutableState.update { it.copy(urlError = it.urlError ?: MISSING_ADDRESS) }
            return
        }
        val validation = credentialError(state)
        if (validation != null) {
            mutableState.update { it.copy(formError = validation) }
            return
        }
        viewModelScope.launch { signIn(state, baseUrl) }
    }

    private fun initialState(isCloud: Boolean): AddInstanceUiState {
        val baseUrl = if (isCloud) AddInstanceUiState.CLOUD_BASE_URL else null
        return AddInstanceUiState(
            isCloud = isCloud,
            baseUrl = baseUrl,
            displayName = defaultDisplayName(baseUrl, isCloud),
        )
    }

    private suspend fun checkServer(baseUrl: String): Boolean {
        mutableState.update { it.copy(serverCheck = ServerCheck.Checking, serverProblem = null, formError = null) }
        return try {
            authenticator.probe(baseUrl)
            updateCheckResult(baseUrl, ServerCheck.Reachable, problem = null)
            true
        } catch (error: ApiException) {
            updateCheckResult(baseUrl, ServerCheck.Failed, describeAddServerError(error, AddServerStage.Probe))
            false
        }
    }

    /** Applies a check result only if the address is still the one that was checked. */
    private fun updateCheckResult(baseUrl: String, check: ServerCheck, problem: String?) {
        mutableState.update { state ->
            if (state.baseUrl != baseUrl) state else state.copy(serverCheck = check, serverProblem = problem)
        }
    }

    private suspend fun signIn(input: AddInstanceUiState, baseUrl: String) {
        setProgress("Checking server…")
        var stage = AddServerStage.Probe
        try {
            // Cloud is fixed and always reachable, so only self-hosted addresses are probed.
            if (!input.isCloud && input.serverCheck != ServerCheck.Reachable) {
                authenticator.probe(baseUrl)
            }
            stage = AddServerStage.SignIn
            setProgress("Signing in…")
            val signedIn = authenticate(input, baseUrl)
            setProgress("Saving…")
            save(input, baseUrl, signedIn)
            mutableState.update { it.copy(isSubmitting = false, progress = null, serverCheck = ServerCheck.Reachable) }
            completions.trySend(Unit)
        } catch (error: ApiException) {
            fail(describeAddServerError(error, stage), stage, baseUrl)
        } catch (error: IllegalArgumentException) {
            fail(SAVE_FAILED, AddServerStage.SignIn, baseUrl)
        } catch (error: IOException) {
            // Keystore and DataStore write failures; the form stays editable so the user can retry.
            fail(SAVE_FAILED, AddServerStage.SignIn, baseUrl)
        }
    }

    private suspend fun authenticate(input: AddInstanceUiState, baseUrl: String): SignedIn = when (input.authMethod) {
        AuthMethod.ApiKey -> {
            val key = input.apiKey.trim()
            authenticator.verifyApiKey(baseUrl, key)
            SignedIn(Credential.ApiKey(key), accountLabel = null)
        }
        AuthMethod.Password -> {
            val email = input.email.trim()
            val credential = authenticator.signInWithPassword(baseUrl, email, input.password)
            SignedIn(credential, accountLabel = email)
        }
    }

    private suspend fun save(input: AddInstanceUiState, baseUrl: String, signedIn: SignedIn) {
        val instance = Instance(
            id = newId(),
            displayName = input.displayName.trim().ifEmpty { defaultDisplayName(baseUrl, input.isCloud) },
            baseUrl = baseUrl,
            kind = if (input.isCloud || baseUrl == AddInstanceUiState.CLOUD_BASE_URL) {
                InstanceKind.Cloud
            } else {
                InstanceKind.SelfHosted
            },
            authType = when (signedIn.credential) {
                is Credential.ApiKey -> AuthType.ApiKey
                is Credential.Session -> AuthType.Session
            },
            accountLabel = signedIn.accountLabel,
            createdAtMillis = now(),
        )
        instances.add(instance, signedIn.credential)
        instances.setActive(instance.id)
    }

    private fun setProgress(message: String) {
        mutableState.update { it.copy(isSubmitting = true, progress = message, formError = null) }
    }

    private fun fail(message: String, stage: AddServerStage, baseUrl: String) {
        mutableState.update { state ->
            when (stage) {
                AddServerStage.Probe -> state.copy(
                    isSubmitting = false,
                    progress = null,
                    serverCheck = if (state.baseUrl == baseUrl) ServerCheck.Failed else state.serverCheck,
                    serverProblem = if (state.baseUrl == baseUrl) message else state.serverProblem,
                )
                AddServerStage.SignIn -> state.copy(isSubmitting = false, progress = null, formError = message)
            }
        }
    }

    private fun defaultDisplayName(baseUrl: String?, isCloud: Boolean = false): String = when {
        isCloud -> AddInstanceUiState.CLOUD_DISPLAY_NAME
        baseUrl == null -> ""
        else -> hostOf(baseUrl).orEmpty()
    }

    private fun parseAddress(input: String): ParsedAddress {
        if (input.isBlank()) return ParsedAddress(baseUrl = null, error = null)
        return try {
            ParsedAddress(baseUrl = UrlNormalizer.normalize(input).baseUrl, error = null)
        } catch (error: InvalidUrlException) {
            ParsedAddress(baseUrl = null, error = error.message ?: "That doesn't look like a valid address.")
        }
    }

    private companion object {
        /** Returns the message for the first missing or malformed credential field, or null when valid. */
        fun credentialError(state: AddInstanceUiState): String? = when (state.authMethod) {
            AuthMethod.ApiKey -> if (state.apiKey.isBlank()) MISSING_KEY else null
            AuthMethod.Password -> when {
                state.email.isBlank() || !state.email.contains('@') -> MISSING_EMAIL
                state.password.isEmpty() -> MISSING_PASSWORD
                else -> null
            }
        }
    }
}
