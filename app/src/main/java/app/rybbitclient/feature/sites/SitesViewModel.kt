package app.rybbitclient.feature.sites

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.rybbitclient.core.data.AnalyticsRepository
import app.rybbitclient.core.data.NoActiveInstanceException
import app.rybbitclient.core.model.Instance
import app.rybbitclient.core.model.OrganizationSites
import app.rybbitclient.core.network.ApiException
import app.rybbitclient.core.storage.InstanceRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit

private const val LIVE_COUNT_CONCURRENCY = 4

/**
 * Drives [SitesScreen]: the organization/site list, live visitor counts, search and the instance switcher.
 *
 * The list reloads whenever the active instance changes, including when Settings switches it or removes it.
 * Live counts are best-effort: each one is requested on its own, and a failed count is simply left out.
 * The remembered site opens automatically at most once per [autoOpenGate].
 */
class SitesViewModel(
    private val analytics: AnalyticsRepository,
    private val instances: InstanceRepository,
    private val autoOpenGate: AutoOpenGate = ProcessAutoOpenGate,
) : ViewModel() {

    private val mutableState = MutableStateFlow<SitesUiState>(SitesUiState.Loading)
    val uiState: StateFlow<SitesUiState> = mutableState.asStateFlow()

    /** Instances and the active one, for the top-bar switcher. */
    val switcher: StateFlow<InstanceSwitcherState> = combine(instances.instances, instances.activeInstanceId) { list, id ->
        InstanceSwitcherState(list, id)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, InstanceSwitcherState())

    private val autoOpenEvents = Channel<SiteItem>(Channel.CONFLATED)

    /** Emits the remembered site once per launch, so the screen can open it. */
    val autoOpenSite: Flow<SiteItem> = autoOpenEvents.receiveAsFlow()

    private var loadJob: Job? = null
    private var liveJob: Job? = null

    init {
        viewModelScope.launch {
            instances.activeInstance
                .distinctUntilChangedBy { it?.id }
                .collect { active -> reload(active, keepCounts = false) }
        }
    }

    /** Pull-to-refresh: keeps the list on screen and reloads it along with the live counts. */
    fun refresh() {
        viewModelScope.launch {
            reload(instances.activeInstance.first(), keepCounts = true)
        }
    }

    fun onQueryChanged(query: String) {
        mutableState.update { state -> if (state is SitesUiState.Content) state.copy(query = query) else state }
    }

    /** Switches the active instance. The list reloads through the active-instance observer. */
    fun selectInstance(instanceId: String) {
        viewModelScope.launch { instances.setActive(instanceId) }
    }

    /** Remembers the tapped site on the active instance, so it can open automatically next launch. */
    fun onSiteTapped(site: SiteItem) {
        val activeId = switcher.value.activeId ?: return
        viewModelScope.launch { instances.setLastSelectedSiteId(activeId, site.analyticsId) }
    }

    private fun reload(active: Instance?, keepCounts: Boolean) {
        loadJob?.cancel()
        liveJob?.cancel()
        loadJob = viewModelScope.launch { loadSites(active, keepCounts) }
    }

    private suspend fun loadSites(active: Instance?, keepCounts: Boolean) {
        val previous = mutableState.value as? SitesUiState.Content
        mutableState.value = previous?.copy(isRefreshing = true) ?: SitesUiState.Loading
        if (active == null) {
            mutableState.value = SitesUiState.Content(organizations = emptyList())
            return
        }
        try {
            val groups = fetchGroups()
            val counts = if (keepCounts) previous?.liveCounts.orEmpty() else emptyMap()
            mutableState.value = SitesUiState.Content(
                organizations = groups,
                liveCounts = counts,
                query = previous?.query.orEmpty(),
            )
            loadLiveCounts(groups.flatMap { it.sites })
            offerAutoOpen(active.id, groups.flatMap { it.sites })
        } catch (error: ApiException) {
            mutableState.value = SitesUiState.Error(error)
        } catch (removed: NoActiveInstanceException) {
            // The server was removed while the list loaded; the active-instance observer shows whatever comes next.
            mutableState.value = SitesUiState.Content(organizations = emptyList())
        }
    }

    /** Organizations embed their sites; an organization that embeds none is asked for its sites directly. */
    private suspend fun fetchGroups(): List<OrganizationGroup> {
        val entries = analytics.organizations().map { org ->
            OrganizationSites(organization = org, sites = org.sites.ifEmpty { analytics.sites(org.id) })
        }
        return buildGroups(entries)
    }

    private fun loadLiveCounts(sites: List<SiteItem>) {
        liveJob = viewModelScope.launch {
            val permits = Semaphore(LIVE_COUNT_CONCURRENCY)
            sites.forEach { site ->
                launch {
                    permits.withPermit {
                        val count = liveCountOrNull(site.analyticsId) ?: return@withPermit
                        mutableState.update { recordLiveCount(it, site.analyticsId, count) }
                    }
                }
            }
        }
    }

    /** A failed count is not an error for the list; the row just shows no badge. */
    private suspend fun liveCountOrNull(siteId: String): Int? = try {
        analytics.liveUserCount(siteId)
    } catch (expected: ApiException) {
        null
    }

    private suspend fun offerAutoOpen(instanceId: String, sites: List<SiteItem>) {
        val remembered = instances.lastSelectedSiteId(instanceId).first() ?: return
        val target = sites.firstOrNull { it.analyticsId == remembered } ?: return
        if (autoOpenGate.tryClaim()) autoOpenEvents.trySend(target)
    }

    private fun recordLiveCount(state: SitesUiState, siteId: String, count: Int): SitesUiState =
        if (state is SitesUiState.Content) state.copy(liveCounts = state.liveCounts + (siteId to count)) else state
}
