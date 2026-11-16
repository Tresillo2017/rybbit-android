package app.rybbitclient.feature.users

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import app.rybbitclient.core.ui.components.ErrorView
import app.rybbitclient.core.ui.components.LoadingView
import app.rybbitclient.core.ui.components.UnavailableView
import app.rybbitclient.core.ui.theme.Spacing
import app.rybbitclient.di.LocalAppContainer
import app.rybbitclient.feature.common.toErrorCopy
import app.rybbitclient.feature.sessions.EmptyCopy
import app.rybbitclient.feature.sessions.PagedListBody
import app.rybbitclient.feature.sessions.SessionRow
import java.time.ZoneId

private const val SCREEN_TITLE = "User"
private const val BACK_DESCRIPTION = "Back"
private const val SESSIONS_TITLE = "Sessions"
private const val PROFILE_KEY = "profile"
private const val SESSIONS_TITLE_KEY = "sessions-title"

/**
 * UserDetail: the visitor's profile (all-time totals, first and last seen, location, device, traits) and their
 * sessions in the shared date range. Tapping a session opens SessionDetail.
 *
 * Parameters:
 * - [siteId]: Site.analyticsId for API calls.
 * - [userId]: User id from the users list (the identified ID, or the anonymous device ID).
 * - [onOpenSession]: Open SessionDetail.
 * - [onBack]: Back navigation.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserDetailScreen(
    siteId: String,
    userId: String,
    onOpenSession: (sessionId: String) -> Unit,
    onBack: () -> Unit,
) {
    val container = LocalAppContainer.current
    val viewModel: UserDetailViewModel = viewModel(key = "user-$siteId-$userId") {
        UserDetailViewModel(siteId, userId, container.analyticsRepository, container.siteQueryState.query)
    }
    val profileState by viewModel.profileState.collectAsStateWithLifecycle()
    val sessionsState by viewModel.uiState.collectAsStateWithLifecycle()
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(SCREEN_TITLE) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = BACK_DESCRIPTION)
                    }
                },
            )
        },
    ) { innerPadding ->
        val modifier = Modifier.padding(innerPadding).fillMaxSize()
        when (val profile = profileState) {
            UserProfileState.Loading -> LoadingView(modifier = modifier)
            is UserProfileState.Error -> {
                val copy = profile.error.toErrorCopy()
                ErrorView(title = copy.title, message = copy.message, modifier = modifier, onRetry = viewModel::refreshAll)
            }
            UserProfileState.Unavailable -> UnavailableView(modifier = modifier)
            is UserProfileState.Loaded -> PagedListBody(
                state = sessionsState,
                empty = EmptyCopy(
                    title = "No sessions",
                    message = "This user has no sessions in the selected date range.",
                ),
                itemKey = { it.sessionId },
                onRefresh = viewModel::refreshAll,
                onLoadMore = viewModel::loadMore,
                modifier = modifier,
                leading = {
                    item(key = PROFILE_KEY) {
                        UserProfileCard(
                            profile = profile.profile,
                            zone = ZoneId.systemDefault(),
                            modifier = Modifier.padding(horizontal = Spacing.screenGutter, vertical = Spacing.sm),
                        )
                    }
                    item(key = SESSIONS_TITLE_KEY) {
                        Text(
                            text = SESSIONS_TITLE,
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(horizontal = Spacing.screenGutter, vertical = Spacing.sm),
                        )
                    }
                },
            ) { session ->
                SessionRow(session = session, onClick = { onOpenSession(session.sessionId) })
            }
        }
    }
}
