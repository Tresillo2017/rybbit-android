package app.rybbitclient.feature.overview

import app.rybbitclient.core.ui.components.hapticClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ListItem
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import app.rybbitclient.core.model.MetricRow
import app.rybbitclient.core.ui.components.EmptyView
import app.rybbitclient.core.ui.components.ErrorView
import app.rybbitclient.core.ui.format.formatFullNumber
import app.rybbitclient.core.ui.theme.Spacing
import app.rybbitclient.feature.common.toErrorCopy

private const val PREFETCH_DISTANCE = 5
private const val SHEET_HEIGHT_FRACTION = 0.9f

/**
 * Full ranked list for one breakdown parameter, in a bottom sheet. Pages load as the user nears the end.
 * Tapping a row adds it as a filter and closes the sheet.
 *
 * Parameters:
 * - [list]: rows loaded so far, paging state and any page error.
 * - [title]: sheet heading, e.g. "Sources: Referrer".
 * - [onRowSelected]: receives the raw value of the tapped row.
 * - [onLoadMore]: fetches the next page; also the retry action after a page error.
 * - [onDismiss]: closes the sheet.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FullListSheet(
    list: FullListState,
    title: String,
    onRowSelected: (String) -> Unit,
    onLoadMore: () -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        FullListBody(list = list, title = title, onRowSelected = onRowSelected, onLoadMore = onLoadMore)
    }
}

@Composable
private fun FullListBody(
    list: FullListState,
    title: String,
    onRowSelected: (String) -> Unit,
    onLoadMore: () -> Unit,
) {
    val listState = rememberLazyListState()
    LoadMoreEffect(listState = listState, list = list, onLoadMore = onLoadMore)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(SHEET_HEIGHT_FRACTION)
            .padding(horizontal = Spacing.screenGutter),
    ) {
        LazyColumn(state = listState, modifier = Modifier.fillMaxSize()) {
            item(key = "title") {
                Text(text = title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(bottom = Spacing.sm))
            }
            itemsIndexed(list.rows, key = { index, row -> "$index-${row.value}" }) { _, row ->
                FullListRow(
                    row = row,
                    parameter = list.parameter,
                    onClick = { onRowSelected(row.value.orEmpty()) },
                )
            }
            footer(list = list, onLoadMore = onLoadMore)
        }
    }
}

private fun LazyListScope.footer(list: FullListState, onLoadMore: () -> Unit) {
    val error = list.error
    when {
        error != null -> item(key = "error") {
            val copy = error.toErrorCopy()
            ErrorView(title = copy.title, message = copy.message, onRetry = onLoadMore)
        }
        list.isLoadingMore -> item(key = "loading") {
            Box(modifier = Modifier.fillMaxWidth().padding(Spacing.md), contentAlignment = Alignment.Center) {
                LoadingIndicator()
            }
        }
        list.rows.isEmpty() -> item(key = "empty") {
            EmptyView(title = "Nothing here yet", message = "No values were recorded for this range or filter.")
        }
    }
}

@Composable
private fun LoadMoreEffect(listState: LazyListState, list: FullListState, onLoadMore: () -> Unit) {
    val nearEnd by remember(listState) {
        derivedStateOf {
            val info = listState.layoutInfo
            val lastVisible = info.visibleItemsInfo.lastOrNull()?.index ?: -1
            info.totalItemsCount > 0 && lastVisible >= info.totalItemsCount - PREFETCH_DISTANCE
        }
    }
    LaunchedEffect(nearEnd, list.hasMore, list.isLoadingMore, list.error) {
        if (nearEnd && list.hasMore && !list.isLoadingMore && list.error == null) {
            onLoadMore()
        }
    }
}

@Composable
private fun FullListRow(row: MetricRow, parameter: String, onClick: () -> Unit) {
    val label = displayValue(parameter, row.value)
    val countText = row.count?.let { formatFullNumber(it.toDouble()) }.orEmpty()
    val tappable = !row.value.isNullOrBlank()
    ListItem(
        headlineContent = { Text(label) },
        trailingContent = { Text(countText) },
        modifier = Modifier
            .semantics { contentDescription = "$label, $countText" }
            .then(if (tappable) Modifier.hapticClickable(role = Role.Button, onClick = onClick) else Modifier),
    )
}
