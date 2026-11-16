package app.rybbitclient.di

import android.content.Context
import android.os.Build
import app.rybbitclient.BuildConfig
import app.rybbitclient.core.auth.AuthService
import app.rybbitclient.core.data.AnalyticsRepository
import app.rybbitclient.core.data.DefaultAnalyticsRepository
import app.rybbitclient.core.data.FeatureAvailability
import app.rybbitclient.core.data.RybbitApiProvider
import app.rybbitclient.core.network.HttpClientFactory
import app.rybbitclient.core.query.SiteQueryState
import app.rybbitclient.core.storage.DataStoreKeyValueStore
import app.rybbitclient.core.storage.InstanceRepository
import app.rybbitclient.core.storage.KeystoreSecretCipher
import app.rybbitclient.core.storage.KeystoreSecretStore
import app.rybbitclient.core.storage.rybbitPreferencesStore
import app.rybbitclient.core.storage.rybbitSecretsStore

/**
 * Manual dependency graph (no Hilt, no KSP). Built once in [app.rybbitclient.RybbitApp] and shared
 * through [LocalAppContainer]. Every collaborator is created here so tests can rebuild the graph with fakes.
 */
class AppContainer(context: Context) {

    val httpClientFactory: HttpClientFactory =
        HttpClientFactory(userAgent = HttpClientFactory.userAgent(BuildConfig.VERSION_NAME))

    val instanceRepository: InstanceRepository = InstanceRepository(
        store = DataStoreKeyValueStore(context.applicationContext.rybbitPreferencesStore),
        secrets = KeystoreSecretStore(
            cipher = KeystoreSecretCipher(),
            store = DataStoreKeyValueStore(context.applicationContext.rybbitSecretsStore),
        ),
    )

    val authService: AuthService = AuthService(httpClientFactory, deviceModel = Build.MODEL)

    val featureAvailability: FeatureAvailability = FeatureAvailability()

    val apiProvider: RybbitApiProvider = RybbitApiProvider(instanceRepository, httpClientFactory)

    val analyticsRepository: AnalyticsRepository =
        DefaultAnalyticsRepository(instanceRepository, apiProvider, featureAvailability)

    /** Date range + filters shared across all tabs of the site being viewed. */
    val siteQueryState: SiteQueryState = SiteQueryState()

    /**
     * Removes an instance everywhere it is held: the credential and metadata (via the repository),
     * its cached HTTP client, and its feature-availability record.
     */
    suspend fun removeInstance(instanceId: String) {
        instanceRepository.remove(instanceId)
        apiProvider.evict(instanceId)
        featureAvailability.clear(instanceId)
    }
}
