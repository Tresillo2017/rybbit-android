package app.rybbitclient.core.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

/** Screens or data sets that some Rybbit builds don't provide. */
enum class Feature {
    /** Cloud-only `/pages` report. */
    Pages,

    /** Web vitals. Self-hosted builds return empty percentiles. */
    Performance,
}

/**
 * In-memory, per-instance record of features the server reported as missing (404 `API_ROUTE_NOT_FOUND`).
 * Lives for the app process only, so a server upgrade is picked up on the next launch.
 */
class FeatureAvailability {

    private val unavailable = MutableStateFlow<Map<String, Set<Feature>>>(emptyMap())

    fun isAvailable(instanceId: String, feature: Feature): Boolean =
        unavailable.value[instanceId]?.contains(feature) != true

    fun observeUnavailable(instanceId: String): Flow<Set<Feature>> =
        unavailable.map { it[instanceId].orEmpty() }.distinctUntilChanged()

    fun markUnavailable(instanceId: String, feature: Feature) {
        unavailable.update { current ->
            current + (instanceId to current[instanceId].orEmpty() + feature)
        }
    }

    fun clear(instanceId: String) {
        unavailable.update { current -> current - instanceId }
    }
}
