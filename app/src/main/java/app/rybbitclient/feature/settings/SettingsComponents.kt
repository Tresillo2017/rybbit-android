package app.rybbitclient.feature.settings

import androidx.compose.foundation.clickable
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
        modifier = Modifier.clickable(enabled = enabled && !isActive, onClickLabel = "Make active", onClick = onSwitch),
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
        modifier = Modifier.clickable(onClickLabel = "Open the Rybbit documentation") { uriHandler.openUri(DOCS_URL) },
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
