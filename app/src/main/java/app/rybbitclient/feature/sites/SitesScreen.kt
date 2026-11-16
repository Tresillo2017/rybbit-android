package app.rybbitclient.feature.sites

import app.rybbitclient.core.ui.components.hapticClickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Clear
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.MediumFlexibleTopAppBar
import androidx.compose.material3.TopAppBarScrollBehavior
import app.rybbitclient.core.ui.components.ExpressiveRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import app.rybbitclient.core.ui.haptics.rememberHaptics
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import app.rybbitclient.core.ui.components.rememberCollapsingBarBehavior
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import app.rybbitclient.core.network.Unauthorized
import app.rybbitclient.core.ui.components.EmptyView
import app.rybbitclient.core.ui.components.ErrorView
import app.rybbitclient.core.ui.components.LiveBadge
import app.rybbitclient.core.ui.components.LoadingView
import app.rybbitclient.core.ui.theme.Spacing
import app.rybbitclient.di.LocalAppContainer
import app.rybbitclient.feature.common.toErrorCopy

/**
 * The first screen of the main flow: every organization and its sites, with live visitor counts.
 * Tapping a site remembers it and opens its dashboard. The top bar switches servers and opens Settings.
 *
 * Parameters:
 * - [onSiteSelected]: siteId is Site.analyticsId; siteName is the display name.
 * - [onOpenSettings]: Open Settings (instance management).
 * - [onAddInstance]: Open the add-instance flow.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun SitesScreen(
    onSiteSelected: (siteId: String, siteName: String) -> Unit,
    onOpenSettings: () -> Unit,
    onAddInstance: () -> Unit,
) {
    val container = LocalAppContainer.current
    val viewModel: SitesViewModel = viewModel(key = "sites") {
        SitesViewModel(container.analyticsRepository, container.instanceRepository)
    }
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val switcher by viewModel.switcher.collectAsStateWithLifecycle()
    val currentOnSiteSelected by rememberUpdatedState(onSiteSelected)
    LaunchedEffect(viewModel) {
        viewModel.autoOpenSite.collect { site -> currentOnSiteSelected(site.analyticsId, site.name) }
    }

    val scrollBehavior = rememberCollapsingBarBehavior()
    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            SitesTopBar(
                scrollBehavior = scrollBehavior,
                switcher = switcher,
                onSelectInstance = viewModel::selectInstance,
                onAddInstance = onAddInstance,
                onOpenSettings = onOpenSettings,
            )
        },
    ) { padding ->
        SitesBody(
            state = state,
            modifier = Modifier.fillMaxSize().padding(padding),
            onQueryChanged = viewModel::onQueryChanged,
            onRefresh = viewModel::refresh,
            onSiteTapped = { site ->
                viewModel.onSiteTapped(site)
                onSiteSelected(site.analyticsId, site.name)
            },
            onOpenSettings = onOpenSettings,
        )
    }
}

@Composable
private fun SitesBody(
    state: SitesUiState,
    modifier: Modifier,
    onQueryChanged: (String) -> Unit,
    onRefresh: () -> Unit,
    onSiteTapped: (SiteItem) -> Unit,
    onOpenSettings: () -> Unit,
) {
    when (state) {
        SitesUiState.Loading -> LoadingView(modifier = modifier)
        is SitesUiState.Error -> SitesError(state, modifier, onRetry = onRefresh, onOpenSettings = onOpenSettings)
        is SitesUiState.Content -> SitesContent(state, modifier, onQueryChanged, onRefresh, onSiteTapped)
    }
}

@Composable
private fun SitesError(
    state: SitesUiState.Error,
    modifier: Modifier,
    onRetry: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    val copy = state.error.toErrorCopy()
    val needsSignIn = state.error is Unauthorized
    ErrorView(
        title = copy.title,
        message = copy.message,
        modifier = modifier,
        onRetry = if (needsSignIn) onOpenSettings else onRetry,
        retryLabel = if (needsSignIn) "Open Settings" else "Try again",
    )
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun SitesContent(
    state: SitesUiState.Content,
    modifier: Modifier,
    onQueryChanged: (String) -> Unit,
    onRefresh: () -> Unit,
    onSiteTapped: (SiteItem) -> Unit,
) {
    ExpressiveRefreshBox(
        isRefreshing = state.isRefreshing,
        onRefresh = onRefresh,
        modifier = modifier,
    ) {
        LazyColumn(
            contentPadding = PaddingValues(bottom = Spacing.lg),
            verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap),
        ) {
            if (state.showSearch) {
                item(key = "search") {
                    SiteSearchField(query = state.query, onQueryChanged = onQueryChanged)
                }
            }
            sitesSections(state, onSiteTapped)
        }
    }
}

/** Renders the sections, or an empty state when there are no sites or nothing matches the search. */
@OptIn(ExperimentalFoundationApi::class)
private fun LazyListScope.sitesSections(state: SitesUiState.Content, onSiteTapped: (SiteItem) -> Unit) {
    when {
        state.organizations.isEmpty() -> item(key = "empty") {
            EmptyView(
                title = "No sites yet",
                message = "Add a site in Rybbit to see it here. Pull down to refresh once it's set up.",
            )
        }
        state.visibleOrganizations.isEmpty() -> item(key = "no-match") {
            EmptyView(title = "No matching sites", message = "Try a different name or domain.")
        }
        else -> state.visibleOrganizations.forEach { group ->
            stickyHeader(key = "org-${group.id}") { OrganizationHeader(group.name) }
            itemsIndexed(group.sites, key = { _, site -> "site-${site.analyticsId}" }) { index, site ->
                SiteRow(
                    site = site,
                    liveCount = state.liveCounts[site.analyticsId],
                    index = index,
                    count = group.sites.size,
                    onClick = { onSiteTapped(site) },
                )
            }
        }
    }
}

@Composable
private fun OrganizationHeader(name: String) {
    Text(
        text = name,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = Spacing.screenGutter, vertical = Spacing.sm)
            .semantics { heading() },
    )
}

@Composable
private fun SiteRow(site: SiteItem, liveCount: Int?, index: Int, count: Int, onClick: () -> Unit) {
    val domain = site.domain
    val haptics = rememberHaptics()
    SegmentedListItem(
        onClick = {
            haptics.click()
            onClick()
        },
        shapes = ListItemDefaults.segmentedShapes(index = index, count = count),
        colors = ListItemDefaults.segmentedColors(),
        leadingContent = { SiteAvatar(site.name) },
        trailingContent = { if (liveCount != null) LiveBadge(count = liveCount) },
        supportingContent = { if (domain != null) Text(domain, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        modifier = Modifier.padding(horizontal = Spacing.screenGutter),
    ) {
        Text(site.name, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun SiteSearchField(query: String, onQueryChanged: (String) -> Unit) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChanged,
        label = { Text("Search sites") },
        singleLine = true,
        leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = { onQueryChanged("") }) {
                    Icon(Icons.Outlined.Clear, contentDescription = "Clear search")
                }
            }
        },
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.screenGutter, vertical = Spacing.sm),
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SitesTopBar(
    scrollBehavior: TopAppBarScrollBehavior,
    switcher: InstanceSwitcherState,
    onSelectInstance: (String) -> Unit,
    onAddInstance: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    MediumFlexibleTopAppBar(
        scrollBehavior = scrollBehavior,
        title = {
            InstanceSwitcher(switcher = switcher, onSelectInstance = onSelectInstance, onAddInstance = onAddInstance)
        },
        actions = {
            IconButton(onClick = onOpenSettings) {
                Icon(Icons.Outlined.Settings, contentDescription = "Settings")
            }
        },
    )
}
