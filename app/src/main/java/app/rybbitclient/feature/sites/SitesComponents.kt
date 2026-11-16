package app.rybbitclient.feature.sites

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.ArrowDropDown
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.rybbitclient.feature.onboarding.hostOf

private const val AVATAR_SIZE_DP = 40
private const val MIN_TITLE_HEIGHT_DP = 48

/** The top-bar title: the active server name, which opens a menu of servers and "Add server". */
@Composable
fun InstanceSwitcher(
    switcher: InstanceSwitcherState,
    onSelectInstance: (String) -> Unit,
    onAddInstance: () -> Unit,
) {
    var menuOpen by remember { mutableStateOf(false) }
    val activeName = switcher.active?.displayName ?: "Rybbit"
    Box {
        Row(
            modifier = Modifier
                .heightIn(min = MIN_TITLE_HEIGHT_DP.dp)
                .clickable(role = Role.Button, onClickLabel = "Switch server") { menuOpen = true }
                .semantics { role = Role.Button },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(text = activeName, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.titleLarge)
            Icon(Icons.Outlined.ArrowDropDown, contentDescription = null)
        }
        DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
            switcher.instances.forEach { instance ->
                val isActive = instance.id == switcher.activeId
                DropdownMenuItem(
                    text = {
                        Column {
                            Text(instance.displayName, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(
                                text = hostOf(instance.baseUrl).orEmpty(),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    },
                    trailingIcon = {
                        if (isActive) Icon(Icons.Filled.Check, contentDescription = "Active server")
                    },
                    onClick = {
                        menuOpen = false
                        if (!isActive) onSelectInstance(instance.id)
                    },
                )
            }
            HorizontalDivider()
            DropdownMenuItem(
                text = { Text("Add server") },
                leadingIcon = { Icon(Icons.Outlined.Add, contentDescription = null) },
                onClick = {
                    menuOpen = false
                    onAddInstance()
                },
            )
        }
    }
}

/** A letter avatar standing in for a favicon. Decorative: the row's text already names the site. */
@Composable
fun SiteAvatar(name: String) {
    val letter = name.trim().firstOrNull()?.uppercaseChar()?.toString() ?: "?"
    Box(
        modifier = Modifier
            .size(AVATAR_SIZE_DP.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primaryContainer),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = letter,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
        )
    }
}
