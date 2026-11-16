package app.rybbitclient.screenshots

import androidx.activity.ComponentActivity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onRoot
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import app.rybbitclient.core.data.AnalyticsRepository
import app.rybbitclient.core.model.Instance
import app.rybbitclient.core.model.InstanceKind
import app.rybbitclient.core.network.Credential
import app.rybbitclient.core.query.DateRange
import app.rybbitclient.core.storage.InMemoryKeyValueStore
import app.rybbitclient.core.storage.InMemorySecretStore
import app.rybbitclient.core.storage.InstanceRepository
import app.rybbitclient.core.ui.theme.RybbitTheme
import app.rybbitclient.di.AppContainer
import app.rybbitclient.di.LocalAppContainer
import app.rybbitclient.feature.errors.ErrorDetailScreen
import app.rybbitclient.feature.errors.ErrorDetailViewModel
import app.rybbitclient.feature.errors.ErrorsScreen
import app.rybbitclient.feature.errors.ErrorsViewModel
import app.rybbitclient.feature.events.EventsScreen
import app.rybbitclient.feature.events.EventsViewModel
import app.rybbitclient.feature.goals.FunnelDetailScreen
import app.rybbitclient.feature.goals.FunnelDetailViewModel
import app.rybbitclient.feature.goals.GoalsScreen
import app.rybbitclient.feature.goals.GoalsViewModel
import app.rybbitclient.feature.more.MoreScreen
import app.rybbitclient.feature.more.MoreViewModel
import app.rybbitclient.feature.onboarding.AddInstanceScreen
import app.rybbitclient.feature.onboarding.AddInstanceViewModel
import app.rybbitclient.feature.onboarding.FakeInstanceAuthenticator
import app.rybbitclient.feature.onboarding.WelcomeScreen
import app.rybbitclient.feature.overview.OverviewScreen
import app.rybbitclient.feature.overview.OverviewViewModel
import app.rybbitclient.feature.retention.RetentionScreen
import app.rybbitclient.feature.retention.RetentionViewModel
import app.rybbitclient.feature.settings.SettingsScreen
import app.rybbitclient.feature.settings.SettingsViewModel
import app.rybbitclient.feature.sessions.SessionDetailScreen
import app.rybbitclient.feature.sessions.SessionDetailViewModel
import app.rybbitclient.feature.sessions.SessionsScreen
import app.rybbitclient.feature.sessions.SessionsViewModel
import app.rybbitclient.feature.sites.SitesScreen
import app.rybbitclient.feature.sites.SitesViewModel
import app.rybbitclient.feature.users.UsersScreen
import app.rybbitclient.feature.users.UsersViewModel
import com.github.takahirom.roborazzi.captureRoboImage
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.time.LocalDate
import java.util.Locale
import java.util.TimeZone

/**
 * Renders the real Compose screens on the JVM (Robolectric native graphics) and records them as PNGs
 * under docs/screenshots. Regenerate with `JAVA_HOME=<jdk17+> ./gradlew recordRoborazziDebug`; the
 * default `testDebugUnitTest` run excludes this package.
 *
 * Every screen is backed by its real ViewModel, fed by [FixtureAnalyticsRepository] (captured
 * responses from docs/fixtures). The ViewModels are created up front under the same keys the screens
 * use, so `viewModel(key = ...)` picks them up instead of building its own with the live repository.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w411dp-h891dp-xxhdpi")
class ScreenshotTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private val repository: AnalyticsRepository = FixtureAnalyticsRepository()
    private val instances = InstanceRepository(InMemoryKeyValueStore(), InMemorySecretStore())
    private lateinit var container: AppContainer
    private var previousLocale: Locale? = null
    private var previousZone: TimeZone? = null

    @Before
    fun setUp() {
        previousLocale = Locale.getDefault()
        previousZone = TimeZone.getDefault()
        Locale.setDefault(Locale.US)
        TimeZone.setDefault(TimeZone.getTimeZone(SITE_ZONE))

        container = AppContainer(RuntimeEnvironment.getApplication())
        container.siteQueryState.onSiteOpened(SITE_ID)
        container.siteQueryState.setRange(
            DateRange.Custom(LocalDate.of(2026, 11, 10), LocalDate.of(2026, 11, 16)),
        )
        runBlocking {
            instances.add(DEMO_INSTANCE, Credential.ApiKey(DEMO_KEY))
            instances.setActive(DEMO_INSTANCE.id)
        }
        composeRule.mainClock.autoAdvance = false
    }

    @After
    fun tearDown() {
        previousLocale?.let { Locale.setDefault(it) }
        previousZone?.let { TimeZone.setDefault(it) }
    }

    @Test
    fun welcome() = capture("01-welcome") {
        WelcomeScreen(onAddCloud = {}, onAddSelfHosted = {})
    }

    @Test
    fun addServer() {
        val viewModel = preseed<AddInstanceViewModel>(key = "add-instance-false") {
            AddInstanceViewModel(
                isCloud = false,
                authenticator = FakeInstanceAuthenticator(),
                instances = instances,
            )
        }
        viewModel.onServerInputChanged(INSECURE_SERVER)
        capture("02-add-server") {
            AddInstanceScreen(isCloud = false, onInstanceAdded = {}, onBack = {})
        }
    }

    @Test
    fun sites() {
        preseed(key = "sites") { SitesViewModel(repository, instances) }
        capture("03-sites") {
            SitesScreen(onSiteSelected = { _, _ -> }, onOpenSettings = {}, onAddInstance = {})
        }
    }

    @Test
    fun overviewLight() = overview("04-overview-light", darkTheme = false)

    @Test
    fun overviewDark() = overview("05-overview-dark", darkTheme = true)

    @Test
    fun sessions() {
        preseed(key = "sessions-$SITE_ID") {
            SessionsViewModel(SITE_ID, repository, container.siteQueryState.query)
        }
        capture("06-sessions") { SessionsScreen(siteId = SITE_ID, onOpenSession = {}) }
    }

    @Test
    fun sessionDetail() {
        preseed(key = "session-$SITE_ID-$SESSION_ID") {
            SessionDetailViewModel(SITE_ID, SESSION_ID, repository)
        }
        capture("07-session-detail") {
            SessionDetailScreen(siteId = SITE_ID, sessionId = SESSION_ID, onOpenUser = {}, onBack = {})
        }
    }

    @Test
    fun events() {
        preseed(key = "events-$SITE_ID") { EventsViewModel(SITE_ID, repository, container.siteQueryState) }
        capture("08-events") { EventsScreen(siteId = SITE_ID, onOpenEvent = {}) }
    }

    @Test
    fun users() {
        preseed(key = "users-$SITE_ID") {
            UsersViewModel(SITE_ID, repository, container.siteQueryState.query)
        }
        capture("09-users") { UsersScreen(siteId = SITE_ID, onOpenUser = {}) }
    }

    @Test
    fun moreMenu() {
        preseed(key = "more-$SITE_ID") { MoreViewModel(SITE_ID, repository, instances) }
        capture("10-more") {
            MoreScreen(
                siteId = SITE_ID,
                onOpenGoals = {},
                onOpenFunnels = {},
                onOpenErrors = {},
                onOpenRetention = {},
                onOpenRealtime = {},
                onOpenWebVitals = {},
            )
        }
    }

    @Test
    fun goals() {
        preseed(key = "goals-$SITE_ID") { GoalsViewModel(SITE_ID, repository, container.siteQueryState) }
        capture("11-goals") { GoalsScreen(siteId = SITE_ID, onOpenSession = {}, onBack = {}) }
    }

    @Test
    fun funnelDetail() {
        preseed(key = "funnel-$SITE_ID-$FUNNEL_ID") {
            FunnelDetailViewModel(SITE_ID, FUNNEL_ID, repository, container.siteQueryState)
        }
        capture("12-funnel-detail") {
            FunnelDetailScreen(siteId = SITE_ID, funnelId = FUNNEL_ID, onBack = {})
        }
    }

    @Test
    fun retention() {
        preseed(key = "retention-$SITE_ID") { RetentionViewModel(SITE_ID, repository) }
        capture("13-retention") { RetentionScreen(siteId = SITE_ID, onBack = {}) }
    }

    @Test
    fun errors() {
        preseed(key = "errors-$SITE_ID") { ErrorsViewModel(SITE_ID, repository, container.siteQueryState) }
        capture("14-errors") { ErrorsScreen(siteId = SITE_ID, onOpenError = {}, onBack = {}) }
    }

    @Test
    fun errorDetail() {
        preseed(key = "error-$SITE_ID-$ERROR_MESSAGE") {
            ErrorDetailViewModel(SITE_ID, ERROR_MESSAGE, repository, container.siteQueryState)
        }
        capture("15-error-detail") {
            ErrorDetailScreen(siteId = SITE_ID, errorMessage = ERROR_MESSAGE, onOpenSession = {}, onBack = {})
        }
    }

    @Test
    fun settings() {
        preseed(key = "settings") {
            SettingsViewModel(instances, FakeInstanceAuthenticator(), remover = {})
        }
        capture("16-settings") {
            SettingsScreen(
                onBack = {},
                onAddInstance = {},
                onActiveInstanceChanged = {},
                onAllInstancesRemoved = {},
            )
        }
    }

    private fun overview(name: String, darkTheme: Boolean) {
        preseed(key = "overview-$SITE_ID") {
            OverviewViewModel(SITE_ID, repository, container.siteQueryState)
        }
        capture(name, darkTheme = darkTheme) { OverviewScreen(siteId = SITE_ID, onOpenRealtime = {}) }
    }

    /**
     * Creates the ViewModel under [key] in the activity's store, so the screen's `viewModel(key = ...)`
     * call returns this instance. The initializer runs once, here, with the fixture-backed dependencies.
     */
    private inline fun <reified VM : ViewModel> preseed(key: String, crossinline create: () -> VM): VM {
        val factory = viewModelFactory { initializer { create() } }
        return ViewModelProvider(composeRule.activity, factory)[key, VM::class.java]
    }

    /**
     * Sets [content] inside the app's providers and theme, lets loading settle, and records the root.
     * The mainClock is paused so shimmers and progress indicators don't keep the test from going idle;
     * the fixed advance lets the composition and any state-driven animations finish.
     */
    private fun capture(name: String, darkTheme: Boolean = false, content: @Composable () -> Unit) {
        composeRule.setContent {
            CompositionLocalProvider(LocalAppContainer provides container) {
                RybbitTheme(darkTheme = darkTheme, dynamicColor = false) {
                    Surface(modifier = Modifier, color = MaterialTheme.colorScheme.background) {
                        content()
                    }
                }
            }
        }
        composeRule.waitForIdle()
        composeRule.mainClock.advanceTimeBy(SETTLE_MILLIS)
        composeRule.waitForIdle()
        composeRule.onRoot().captureRoboImage("$OUTPUT_DIR/$name.png")
    }

    private companion object {
        /** Gradle runs unit tests from the module directory (app/). */
        const val OUTPUT_DIR = "../docs/screenshots"
        const val SETTLE_MILLIS = 2_000L
        const val SITE_ID = "1"
        const val SITE_ZONE = "Europe/Madrid"
        const val SESSION_ID = "iAyU8X03pz02UR"
        const val FUNNEL_ID = 1
        const val ERROR_MESSAGE = "Cannot read properties of undefined (reading 'map')"
        const val INSECURE_SERVER = "http://192.168.1.20:3001"
        const val DEMO_KEY = "demo-api-key"

        val DEMO_INSTANCE = Instance(
            id = "demo-instance",
            displayName = "Home server",
            baseUrl = "http://192.168.1.20:3001",
            kind = InstanceKind.SelfHosted,
            authType = app.rybbitclient.core.model.AuthType.ApiKey,
            accountLabel = "Demo Org",
            createdAtMillis = 1_793_000_000_000L,
        )
    }
}
