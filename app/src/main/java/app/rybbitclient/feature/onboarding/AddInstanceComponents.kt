package app.rybbitclient.feature.onboarding

import app.rybbitclient.core.ui.haptics.rememberHaptics
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import app.rybbitclient.core.ui.theme.Spacing
import app.rybbitclient.core.ui.theme.rybbitColors

private const val SPINNER_SIZE_DP = 18
private const val BANNER_ICON_DP = 20

/** The "Check server" button and the status beside it. The status is announced when it changes. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ServerCheckRow(
    state: AddInstanceUiState,
    onCheck: () -> Unit,
) {
    val canCheck = state.baseUrl != null && !state.isSubmitting && state.serverCheck != ServerCheck.Checking
    val haptics = rememberHaptics()
    LaunchedEffect(state.serverCheck) {
        when (state.serverCheck) {
            ServerCheck.Reachable -> haptics.confirm()
            ServerCheck.Failed -> haptics.reject()
            else -> Unit
        }
    }
    Row(
        modifier = Modifier.fillMaxWidth().semantics { liveRegion = LiveRegionMode.Polite },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        OutlinedButton(
            onClick = onCheck,
            enabled = canCheck,
            shapes = ButtonDefaults.shapes(),
            modifier = Modifier.minimumInteractiveComponentSize(),
        ) {
            Text("Check server")
        }
        Spacer(Modifier.width(Spacing.md))
        ServerCheckStatus(state)
    }
}

@Composable
private fun ServerCheckStatus(state: AddInstanceUiState) {
    val positive = MaterialTheme.rybbitColors.positive
    val error = MaterialTheme.colorScheme.error
    when (state.serverCheck) {
        ServerCheck.NotChecked -> StatusLine("Not checked yet", MaterialTheme.colorScheme.onSurfaceVariant)
        ServerCheck.Checking -> Row(verticalAlignment = Alignment.CenterVertically) {
            CircularProgressIndicator(modifier = Modifier.size(SPINNER_SIZE_DP.dp), strokeWidth = 2.dp)
            Spacer(Modifier.width(Spacing.sm))
            StatusLine("Checking…", MaterialTheme.colorScheme.onSurfaceVariant)
        }
        ServerCheck.Reachable -> Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Filled.CheckCircle,
                contentDescription = null,
                tint = positive,
                modifier = Modifier.size(BANNER_ICON_DP.dp),
            )
            Spacer(Modifier.width(Spacing.xs))
            StatusLine("Rybbit server found", positive)
        }
        ServerCheck.Failed -> StatusLine(state.serverProblem.orEmpty(), error)
    }
}

@Composable
private fun StatusLine(text: String, color: androidx.compose.ui.graphics.Color) {
    Text(text = text, style = MaterialTheme.typography.bodyMedium, color = color)
}

/** Warning shown for plain http:// addresses. Uses the warning colors of the theme's error scheme. */
@Composable
fun InsecureTrafficBanner() {
    Surface(
        color = MaterialTheme.colorScheme.errorContainer,
        contentColor = MaterialTheme.colorScheme.onErrorContainer,
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier.fillMaxWidth().semantics { liveRegion = LiveRegionMode.Polite },
    ) {
        Row(
            modifier = Modifier.padding(Spacing.md),
            verticalAlignment = Alignment.Top,
        ) {
            Icon(
                imageVector = Icons.Outlined.Warning,
                contentDescription = null,
                modifier = Modifier.size(BANNER_ICON_DP.dp),
            )
            Spacer(Modifier.width(Spacing.sm))
            Text(
                text = "Traffic to this server is not encrypted. Use https:// if the server supports it.",
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

/** Segmented control for choosing the authentication method. Each segment is at least 48dp tall. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthMethodSwitch(
    selected: AuthMethod,
    onSelect: (AuthMethod) -> Unit,
) {
    val methods = AuthMethod.entries
    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
        methods.forEachIndexed { index, method ->
            SegmentedButton(
                selected = selected == method,
                onClick = { onSelect(method) },
                shape = SegmentedButtonDefaults.itemShape(index = index, count = methods.size),
                modifier = Modifier.heightIn(min = Spacing.minTouchTarget),
                label = { Text(method.label) },
            )
        }
    }
}

/** Show/hide toggle for a secret field. [subject] names the field for the screen reader. */
@Composable
fun VisibilityToggle(
    visible: Boolean,
    subject: String,
    onToggle: () -> Unit,
) {
    IconButton(onClick = onToggle) {
        Icon(
            imageVector = if (visible) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
            contentDescription = if (visible) "Hide $subject" else "Show $subject",
        )
    }
}
