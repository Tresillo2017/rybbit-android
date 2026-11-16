package app.rybbitclient.feature.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import app.rybbitclient.core.ui.theme.Spacing
import app.rybbitclient.feature.onboarding.VisibilityToggle

private const val SPINNER_DP = 18

/** Rename dialog. Save is disabled until the name has text. */
@Composable
fun RenameServerDialog(
    currentName: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var name by rememberSaveable { mutableStateOf(currentName) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Rename server") },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it.take(MAX_DISPLAY_NAME_LENGTH) },
                label = { Text("Name") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { if (name.isNotBlank()) onConfirm(name) }),
            )
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(name) }, enabled = name.isNotBlank()) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}

/**
 * Pastes a new API key for an existing server. The key is verified before it replaces the saved one, and
 * the error copy from a failed check is shown inside the dialog.
 */
@Composable
fun ReauthenticateDialog(
    host: String,
    isWorking: Boolean,
    errorMessage: String?,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var key by rememberSaveable { mutableStateOf("") }
    var isKeyVisible by rememberSaveable { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = { if (!isWorking) onDismiss() },
        title = { Text("Re-authenticate") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                Text("Paste a new API key for $host. It is checked before it replaces the saved key.")
                Text(
                    text = "Create one under Settings → Account → Personal API Keys.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                OutlinedTextField(
                    value = key,
                    onValueChange = { key = it },
                    label = { Text("API key") },
                    singleLine = true,
                    isError = errorMessage != null,
                    visualTransformation = if (isKeyVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        capitalization = KeyboardCapitalization.None,
                        imeAction = ImeAction.Done,
                    ),
                    keyboardActions = KeyboardActions(onDone = { if (key.isNotBlank() && !isWorking) onConfirm(key) }),
                    trailingIcon = {
                        VisibilityToggle(
                            visible = isKeyVisible,
                            subject = "API key",
                            onToggle = { isKeyVisible = !isKeyVisible },
                        )
                    },
                    supportingText = {
                        if (errorMessage != null) {
                            Text(text = errorMessage, color = MaterialTheme.colorScheme.error)
                        }
                    },
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(key) }, enabled = !isWorking && key.isNotBlank()) {
                if (isWorking) {
                    CircularProgressIndicator(modifier = Modifier.size(SPINNER_DP.dp), strokeWidth = 2.dp)
                } else {
                    Text("Check and save")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isWorking) { Text("Cancel") }
        },
    )
}

/** Confirms removal. The local key is deleted; the server and its key are untouched. */
@Composable
fun RemoveServerDialog(
    name: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Remove $name?") },
        text = {
            Text(
                "The saved key is deleted from this device. Revoke it in Rybbit if you no longer need it. " +
                    "Nothing on the server changes.",
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("Remove", color = MaterialTheme.colorScheme.error)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}
