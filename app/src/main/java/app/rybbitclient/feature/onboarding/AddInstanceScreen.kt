package app.rybbitclient.feature.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.autofill.ContentType
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentType
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import app.rybbitclient.core.ui.theme.Spacing
import app.rybbitclient.di.LocalAppContainer

/**
 * Adds a Rybbit instance. Cloud fixes the address to app.rybbit.io; self-hosted takes any address and
 * normalizes it live. On success the instance is saved and made active, then [onInstanceAdded] runs.
 *
 * Parameters:
 * - [isCloud]: True for the Cloud preset, false for a self-hosted server.
 * - [onInstanceAdded]: Called after the instance and credential are saved and selected.
 * - [onBack]: Back navigation.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddInstanceScreen(
    isCloud: Boolean,
    onInstanceAdded: () -> Unit,
    onBack: () -> Unit,
) {
    val container = LocalAppContainer.current
    val viewModel: AddInstanceViewModel = viewModel(key = "add-instance-$isCloud") {
        AddInstanceViewModel(
            isCloud = isCloud,
            authenticator = AuthServiceAuthenticator(container.authService),
            instances = container.instanceRepository,
        )
    }
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(viewModel) { viewModel.completed.collect { onInstanceAdded() } }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isCloud) "Rybbit Cloud" else "Add server") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        AddInstanceForm(state = state, viewModel = viewModel, modifier = Modifier.padding(padding))
    }
}

@Composable
private fun AddInstanceForm(
    state: AddInstanceUiState,
    viewModel: AddInstanceViewModel,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = Spacing.screenGutter, vertical = Spacing.lg),
        verticalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        if (state.isCloud) {
            Text(
                text = "Sign in to Rybbit Cloud at ${AddInstanceUiState.CLOUD_BASE_URL}.",
                style = MaterialTheme.typography.bodyMedium,
            )
        } else {
            ServerAddressField(state, viewModel)
        }
        ServerCheckRow(state = state, onCheck = viewModel::onCheckServer)
        AuthMethodSwitch(selected = state.authMethod, onSelect = viewModel::onAuthMethodChanged)
        when (state.authMethod) {
            AuthMethod.ApiKey -> ApiKeyFields(state, viewModel)
            AuthMethod.Password -> PasswordFields(state, viewModel)
        }
        DisplayNameField(state, viewModel)
        FormErrorText(state.formError)
        SubmitButton(state = state, onSubmit = viewModel::submit)
    }
}

@Composable
private fun ServerAddressField(state: AddInstanceUiState, viewModel: AddInstanceViewModel) {
    val supporting = state.urlError ?: state.previewText ?: "For example analytics.example.com"
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        OutlinedTextField(
            value = state.serverInput,
            onValueChange = viewModel::onServerInputChanged,
            label = { Text("Server address") },
            singleLine = true,
            isError = state.urlError != null,
            supportingText = { Text(supporting) },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Uri,
                capitalization = KeyboardCapitalization.None,
                imeAction = ImeAction.Done,
            ),
            keyboardActions = KeyboardActions(onDone = { viewModel.onCheckServer() }),
            modifier = Modifier.fillMaxWidth(),
        )
        if (state.isInsecure) InsecureTrafficBanner()
    }
}

@Composable
private fun ApiKeyFields(state: AddInstanceUiState, viewModel: AddInstanceViewModel) {
    Text(
        text = "Create a key in Rybbit under Settings → Account → Personal API Keys. " +
            "For a key shared across an organization, use Organization API Keys.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    OutlinedTextField(
        value = state.apiKey,
        onValueChange = viewModel::onApiKeyChanged,
        label = { Text("API key") },
        singleLine = true,
        visualTransformation = secretTransformation(state.isApiKeyVisible),
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Password,
            capitalization = KeyboardCapitalization.None,
            imeAction = ImeAction.Done,
        ),
        keyboardActions = KeyboardActions(onDone = { viewModel.submit() }),
        trailingIcon = {
            VisibilityToggle(
                visible = state.isApiKeyVisible,
                subject = "API key",
                onToggle = viewModel::onToggleApiKeyVisible,
            )
        },
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun PasswordFields(state: AddInstanceUiState, viewModel: AddInstanceViewModel) {
    Text(
        text = "We sign in once with your email and password, then create an API key named for this device. " +
            "The app keeps only that key, never your password.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    val focus = LocalFocusManager.current
    OutlinedTextField(
        value = state.email,
        onValueChange = viewModel::onEmailChanged,
        label = { Text("Email") },
        singleLine = true,
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Email,
            capitalization = KeyboardCapitalization.None,
            imeAction = ImeAction.Next,
        ),
        keyboardActions = KeyboardActions(onNext = { focus.moveFocus(FocusDirection.Down) }),
        modifier = Modifier.fillMaxWidth().semantics { contentType = ContentType.EmailAddress },
    )
    OutlinedTextField(
        value = state.password,
        onValueChange = viewModel::onPasswordChanged,
        label = { Text("Password") },
        singleLine = true,
        visualTransformation = secretTransformation(state.isPasswordVisible),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(onDone = { viewModel.submit() }),
        trailingIcon = {
            VisibilityToggle(
                visible = state.isPasswordVisible,
                subject = "password",
                onToggle = viewModel::onTogglePasswordVisible,
            )
        },
        modifier = Modifier.fillMaxWidth().semantics { contentType = ContentType.Password },
    )
}

@Composable
private fun DisplayNameField(state: AddInstanceUiState, viewModel: AddInstanceViewModel) {
    OutlinedTextField(
        value = state.displayName,
        onValueChange = viewModel::onDisplayNameChanged,
        label = { Text("Display name") },
        supportingText = { Text("Shown in your server list") },
        singleLine = true,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun FormErrorText(message: String?) {
    if (message == null) return
    Text(
        text = message,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.error,
        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Assertive },
    )
}

@Composable
private fun SubmitButton(state: AddInstanceUiState, onSubmit: () -> Unit) {
    Button(
        onClick = onSubmit,
        enabled = !state.isSubmitting,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = Spacing.minTouchTarget),
    ) {
        if (state.isSubmitting) {
            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
            Text(text = state.progress.orEmpty(), modifier = Modifier.padding(start = Spacing.sm))
        } else {
            Text(text = if (state.isCloud) "Connect to Rybbit Cloud" else "Connect")
        }
    }
}

private fun secretTransformation(visible: Boolean): VisualTransformation =
    if (visible) VisualTransformation.None else PasswordVisualTransformation()
