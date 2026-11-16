package app.rybbitclient.feature.errors

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import app.rybbitclient.core.model.ErrorEvent
import app.rybbitclient.core.ui.format.countryLabel
import app.rybbitclient.core.ui.theme.Spacing
import app.rybbitclient.feature.events.formatEventTime

private const val STACK_CORNER_DP = 8
private const val SHOW_STACK = "Show stack trace"
private const val HIDE_STACK = "Hide stack trace"
private const val COPY_STACK = "Copy stack trace"
private const val COPIED = "Copied"
private const val VIEW_SESSION = "View session"
private const val CLIPBOARD_LABEL = "Error stack trace"
private const val UNKNOWN_PAGE = "Unknown page"
private const val DETAIL_SEPARATOR = " · "

/**
 * One occurrence of an error: when, where, on what device, and the stack trace. The stack is collapsed
 * until asked for, and can be copied to the clipboard. [onOpenSession] opens the session it happened in.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ErrorOccurrenceCard(
    occurrence: ErrorEvent,
    onOpenSession: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val stack = occurrence.stack?.takeIf { it.isNotBlank() }
    val location = occurrence.location()
    val sessionId = occurrence.sessionId?.takeIf { it.isNotBlank() }
    var expanded by rememberSaveable(occurrence.timestamp, occurrence.sessionId) { mutableStateOf(false) }
    var copied by remember { mutableStateOf(false) }
    val context = LocalContext.current

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        Column(
            modifier = Modifier.padding(Spacing.lg),
            verticalArrangement = Arrangement.spacedBy(Spacing.xs),
        ) {
            Text(text = formatEventTime(occurrence.timestamp), style = MaterialTheme.typography.titleSmall)
            Text(text = occurrence.pageLine(), style = MaterialTheme.typography.bodyMedium, maxLines = 2)
            Text(
                text = occurrence.environmentLine(),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (sessionId != null) {
                TextButton(onClick = { onOpenSession(sessionId) }, modifier = Modifier.minimumInteractiveComponentSize()) {
                    Text(VIEW_SESSION)
                }
            }
            if (stack != null) {
                TextButton(onClick = { expanded = !expanded }, modifier = Modifier.minimumInteractiveComponentSize()) {
                    Text(if (expanded) HIDE_STACK else SHOW_STACK)
                }
                if (expanded) {
                    StackTraceBlock(stack = stack, location = location)
                    TextButton(
                        onClick = {
                            copyToClipboard(context, stack)
                            copied = true
                        },
                        modifier = Modifier.minimumInteractiveComponentSize(),
                    ) {
                        Text(if (copied) COPIED else COPY_STACK)
                    }
                }
            }
        }
    }
}

@Composable
private fun StackTraceBlock(stack: String, location: String?) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(STACK_CORNER_DP.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHighest)
            .horizontalScroll(rememberScrollState())
            .padding(Spacing.sm),
    ) {
        if (location != null) {
            Text(
                text = "Source: $location",
                fontFamily = FontFamily.Monospace,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(
            text = stack,
            fontFamily = FontFamily.Monospace,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

private fun ErrorEvent.location(): String? {
    val file = fileName?.takeIf { it.isNotBlank() } ?: return null
    return listOfNotNull(file, lineNumber?.toString(), columnNumber?.toString()).joinToString(separator = ":")
}

private fun ErrorEvent.pageLine(): String {
    val page = listOfNotNull(hostname?.takeIf { it.isNotBlank() }, pathname?.takeIf { it.isNotBlank() })
        .joinToString(separator = "")
    return page.ifBlank { UNKNOWN_PAGE }
}

private fun ErrorEvent.environmentLine(): String {
    val browserText = browser?.takeIf { it.isNotBlank() }?.let { name ->
        browserVersion?.takeIf { it.isNotBlank() }?.let { "$name $it" } ?: name
    }
    val parts = listOfNotNull(
        browserText,
        operatingSystem?.takeIf { it.isNotBlank() },
        deviceType?.takeIf { it.isNotBlank() },
        country?.takeIf { it.isNotBlank() }?.let { countryLabel(it) },
    )
    return parts.joinToString(separator = DETAIL_SEPARATOR)
}

private fun copyToClipboard(context: Context, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager ?: return
    clipboard.setPrimaryClip(ClipData.newPlainText(CLIPBOARD_LABEL, text))
}
