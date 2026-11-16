package app.rybbitclient.feature.realtime

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.rybbitclient.core.data.AnalyticsRepository
import app.rybbitclient.core.network.ApiException
import app.rybbitclient.core.query.AnalyticsQuery
import app.rybbitclient.core.query.DateRange
import app.rybbitclient.core.query.TimeBucket
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/** Live count and feed refresh interval while the screen is visible. */
const val REALTIME_POLL_MILLIS = 10_000L

/** Sparkline refresh interval. The minute series is large, so it polls less often. */
const val REALTIME_TREND_POLL_MILLIS = 60_000L

/** Newest events fetched per poll. */
const val REALTIME_FEED_PAGE_SIZE = 50

/** Minutes shown in the sessions-per-minute sparkline. */
const val REALTIME_TREND_POINTS = 30

/**
 * Everything [RealtimeScreen] renders. Before the first poll finishes, [liveCount], [feed] and [error]
 * are all null. A poll failure after data has loaded sets [error] and keeps the last good values.
 */
data class RealtimeUiState(
    val liveCount: Int? = null,
    val feed: List<FeedRow>? = null,
    val trend: List<Double>? = null,
    val trendUnavailable: Boolean = false,
    val error: ApiException? = null,
    val isRefreshing: Boolean = false,
) {
    val isInitialLoading: Boolean get() = liveCount == null && feed == null && error == null
    val hasNoData: Boolean get() = liveCount == null && feed == null
}

/**
 * ViewModel for [RealtimeScreen]. Polls the live count and the newest events every 10 s and the
 * sessions-per-minute series every 60 s. The screen starts and stops polling with its lifecycle.
 * Realtime ignores the shared date range and filters: it always shows "now".
 */
class RealtimeViewModel(
    private val siteId: String,
    private val analytics: AnalyticsRepository,
    private val livePollMillis: Long = REALTIME_POLL_MILLIS,
    private val trendPollMillis: Long = REALTIME_TREND_POLL_MILLIS,
) : ViewModel() {

    private val mutableState = MutableStateFlow(RealtimeUiState())
    val uiState: StateFlow<RealtimeUiState> = mutableState.asStateFlow()

    private var pollJob: Job? = null
    private var trendJob: Job? = null

    /** Starts both loops. Idempotent. */
    fun startPolling() {
        if (pollJob?.isActive != true) {
            pollJob = viewModelScope.launch {
                while (isActive) {
                    pollLiveAndFeed()
                    delay(livePollMillis)
                }
            }
        }
        if (trendJob?.isActive != true) {
            trendJob = viewModelScope.launch {
                while (isActive) {
                    loadTrend()
                    delay(trendPollMillis)
                }
            }
        }
    }

    /** Stops both loops while the screen is not visible. */
    fun stopPolling() {
        pollJob?.cancel()
        trendJob?.cancel()
        pollJob = null
        trendJob = null
    }

    /** Pull-to-refresh: polls everything once right away. */
    fun refresh() {
        mutableState.update { it.copy(isRefreshing = true) }
        viewModelScope.launch {
            try {
                coroutineScope {
                    launch { pollLiveAndFeed() }
                    launch { loadTrend() }
                }
            } finally {
                mutableState.update { it.copy(isRefreshing = false) }
            }
        }
    }

    private suspend fun pollLiveAndFeed() {
        val (live, feed) = coroutineScope {
            val liveDeferred = async { attempt { analytics.liveUserCount(siteId) } }
            val feedDeferred = async { attempt { RealtimeFeed.rowsFrom(analytics.events(siteId, FEED_QUERY, REALTIME_FEED_PAGE_SIZE).data) } }
            liveDeferred.await() to feedDeferred.await()
        }
        mutableState.update { state ->
            state.copy(
                liveCount = live.getOrNull() ?: state.liveCount,
                feed = feed.getOrNull()?.let { RealtimeFeed.merge(state.feed.orEmpty(), it) } ?: state.feed,
                error = listOfNotNull(live.exceptionOrNull(), feed.exceptionOrNull()).firstOrNull() as? ApiException,
            )
        }
    }

    private suspend fun loadTrend() {
        val series = attempt {
            analytics.overviewTimeSeries(siteId, TREND_QUERY, TimeBucket.Minute)
                .takeLast(REALTIME_TREND_POINTS)
                .map { (it.sessions ?: 0).toDouble() }
        }
        mutableState.update { state ->
            state.copy(trend = series.getOrNull(), trendUnavailable = series.isFailure)
        }
    }

    private suspend fun <T> attempt(block: suspend () -> T): Result<T> = try {
        Result.success(block())
    } catch (error: ApiException) {
        Result.failure(error)
    }

    private companion object {
        val FEED_QUERY = AnalyticsQuery(DateRange.AllTime)
        val TREND_QUERY = AnalyticsQuery(DateRange.Last24Hours)
    }
}
