package app.rybbitclient.feature.settings

import app.rybbitclient.core.storage.ThemeMode
import app.rybbitclient.core.storage.AppSettings
import app.rybbitclient.core.storage.AppPreferences
import app.rybbitclient.core.storage.AccentColor
import kotlinx.coroutines.CoroutineScope
import androidx.compose.foundation.isSystemInDarkTheme
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope
import android.os.Build
import app.rybbitclient.core.ui.components.hapticClickable
import app.rybbitclient.core.ui.components.rememberCollapsingBarBehavior
import app.rybbitclient.core.ui.components.ExpressiveBackTopBar
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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
 * - [onActiveInstanceChanged]: The active server was switched; screens bound to the previous server must close.
 * - [onAllInstancesRemoved]: Called when the last instance is removed; navigate to onboarding.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onAddInstance: () -> Unit,
    onActiveInstanceChanged: () -> Unit,
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

    val scrollBehavior = rememberCollapsingBarBehavior()
    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            ExpressiveBackTopBar(
                title = "Settings",
                onBack = onBack,
                scrollBehavior = scrollBehavior,
            )
        },
    ) { padding ->
        val modifier = Modifier.fillMaxSize().padding(padding)
        when (val content = state) {
            SettingsUiState.Loading -> LoadingView(modifier = modifier)
            is SettingsUiState.Content -> SettingsList(content, modifier, viewModel, onAddInstance, onActiveInstanceChanged)
        }
    }
}

@Composable
private fun SettingsList(
    state: SettingsUiState.Content,
    modifier: Modifier,
    viewModel: SettingsViewModel,
    onAddInstance: () -> Unit,
    onActiveInstanceChanged: () -> Unit,
) {
    var dialog by remember { mutableStateOf<ServerDialog?>(null) }
    val preferences = LocalAppContainer.current.appPreferences
    val scope = rememberCoroutineScope()
    val settings by preferences.settings.collectAsStateWithLifecycle(initialValue = AppSettings())
    val actions = remember(preferences, scope) { SettingsActions(preferences, scope) }
    val isDark = when (settings.themeMode) {
        ThemeMode.System -> isSystemInDarkTheme()
        ThemeMode.Light -> false
        ThemeMode.Dark -> true
    }
    LazyColumn(modifier = modifier) {
        serverSection(
            state,
            onSwitch = { id -> viewModel.switchTo(id, onSwitched = onActiveInstanceChanged) },
            onAction = { dialog = it },
            enabled = !state.isWorking,
        )
        item(key = "add-server") {
            ListItem(
                headlineContent = { Text("Add server") },
                supportingContent = { Text("Connect to Rybbit Cloud or another server") },
                leadingContent = { Icon(Icons.Outlined.Add, contentDescription = null) },
                modifier = Modifier.hapticClickable(enabled = !state.isWorking, onClick = onAddInstance),
            )
        }
        appearanceSection(settings, isDark, actions)
        dashboardSection(settings, actions)
        feedbackSection(settings, actions)
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

private fun LazyListScope.appearanceSection(settings: AppSettings, isDark: Boolean, actions: SettingsActions) {
    item(key = "appearance-header") { SectionHeader("Appearance") }
    item(key = "appearance-theme") { ThemeModeRow(settings.themeMode, actions::setThemeMode) }
    val supportsDynamic = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    if (supportsDynamic) {
        item(key = "appearance-dynamic") {
            SettingsSwitchRow(
                title = "Dynamic color",
                supporting = "Match the colors of your wallpaper",
                checked = settings.dynamicColor,
                onCheckedChange = actions::setDynamicColor,
            )
        }
    }
    item(key = "appearance-accent") {
        AccentPickerRow(
            selected = settings.accent,
            enabled = !(supportsDynamic && settings.dynamicColor),
            isDark = isDark,
            onSelect = actions::setAccent,
        )
    }
    item(key = "appearance-black") {
        SettingsSwitchRow(
            title = "Pure black",
            supporting = "True black backgrounds in dark theme, easier on OLED screens",
            checked = settings.pureBlack,
            onCheckedChange = actions::setPureBlack,
        )
    }
}

private fun LazyListScope.dashboardSection(settings: AppSettings, actions: SettingsActions) {
    item(key = "dashboard-header") { SectionHeader("Dashboard") }
    item(key = "dashboard-range") { DefaultRangeRow(settings.defaultRangeKey, actions::setDefaultRange) }
    item(key = "dashboard-smooth") {
        SettingsSwitchRow(
            title = "Smooth chart lines",
            supporting = "Curved lines instead of straight segments between points",
            checked = settings.smoothCharts,
            onCheckedChange = actions::setSmoothCharts,
        )
    }
}

private fun LazyListScope.feedbackSection(settings: AppSettings, actions: SettingsActions) {
    item(key = "feedback-header") { SectionHeader("Feedback and motion") }
    item(key = "feedback-haptics") {
        SettingsSwitchRow(
            title = "Haptic feedback",
            supporting = "Subtle vibrations as you tap, scrub and refresh",
            checked = settings.hapticsEnabled,
            onCheckedChange = actions::setHaptics,
        )
    }
    item(key = "feedback-motion") {
        SettingsSwitchRow(
            title = "Reduce motion",
            supporting = "Fade between screens and stop looping animations",
            checked = settings.reduceMotion,
            onCheckedChange = actions::setReduceMotion,
        )
    }
    item(key = "feedback-reset") { ResetPreferencesRow(onReset = actions::resetAll) }
}

/** Persists preference changes from the UI; each call is fire-and-forget on the screen's scope. */
private class SettingsActions(
    private val preferences: AppPreferences,
    private val scope: CoroutineScope,
) {
    fun setThemeMode(mode: ThemeMode) { scope.launch { preferences.setThemeMode(mode) } }
    fun setDynamicColor(enabled: Boolean) { scope.launch { preferences.setDynamicColor(enabled) } }
    fun setAccent(accent: AccentColor) { scope.launch { preferences.setAccent(accent) } }
    fun setPureBlack(enabled: Boolean) { scope.launch { preferences.setPureBlack(enabled) } }
    fun setHaptics(enabled: Boolean) { scope.launch { preferences.setHapticsEnabled(enabled) } }
    fun setReduceMotion(enabled: Boolean) { scope.launch { preferences.setReduceMotion(enabled) } }
    fun setSmoothCharts(enabled: Boolean) { scope.launch { preferences.setSmoothCharts(enabled) } }
    fun setDefaultRange(key: String) { scope.launch { preferences.setDefaultRange(key) } }
    fun resetAll() { scope.launch { preferences.resetToDefaults() } }
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
