package app.rybbitclient.feature.users

import androidx.lifecycle.viewModelScope
import app.rybbitclient.core.data.AnalyticsRepository
import app.rybbitclient.core.model.SessionSummary
import app.rybbitclient.core.model.UserDetail
import app.rybbitclient.core.network.ApiException
import app.rybbitclient.core.network.FeatureUnavailable
import app.rybbitclient.core.query.AnalyticsQuery
import app.rybbitclient.feature.sessions.PageResult
import app.rybbitclient.feature.sessions.PagedListViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** The user's profile, loaded once per refresh. Sessions are paged separately through [PagedListViewModel.uiState]. */
sealed interface UserProfileState {
    data object Loading : UserProfileState
    data class Loaded(val profile: UserDetail) : UserProfileState
    data class Error(val error: ApiException) : UserProfileState
    data object Unavailable : UserProfileState
}

/**
 * UserDetail: the profile (all-time, from `GET /users/{id}`) plus the user's sessions in the shared date range,
 * paged. [userId] is the identified ID or anonymous device ID, as listed by the Users tab.
 *
 * Sessions are listed by the identified ID when the profile has one, because `/sessions?user_id=` matches an
 * identified user's sessions by that ID, not by the device ID. Until the profile loads, or if it fails, the
 * sessions use [userId].
 */
class UserDetailViewModel(
    private val siteId: String,
    private val userId: String,
    private val analytics: AnalyticsRepository,
    private val queryState: StateFlow<AnalyticsQuery>,
) : PagedListViewModel<SessionSummary>() {

    private val mutableProfile = MutableStateFlow<UserProfileState>(UserProfileState.Loading)
    val profileState: StateFlow<UserProfileState> = mutableProfile.asStateFlow()

    /** The ID the sessions are listed by. Set from the profile before the first sessions page is requested. */
    private var sessionsUserId: String = userId

    init {
        viewModelScope.launch {
            loadProfile()
            reloadOn(queryState)
        }
    }

    /** Pull-to-refresh and retry: reloads the profile, then the first page of sessions. */
    fun refreshAll() {
        viewModelScope.launch {
            loadProfile()
            refresh()
        }
    }

    override suspend fun fetchPage(page: Int): PageResult<SessionSummary> {
        val items = analytics.userSessions(siteId, queryState.value, sessionsUserId, page, PAGE_SIZE)
        return PageResult(items = items, hasMore = items.size >= PAGE_SIZE)
    }

    override fun itemKey(item: SessionSummary): Any = item.sessionId

    private suspend fun loadProfile() {
        mutableProfile.value = try {
            UserProfileState.Loaded(analytics.userDetail(siteId, userId))
        } catch (error: ApiException) {
            if (error is FeatureUnavailable) UserProfileState.Unavailable else UserProfileState.Error(error)
        }
        sessionsUserId = identifiedIdOrNull() ?: userId
    }

    private fun identifiedIdOrNull(): String? =
        (mutableProfile.value as? UserProfileState.Loaded)?.profile?.identifiedUserId?.takeIf(String::isNotBlank)

    companion object {
        const val PAGE_SIZE = 50
    }
}
