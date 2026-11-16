package app.rybbitclient.feature.users

import app.rybbitclient.core.ui.components.hapticClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Clear
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import app.rybbitclient.core.model.UserSummary
import app.rybbitclient.core.ui.format.EMPTY_VALUE
import app.rybbitclient.core.ui.format.countryLabel
import app.rybbitclient.core.ui.format.formatRelativeTime
import app.rybbitclient.core.ui.theme.Spacing
import app.rybbitclient.di.LocalAppContainer
import app.rybbitclient.feature.sessions.EmptyCopy
import app.rybbitclient.feature.sessions.PagedListBody
import app.rybbitclient.feature.sessions.SiteQueryHeader
import app.rybbitclient.feature.sessions.browserLabel
import app.rybbitclient.feature.sessions.deviceIcon
import app.rybbitclient.feature.sessions.plural
import app.rybbitclient.feature.sessions.userDisplayName
import app.rybbitclient.feature.sessions.utcMillisOrNull

private const val SEARCH_PLACEHOLDER = "Search by user ID"
private const val IDENTIFIED_ONLY_LABEL = "Identified only"
private const val CLEAR_SEARCH_DESCRIPTION = "Clear search"
private const val MIDDOT = " · "

/**
 * The Users tab: infinite list of visitors (identified or anonymous) with a search box and an identified-only
 * toggle, under the shared date range and filters. Tapping a user opens UserDetail.
 *
 * Parameters:
 * - [siteId]: Site.analyticsId for API calls.
 * - [onOpenUser]: Open UserDetail with the user's effective ID.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UsersScreen(
    siteId: String,
    onOpenUser: (userId: String) -> Unit,
) {
    val container = LocalAppContainer.current
    val viewModel: UsersViewModel = viewModel(key = "users-$siteId") {
        UsersViewModel(siteId, container.analyticsRepository, container.siteQueryState.query)
    }
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val query by container.siteQueryState.query.collectAsStateWithLifecycle()
    val searchText by viewModel.searchText.collectAsStateWithLifecycle()
    val identifiedOnly by viewModel.identifiedOnly.collectAsStateWithLifecycle()

    Column(modifier = Modifier.fillMaxSize()) {
        SiteQueryHeader(
            query = query,
            onRangeSelected = container.siteQueryState::setRange,
            onRemoveFilter = container.siteQueryState::removeFilter,
            onClearFilters = container.siteQueryState::clearFilters,
        )
        UserListControls(
            searchText = searchText,
            onSearchChanged = viewModel::onSearchTextChanged,
            identifiedOnly = identifiedOnly,
            onIdentifiedOnlyChanged = viewModel::setIdentifiedOnly,
        )
        PagedListBody(
            state = state,
            empty = usersEmptyCopy(searchText),
            itemKey = { it.userId },
            onRefresh = viewModel::refresh,
            onLoadMore = viewModel::loadMore,
            modifier = Modifier.fillMaxSize(),
        ) { user ->
            UserRow(user = user, onClick = { onOpenUser(user.effectiveUserId ?: user.userId) })
        }
    }
}

/** Search box and the identified-only chip. Typing is debounced in the ViewModel. */
@Composable
private fun UserListControls(
    searchText: String,
    onSearchChanged: (String) -> Unit,
    identifiedOnly: Boolean,
    onIdentifiedOnlyChanged: (Boolean) -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.screenGutter),
        verticalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        OutlinedTextField(
            value = searchText,
            onValueChange = onSearchChanged,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            placeholder = { Text(SEARCH_PLACEHOLDER) },
            leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
            trailingIcon = {
                if (searchText.isNotEmpty()) {
                    IconButton(onClick = { onSearchChanged("") }) {
                        Icon(Icons.Outlined.Clear, contentDescription = CLEAR_SEARCH_DESCRIPTION)
                    }
                }
            },
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            FilterChip(
                selected = identifiedOnly,
                onClick = { onIdentifiedOnlyChanged(!identifiedOnly) },
                label = { Text(IDENTIFIED_ONLY_LABEL) },
            )
        }
    }
}

/** One visitor: name (identified ID or short anonymous ID), country, device and browser, plus counts and last seen. */
@Composable
fun UserRow(user: UserSummary, onClick: () -> Unit) {
    ListItem(
        headlineContent = {
            Text(
                text = userDisplayName(user.userId, user.identifiedUserId),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
        supportingContent = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = deviceIcon(user.deviceType),
                    contentDescription = "Device ${user.deviceType?.takeIf(String::isNotBlank) ?: EMPTY_VALUE}",
                    modifier = Modifier.padding(end = Spacing.xs),
                )
                Text(text = userSupportingLine(user), maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        },
        trailingContent = {
            Column(horizontalAlignment = Alignment.End) {
                Text(text = plural(user.sessions ?: 0, "session"), style = MaterialTheme.typography.labelMedium)
                utcMillisOrNull(user.lastSeen)?.let { lastSeen ->
                    Text(
                        text = formatRelativeTime(lastSeen),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        },
        modifier = Modifier.hapticClickable(onClick = onClick),
    )
}

/** Country flag and name, browser, and pageviews, skipping what is unknown. */
internal fun userSupportingLine(user: UserSummary): String {
    val country = user.country?.takeIf(String::isNotBlank)?.let { countryLabel(it) }
    val browser = browserLabel(user.browser, user.browserVersion).takeIf { it != EMPTY_VALUE }
    val pageviews = user.pageviews?.let { plural(it, "pageview") }
    return listOfNotNull(country, browser, pageviews).joinToString(MIDDOT).ifEmpty { EMPTY_VALUE }
}

/**
 * Empty copy for the users list. A search that matches nothing gets its own hint: the server matches the
 * search term against user IDs only, so clearing it is the next step.
 */
internal fun usersEmptyCopy(searchText: String): EmptyCopy = if (searchText.isBlank()) {
    EmptyCopy(title = "No users found", message = "No users match this filter or date range.")
} else {
    EmptyCopy(
        title = "No users match \"${searchText.trim()}\"",
        message = "Search looks for this text in user IDs. Clear the search to browse all users in this range.",
    )
}
