package app.rybbitclient.feature.settings

import app.rybbitclient.feature.overview.RANGE_PRESETS
import app.rybbitclient.core.ui.theme.previewColor
import app.rybbitclient.core.storage.ThemeMode
import app.rybbitclient.core.storage.AccentColor
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.alpha
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SegmentedButton
import androidx.compose.material.icons.outlined.RestartAlt
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.background
import androidx.compose.animation.core.animateDpAsState
import app.rybbitclient.core.ui.haptics.rememberHaptics
import androidx.compose.ui.semantics.Role
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Switch
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.layout.size
import app.rybbitclient.core.ui.components.hapticClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.Dns
import androidx.compose.material.icons.outlined.Cloud
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.rybbitclient.BuildConfig
import app.rybbitclient.core.model.AuthType
import app.rybbitclient.core.model.Instance
import app.rybbitclient.core.model.InstanceKind
import app.rybbitclient.core.ui.theme.Spacing
import app.rybbitclient.core.ui.theme.rybbitColors
import app.rybbitclient.feature.onboarding.hostOf

private const val DOCS_URL = "https://rybbit.com/docs"
private const val SHAPE_CORNER_DP = 6

/** One saved server: switches on tap, and has a menu for rename, re-authenticate and remove. */
@Composable
fun ServerRow(
    instance: Instance,
    isActive: Boolean,
    enabled: Boolean,
    onSwitch: () -> Unit,
    onRename: () -> Unit,
    onReauthenticate: () -> Unit,
    onRemove: () -> Unit,
) {
    ListItem(
        headlineContent = { ServerHeadline(instance) },
        supportingContent = { Text(serverSummary(instance), maxLines = 2, overflow = TextOverflow.Ellipsis) },
        leadingContent = {
            Icon(
                imageVector = if (instance.kind == InstanceKind.Cloud) Icons.Outlined.Cloud else Icons.Outlined.Dns,
                contentDescription = null,
            )
        },
        trailingContent = {
            ServerRowActions(
                name = instance.displayName,
                isActive = isActive,
                enabled = enabled,
                onRename = onRename,
                onReauthenticate = onReauthenticate,
                onRemove = onRemove,
            )
        },
        modifier = Modifier.hapticClickable(enabled = enabled && !isActive, onClickLabel = "Make active", onClick = onSwitch),
    )
}

@Composable
private fun ServerHeadline(instance: Instance) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = instance.displayName,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f, fill = false),
        )
        Spacer(Modifier.width(Spacing.sm))
        KindBadge(instance.kind)
    }
}

@Composable
private fun KindBadge(kind: InstanceKind) {
    val label = if (kind == InstanceKind.Cloud) "Cloud" else "Self-hosted"
    Surface(
        shape = androidx.compose.foundation.shape.RoundedCornerShape(SHAPE_CORNER_DP.dp),
        color = MaterialTheme.colorScheme.secondaryContainer,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSecondaryContainer,
            modifier = Modifier.padding(horizontal = Spacing.xs, vertical = Spacing.xxs),
        )
    }
}

/** Host, credential type and a warning for plain http://. */
private fun serverSummary(instance: Instance): String {
    val host = hostOf(instance.baseUrl).orEmpty()
    val auth = when (instance.authType) {
        AuthType.ApiKey -> "API key"
        AuthType.Session -> "Session sign-in"
    }
    val insecure = if (instance.isInsecure) " · Not encrypted" else ""
    return "$host · $auth$insecure"
}

@Composable
private fun ServerRowActions(
    name: String,
    isActive: Boolean,
    enabled: Boolean,
    onRename: () -> Unit,
    onReauthenticate: () -> Unit,
    onRemove: () -> Unit,
) {
    var menuOpen by remember { mutableStateOf(false) }
    Row(verticalAlignment = Alignment.CenterVertically) {
        if (isActive) {
            Icon(
                imageVector = Icons.Filled.Check,
                contentDescription = "Active server",
                tint = MaterialTheme.rybbitColors.positive,
            )
        }
        Box {
            IconButton(onClick = { menuOpen = true }, enabled = enabled) {
                Icon(Icons.Outlined.MoreVert, contentDescription = "Options for $name")
            }
            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                DropdownMenuItem(
                    text = { Text("Rename") },
                    onClick = {
                        menuOpen = false
                        onRename()
                    },
                )
                DropdownMenuItem(
                    text = { Text("Re-authenticate") },
                    onClick = {
                        menuOpen = false
                        onReauthenticate()
                    },
                )
                DropdownMenuItem(
                    text = { Text("Remove", color = MaterialTheme.colorScheme.error) },
                    onClick = {
                        menuOpen = false
                        onRemove()
                    },
                )
            }
        }
    }
}

@Composable
fun AboutVersionRow() {
    ListItem(
        headlineContent = { Text("Version") },
        supportingContent = { Text(BuildConfig.VERSION_NAME) },
        leadingContent = { Icon(Icons.Outlined.Info, contentDescription = null) },
    )
}

@Composable
fun AboutDocsRow() {
    val uriHandler = LocalUriHandler.current
    ListItem(
        headlineContent = { Text("Documentation") },
        supportingContent = { Text("rybbit.com/docs") },
        leadingContent = { Icon(Icons.Outlined.Link, contentDescription = null) },
        modifier = Modifier.hapticClickable(onClickLabel = "Open the Rybbit documentation") { uriHandler.openUri(DOCS_URL) },
    )
}

@Composable
fun AboutOpenSourceRow() {
    ListItem(
        headlineContent = { Text("Open source") },
        supportingContent = { Text("Rybbit Client is open-source software. Rybbit itself is open source too.") },
        leadingContent = { Icon(Icons.Outlined.Code, contentDescription = null) },
    )
}

/**
 * A preference row with a trailing switch. The whole row toggles, the switch shows a check when on,
 * and the change gives a toggle haptic.
 */
@Composable
fun SettingsSwitchRow(
    title: String,
    supporting: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    enabled: Boolean = true,
) {
    val haptics = rememberHaptics()
    val change: (Boolean) -> Unit = { value ->
        haptics.toggle(value)
        onCheckedChange(value)
    }
    ListItem(
        headlineContent = { Text(title) },
        supportingContent = { Text(supporting) },
        trailingContent = {
            Switch(
                checked = checked,
                onCheckedChange = null,
                enabled = enabled,
                thumbContent = if (checked) {
                    { Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(SwitchDefaults.IconSize)) }
                } else {
                    null
                },
            )
        },
        modifier = Modifier.toggleable(value = checked, enabled = enabled, role = Role.Switch, onValueChange = change),
    )
}

/** Light / dark / follow-system selector, as a single-choice segmented row under a title. */
@Composable
fun ThemeModeRow(selected: ThemeMode, onSelect: (ThemeMode) -> Unit) {
    val haptics = rememberHaptics()
    val modes = ThemeMode.entries
    Column(modifier = Modifier.padding(horizontal = Spacing.screenGutter, vertical = Spacing.sm)) {
        Text("Theme", style = MaterialTheme.typography.bodyLarge)
        Spacer(Modifier.height(Spacing.sm))
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            modes.forEachIndexed { index, mode ->
                SegmentedButton(
                    selected = mode == selected,
                    onClick = {
                        if (mode != selected) haptics.selection()
                        onSelect(mode)
                    },
                    shape = SegmentedButtonDefaults.itemShape(index = index, count = modes.size),
                ) {
                    Text(mode.label)
                }
            }
        }
    }
}

/**
 * A row of colour swatches. The chosen one morphs from a circle to a rounded square and shows a check; the
 * shape change is a spatial spring. [enabled] is false while dynamic colour overrides the accent.
 */
@Composable
fun AccentPickerRow(
    selected: AccentColor,
    enabled: Boolean,
    isDark: Boolean,
    onSelect: (AccentColor) -> Unit,
) {
    Column(modifier = Modifier.padding(horizontal = Spacing.screenGutter, vertical = Spacing.sm)) {
        Text("Accent color", style = MaterialTheme.typography.bodyLarge)
        Text(
            text = if (enabled) selected.label else "Turn off dynamic color to choose an accent",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(Spacing.sm))
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.md)) {
            AccentColor.entries.forEach { accent ->
                AccentSwatch(
                    accent = accent,
                    selected = accent == selected,
                    enabled = enabled,
                    isDark = isDark,
                    onClick = { onSelect(accent) },
                )
            }
        }
    }
}

@Composable
private fun AccentSwatch(
    accent: AccentColor,
    selected: Boolean,
    enabled: Boolean,
    isDark: Boolean,
    onClick: () -> Unit,
) {
    val haptics = rememberHaptics()
    val corner by animateDpAsState(
        targetValue = if (selected) SWATCH_SELECTED_CORNER_DP.dp else SWATCH_SIZE_DP.dp / 2,
        animationSpec = MaterialTheme.motionScheme.fastSpatialSpec(),
        label = "swatchCorner",
    )
    val color = accent.previewColor(dark = isDark)
    Box(
        modifier = Modifier
            .size(SWATCH_SIZE_DP.dp)
            .alpha(if (enabled) 1f else DISABLED_ALPHA)
            .clip(RoundedCornerShape(corner))
            .background(color)
            .selectable(
                selected = selected,
                enabled = enabled,
                role = Role.RadioButton,
                onClick = {
                    if (!selected) haptics.selection()
                    onClick()
                },
            )
            .semantics { contentDescription = accent.label },
        contentAlignment = Alignment.Center,
    ) {
        if (selected) {
            Icon(
                imageVector = Icons.Filled.Check,
                contentDescription = null,
                tint = if (color.luminance() > LIGHT_SWATCH_LUMINANCE) Color.Black else Color.White,
            )
        }
    }
}

/** Opens a menu of the date-range presets; the choice is the range the app starts on. */
@Composable
fun DefaultRangeRow(
    selectedKey: String,
    onSelect: (String) -> Unit,
) {
    val haptics = rememberHaptics()
    var open by remember { mutableStateOf(false) }
    val selectedLabel = RANGE_PRESETS.firstOrNull { it.key == selectedKey }?.label ?: selectedKey
    Box {
        ListItem(
            headlineContent = { Text("Default date range") },
            supportingContent = { Text(selectedLabel) },
            leadingContent = { Icon(Icons.Outlined.CalendarMonth, contentDescription = null) },
            modifier = Modifier.hapticClickable(onClickLabel = "Choose default date range") { open = true },
        )
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            RANGE_PRESETS.forEach { preset ->
                DropdownMenuItem(
                    text = { Text(preset.label) },
                    trailingIcon = {
                        if (preset.key == selectedKey) Icon(Icons.Filled.Check, contentDescription = "Selected")
                    },
                    onClick = {
                        open = false
                        if (preset.key != selectedKey) haptics.selection()
                        onSelect(preset.key)
                    },
                )
            }
        }
    }
}

/** Restores every display preference to its default. Servers are not touched. */
@Composable
fun ResetPreferencesRow(onReset: () -> Unit) {
    ListItem(
        headlineContent = { Text("Reset appearance") },
        supportingContent = { Text("Restore theme, colors, motion and chart options. Servers are kept.") },
        leadingContent = { Icon(Icons.Outlined.RestartAlt, contentDescription = null) },
        modifier = Modifier.hapticClickable(onClickLabel = "Reset appearance to defaults", onClick = onReset),
    )
}

private const val SWATCH_SIZE_DP = 44
private const val SWATCH_SELECTED_CORNER_DP = 14
private const val DISABLED_ALPHA = 0.38f
private const val LIGHT_SWATCH_LUMINANCE = 0.5f
