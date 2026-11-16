package app.rybbitclient.feature.onboarding

import app.rybbitclient.core.ui.motion.staggeredEntrance
import app.rybbitclient.core.ui.haptics.rememberHaptics
import app.rybbitclient.core.ui.components.pressScale
import androidx.compose.ui.draw.clip
import androidx.compose.runtime.remember
import androidx.compose.material3.toShape
import androidx.compose.material3.MaterialShapes
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Cloud
import androidx.compose.material.icons.outlined.Dns
import androidx.compose.material.icons.outlined.Insights
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.rybbitclient.core.ui.theme.Spacing

private const val HERO_ICON_DP = 56
private const val HERO_CONTAINER_DP = 120
private const val CHOICE_ICON_DP = 28
private const val CHOICE_MIN_HEIGHT_DP = 88

/**
 * First-run welcome: two large choices. Cloud signs in to app.rybbit.io; self-hosted asks for the
 * address of your own server.
 *
 * Parameters:
 * - [onAddCloud]: Start the Rybbit Cloud sign-in.
 * - [onAddSelfHosted]: Start the self-hosted sign-in.
 */
@Composable
fun WelcomeScreen(
    onAddCloud: () -> Unit,
    onAddSelfHosted: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = Spacing.screenGutter, vertical = Spacing.xxl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Spacing.lg),
    ) {
        Spacer(Modifier.height(Spacing.lg))
        Box(
            modifier = Modifier
                .staggeredEntrance(index = 0)
                .size(HERO_CONTAINER_DP.dp)
                .clip(MaterialShapes.Cookie9Sided.toShape())
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Outlined.Insights,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(HERO_ICON_DP.dp),
            )
        }
        Text(
            text = "Welcome to Rybbit",
            style = MaterialTheme.typography.displaySmall,
            textAlign = TextAlign.Center,
            modifier = Modifier.staggeredEntrance(index = 1),
        )
        Text(
            modifier = Modifier.staggeredEntrance(index = 2),
            text = "See your traffic, sessions and conversions on the go. Connect to Rybbit Cloud or to a server you run yourself.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(Spacing.md))
        ChoiceCard(
            title = "Rybbit Cloud",
            subtitle = "Sign in to ${AddInstanceUiState.CLOUD_BASE_URL}",
            icon = Icons.Outlined.Cloud,
            onClick = onAddCloud,
            modifier = Modifier.staggeredEntrance(index = 3),
        )
        ChoiceCard(
            title = "Self-hosted server",
            subtitle = "Connect to a Rybbit installation you host",
            icon = Icons.Outlined.Dns,
            onClick = onAddSelfHosted,
            modifier = Modifier.staggeredEntrance(index = 4),
        )
    }
}

/** A large, full-width choice. The whole card is the touch target and announces as a button. */
@Composable
private fun ChoiceCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val haptics = rememberHaptics()
    Card(
        onClick = {
            haptics.click()
            onClick()
        },
        interactionSource = interactionSource,
        shape = MaterialTheme.shapes.extraLarge,
        modifier = modifier
            .pressScale(interactionSource)
            .fillMaxWidth()
            .heightIn(min = CHOICE_MIN_HEIGHT_DP.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(Spacing.lg),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(CHOICE_ICON_DP.dp),
            )
            Spacer(Modifier.width(Spacing.lg))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, style = MaterialTheme.typography.titleMedium)
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Icon(
                imageVector = Icons.Outlined.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
