@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package app.rybbitclient.core.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SecondaryScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import app.rybbitclient.core.ui.theme.RybbitTheme
import app.rybbitclient.core.ui.theme.Spacing

private const val BREAKDOWN_EDGE_PADDING_DP = 0

/**
 * A [SectionCard] with a scrollable tab strip (e.g. Pages / Entry / Exit) above a ranked list.
 * The caller owns [selectedTab] and supplies [items] for that tab. The list resets its
 * "show more" state when the tab changes.
 */
@Composable
fun BreakdownCard(
    title: String,
    tabs: List<String>,
    selectedTab: Int,
    onTabSelected: (Int) -> Unit,
    items: List<BarItem>,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    limit: Int = 5,
    onShowMore: (() -> Unit)? = null,
    onItemClick: ((String) -> Unit)? = null,
    itemLeading: (@Composable (BarItem) -> Unit)? = null,
    emptyMessage: String = "No data for this period",
) {
    val safeTab = if (tabs.isEmpty()) 0 else selectedTab.coerceIn(0, tabs.lastIndex)
    SectionCard(title = title, subtitle = subtitle, modifier = modifier) {
        if (tabs.isNotEmpty()) {
            BreakdownTabs(tabs = tabs, selectedTab = safeTab, onTabSelected = onTabSelected)
        }
        key(safeTab) {
            BarListBody(
                items = items,
                limit = limit,
                onShowMore = onShowMore,
                onItemClick = onItemClick,
                itemLeading = itemLeading,
                emptyMessage = emptyMessage,
            )
        }
    }
}

@Composable
private fun BreakdownTabs(
    tabs: List<String>,
    selectedTab: Int,
    onTabSelected: (Int) -> Unit,
) {
    SecondaryScrollableTabRow(
        selectedTabIndex = selectedTab,
        modifier = Modifier.fillMaxWidth(),
        containerColor = Color.Transparent,
        edgePadding = BREAKDOWN_EDGE_PADDING_DP.dp,
    ) {
        tabs.forEachIndexed { index, title ->
            Tab(
                selected = index == selectedTab,
                onClick = { onTabSelected(index) },
                text = {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.labelLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun BreakdownCardPreview() {
    RybbitTheme {
        Column(modifier = Modifier.fillMaxWidth().padding(Spacing.lg)) {
            BreakdownCard(
                title = "Devices",
                tabs = listOf("Device", "Browser", "OS"),
                selectedTab = 1,
                onTabSelected = {},
                items = listOf(
                    BarItem("chrome", "Chrome", 5_200.0, "5.2k", "61%"),
                    BarItem("safari", "Safari", 1_900.0, "1.9k", "22%"),
                    BarItem("firefox", "Firefox", 780.0, "780", "9%"),
                ),
            )
        }
    }
}
