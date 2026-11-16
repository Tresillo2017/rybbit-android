@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package app.rybbitclient.core.ui.components

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.InputChip
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import app.rybbitclient.core.ui.haptics.rememberHaptics
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import app.rybbitclient.core.ui.theme.RybbitTheme
import app.rybbitclient.core.ui.theme.Spacing

private const val CLEAR_ALL_LABEL = "Clear all"
private const val REMOVE_DESCRIPTION_PREFIX = "Remove filter "

/** An applied filter. [key] identifies it for removal, [label] is what the chip shows. */
data class ActiveFilter(val key: String, val label: String)

/**
 * Row of removable chips for the applied filters, followed by a "Clear all" action. Renders
 * nothing when [filters] is empty so it costs no space until a filter is applied.
 */
@Composable
fun FilterChipsRow(
    filters: List<ActiveFilter>,
    onRemove: (key: String) -> Unit,
    onClearAll: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (filters.isEmpty()) return
    val haptics = rememberHaptics()
    LazyRow(
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = Spacing.screenGutter),
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        items(items = filters, key = { it.key }) { filter ->
            InputChip(
                selected = true,
                onClick = {
                    haptics.toggle(isOn = false)
                    onRemove(filter.key)
                },
                label = { Text(filter.label, maxLines = 1) },
                trailingIcon = {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = null,
                        modifier = Modifier.semantics {
                            contentDescription = REMOVE_DESCRIPTION_PREFIX + filter.label
                        },
                    )
                },
                modifier = Modifier.minimumInteractiveComponentSize(),
            )
        }
        item(key = CLEAR_ALL_KEY) {
            TextButton(
                onClick = {
                    haptics.toggle(isOn = false)
                    onClearAll()
                },
                modifier = Modifier.minimumInteractiveComponentSize(),
            ) {
                Text(
                    text = CLEAR_ALL_LABEL,
                    style = MaterialTheme.typography.labelLarge,
                )
            }
        }
    }
}

private const val CLEAR_ALL_KEY = "__clear_all__"

@Preview(showBackground = true)
@Composable
private fun FilterChipsRowPreview() {
    RybbitTheme {
        FilterChipsRow(
            filters = listOf(
                ActiveFilter("browser", "Browser: Firefox"),
                ActiveFilter("country", "Country: Germany"),
            ),
            onRemove = {},
            onClearAll = {},
            modifier = Modifier,
        )
    }
}
