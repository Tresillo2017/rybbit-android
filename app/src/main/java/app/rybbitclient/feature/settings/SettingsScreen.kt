package app.rybbitclient.feature.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import app.rybbitclient.core.ui.components.LoadingView
import app.rybbitclient.core.ui.theme.Spacing
import app.rybbitclient.di.LocalAppContainer
import app.rybbitclient.feature.onboarding.AuthServiceAuthenticator
import app.rybbitclient.core.model.Instance

/** The dialog currently open for one server. */
private sealed interface ServerDialog {
    val instance: Instance

    data class Rename(override val instance: Instance) : ServerDialog
    data class Reauthenticate(override val instance: Instance) : ServerDialog
    data class Remove(override val instance: Instance) : ServerDialog
}

/**
 * Instance management: the server list (switch, rename, re-authenticate, remove), "Add server", and About.
 * Removing the last server leaves for onboarding through [onAllInstancesRemoved].
 *
 * Parameters:
 * - [onBack]: Back navigation.
 * - [onAddInstance]: Open the add-instance flow.
 * - [onAllInstancesRemoved]: Called when the last instance is removed; navigate to onboarding.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onAddInstance: () -> Unit,
    onAllInstancesRemoved: () -> Unit,
) {
    val container = LocalAppContainer.current
    val viewModel: SettingsViewModel = viewModel(key = "settings") {
        SettingsViewModel(
            instances = container.instanceRepository,
            authenticator = AuthServiceAuthenticator(container.authService),
            remover = container::removeInstance,
        )
    }
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val currentOnAllRemoved by rememberUpdatedState(onAllInstancesRemoved)
    LaunchedEffect(viewModel) { viewModel.allInstancesRemoved.collect { currentOnAllRemoved() } }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        val modifier = Modifier.fillMaxSize().padding(padding)
        when (val content = state) {
            SettingsUiState.Loading -> LoadingView(modifier = modifier)
            is SettingsUiState.Content -> SettingsList(content, modifier, viewModel, onAddInstance)
        }
    }
}

@Composable
private fun SettingsList(
    state: SettingsUiState.Content,
    modifier: Modifier,
    viewModel: SettingsViewModel,
    onAddInstance: () -> Unit,
) {
    var dialog by remember { mutableStateOf<ServerDialog?>(null) }
    LazyColumn(modifier = modifier) {
        serverSection(state, onSwitch = viewModel::switchTo, onAction = { dialog = it }, enabled = !state.isWorking)
        item(key = "add-server") {
            ListItem(
                headlineContent = { Text("Add server") },
                supportingContent = { Text("Connect to Rybbit Cloud or another server") },
                leadingContent = { Icon(Icons.Outlined.Add, contentDescription = null) },
                modifier = Modifier.clickable(enabled = !state.isWorking, onClick = onAddInstance),
            )
        }
        aboutSection()
    }
    ServerDialogHost(
        dialog = dialog,
        state = state,
        viewModel = viewModel,
        onDismiss = {
            viewModel.clearError()
            dialog = null
        },
    )
}

@Composable
private fun ServerDialogHost(
    dialog: ServerDialog?,
    state: SettingsUiState.Content,
    viewModel: SettingsViewModel,
    onDismiss: () -> Unit,
) {
    when (dialog) {
        null -> Unit
        is ServerDialog.Rename -> RenameServerDialog(
            currentName = dialog.instance.displayName,
            onConfirm = { name ->
                viewModel.rename(dialog.instance.id, name)
                onDismiss()
            },
            onDismiss = onDismiss,
        )
        is ServerDialog.Reauthenticate -> ReauthenticateDialog(
            host = dialog.instance.baseUrl,
            isWorking = state.isWorking,
            errorMessage = state.errorMessage,
            onConfirm = { key -> viewModel.reauthenticate(dialog.instance.id, key, onDone = onDismiss) },
            onDismiss = onDismiss,
        )
        is ServerDialog.Remove -> RemoveServerDialog(
            name = dialog.instance.displayName,
            onConfirm = {
                viewModel.remove(dialog.instance.id)
                onDismiss()
            },
            onDismiss = onDismiss,
        )
    }
}

private fun LazyListScope.serverSection(
    state: SettingsUiState.Content,
    onSwitch: (String) -> Unit,
    onAction: (ServerDialog) -> Unit,
    enabled: Boolean,
) {
    item(key = "servers-header") { SectionHeader("Servers") }
    items(state.instances, key = { it.id }) { instance ->
        ServerRow(
            instance = instance,
            isActive = instance.id == state.activeInstanceId,
            enabled = enabled,
            onSwitch = { onSwitch(instance.id) },
            onRename = { onAction(ServerDialog.Rename(instance)) },
            onReauthenticate = { onAction(ServerDialog.Reauthenticate(instance)) },
            onRemove = { onAction(ServerDialog.Remove(instance)) },
        )
    }
}

private fun LazyListScope.aboutSection() {
    item(key = "about-header") { SectionHeader("About") }
    item(key = "about-version") { AboutVersionRow() }
    item(key = "about-docs") { AboutDocsRow() }
    item(key = "about-open-source") { AboutOpenSourceRow() }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier
            .padding(horizontal = Spacing.screenGutter, vertical = Spacing.sm)
            .semantics { heading() },
    )
}
