package app.rybbitclient.core.ui.components

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DateRangePicker
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.rememberDateRangePickerState
import androidx.compose.runtime.Composable
import app.rybbitclient.core.ui.haptics.rememberHaptics
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import app.rybbitclient.core.ui.theme.RybbitTheme
import app.rybbitclient.core.ui.theme.Spacing

private const val CUSTOM_CHIP_KEY = "__custom__"
private const val CUSTOM_ICON_DESCRIPTION = "Pick a custom date range"
private const val PICKER_CONFIRM = "Apply"
private const val PICKER_CANCEL = "Cancel"
private const val PICKER_TITLE = "Select dates"

/** A preset the user can pick, e.g. `DateRangeOption("7d", "Last 7 days")`. */
data class DateRangeOption(val key: String, val label: String)

/**
 * Horizontally scrolling preset chips plus a trailing "Custom" chip. Tapping Custom opens a
 * Material 3 [DateRangePicker] dialog; on Apply, [onCustomRangeSelected] receives the start and
 * end as epoch millis (UTC midnight, as the picker reports them).
 *
 * - [selectedKey] highlights a preset. Pass `null` while a custom range is active.
 * - [customLabel] is the Custom chip text; pass the formatted range (e.g. "3 Nov - 9 Nov") when set.
 * - [initialStartMillis]/[initialEndMillis] pre-fill the picker with the active custom range.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateRangeBar(
    options: List<DateRangeOption>,
    selectedKey: String?,
    onPresetSelected: (String) -> Unit,
    onCustomRangeSelected: (startMillis: Long, endMillis: Long) -> Unit,
    modifier: Modifier = Modifier,
    customLabel: String = "Custom",
    customSelected: Boolean = false,
    initialStartMillis: Long? = null,
    initialEndMillis: Long? = null,
) {
    var pickerOpen by remember { mutableStateOf(false) }
    val haptics = rememberHaptics()
    LazyRow(
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = Spacing.screenGutter),
        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(Spacing.sm),
    ) {
        items(items = options, key = { it.key }) { option ->
            FilterChip(
                selected = !customSelected && option.key == selectedKey,
                onClick = {
                    if (option.key != selectedKey || customSelected) haptics.selection()
                    onPresetSelected(option.key)
                },
                label = { Text(option.label, maxLines = 1) },
                modifier = Modifier.minimumInteractiveComponentSize(),
            )
        }
        item(key = CUSTOM_CHIP_KEY) {
            FilterChip(
                selected = customSelected,
                onClick = {
                    haptics.selection()
                    pickerOpen = true
                },
                label = { Text(customLabel, maxLines = 1) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Outlined.CalendarMonth,
                        contentDescription = null,
                        modifier = Modifier.semantics { contentDescription = CUSTOM_ICON_DESCRIPTION },
                    )
                },
                modifier = Modifier.minimumInteractiveComponentSize(),
            )
        }
    }
    if (pickerOpen) {
        DateRangePickerDialog(
            initialStartMillis = initialStartMillis,
            initialEndMillis = initialEndMillis,
            onDismiss = { pickerOpen = false },
            onApply = { start, end ->
                haptics.confirm()
                pickerOpen = false
                onCustomRangeSelected(start, end)
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DateRangePickerDialog(
    initialStartMillis: Long?,
    initialEndMillis: Long?,
    onDismiss: () -> Unit,
    onApply: (Long, Long) -> Unit,
) {
    val state = rememberDateRangePickerState(
        initialSelectedStartDateMillis = initialStartMillis,
        initialSelectedEndDateMillis = initialEndMillis,
    )
    val start = state.selectedStartDateMillis
    val end = state.selectedEndDateMillis
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = { if (start != null && end != null) onApply(start, end) },
                enabled = start != null && end != null,
                modifier = Modifier.minimumInteractiveComponentSize(),
            ) {
                Text(PICKER_CONFIRM)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, modifier = Modifier.minimumInteractiveComponentSize()) {
                Text(PICKER_CANCEL)
            }
        },
    ) {
        DateRangePicker(
            state = state,
            title = { Text(PICKER_TITLE, modifier = Modifier.padding(start = Spacing.lg, top = Spacing.lg)) },
            showModeToggle = false,
            modifier = Modifier.padding(bottom = Spacing.sm),
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun DateRangeBarPreview() {
    RybbitTheme {
        DateRangeBar(
            options = listOf(
                DateRangeOption("today", "Today"),
                DateRangeOption("7d", "Last 7 days"),
                DateRangeOption("30d", "Last 30 days"),
                DateRangeOption("90d", "Last 90 days"),
            ),
            selectedKey = "7d",
            onPresetSelected = {},
            onCustomRangeSelected = { _, _ -> },
            modifier = Modifier.padding(vertical = Spacing.sm),
        )
    }
}
