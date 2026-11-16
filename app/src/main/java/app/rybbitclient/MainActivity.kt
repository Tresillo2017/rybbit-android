package app.rybbitclient

import android.animation.ObjectAnimator
import android.animation.PropertyValuesHolder
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.view.animation.PathInterpolator
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.animation.doOnEnd
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.splashscreen.SplashScreenViewProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import app.rybbitclient.core.storage.AppSettings
import app.rybbitclient.core.storage.ThemeMode
import app.rybbitclient.core.ui.haptics.LocalHapticsEnabled
import app.rybbitclient.core.ui.theme.LocalReduceMotion
import app.rybbitclient.core.ui.theme.LocalSmoothCharts
import app.rybbitclient.feature.overview.rangeForPreset
import app.rybbitclient.core.ui.theme.RybbitTheme
import app.rybbitclient.di.LocalAppContainer
import app.rybbitclient.navigation.AppNavHost
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val instancesLoaded = MutableStateFlow(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        val splash = installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val container = (application as RybbitApp).container
        // Keep the system splash up until the stored instance list has loaded; the nav splash route then decides.
        splash.setKeepOnScreenCondition { !instancesLoaded.value }
        splash.setOnExitAnimationListener { provider -> animateSplashOut(provider) }
        lifecycleScope.launch {
            container.instanceRepository.instances.first()
            rangeForPreset(container.appPreferences.current().defaultRangeKey)?.let(container.siteQueryState::setRange)
            instancesLoaded.value = true
        }

        setContent {
            val settings by container.appPreferences.settings.collectAsStateWithLifecycle(initialValue = AppSettings())
            val darkTheme = when (settings.themeMode) {
                ThemeMode.System -> isSystemInDarkTheme()
                ThemeMode.Light -> false
                ThemeMode.Dark -> true
            }
            SystemBarsFollowTheme(darkTheme)
            RybbitTheme(
                darkTheme = darkTheme,
                dynamicColor = settings.dynamicColor,
                accent = settings.accent,
                pureBlack = settings.pureBlack,
            ) {
                CompositionLocalProvider(
                    LocalAppContainer provides container,
                    LocalHapticsEnabled provides settings.hapticsEnabled,
                    LocalReduceMotion provides settings.reduceMotion,
                    LocalSmoothCharts provides settings.smoothCharts,
                ) {
                    Surface(modifier = Modifier.fillMaxSize()) {
                        AppNavHost()
                    }
                }
            }
        }
    }

    /**
     * Keeps status and navigation bar icons legible when the in-app theme differs from the system one: the default
     * edge-to-edge setup only follows the system dark mode.
     */
    @Composable
    private fun SystemBarsFollowTheme(darkTheme: Boolean) {
        DisposableEffect(darkTheme) {
            enableEdgeToEdge(
                statusBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT) { darkTheme },
                navigationBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT) { darkTheme },
            )
            onDispose { }
        }
    }

    /**
     * Lets the splash icon swell and fade out over the app's first frame instead of cutting away. The platform
     * animator scale applies, so "Remove animations" ends it instantly.
     */
    private fun animateSplashOut(provider: SplashScreenViewProvider) {
        val view = provider.view
        val icon = provider.iconView
        val fade = ObjectAnimator.ofFloat(view, View.ALPHA, 1f, 0f).apply {
            duration = SPLASH_EXIT_MILLIS
            interpolator = PathInterpolator(EMPHASIZED_X1, EMPHASIZED_Y1, EMPHASIZED_X2, EMPHASIZED_Y2)
        }
        val swell = ObjectAnimator.ofPropertyValuesHolder(
            icon,
            PropertyValuesHolder.ofFloat(View.SCALE_X, 1f, SPLASH_SWELL_SCALE),
            PropertyValuesHolder.ofFloat(View.SCALE_Y, 1f, SPLASH_SWELL_SCALE),
        ).apply {
            duration = SPLASH_EXIT_MILLIS
            interpolator = PathInterpolator(EMPHASIZED_X1, EMPHASIZED_Y1, EMPHASIZED_X2, EMPHASIZED_Y2)
        }
        fade.doOnEnd { provider.remove() }
        swell.start()
        fade.start()
    }

    private companion object {
        const val SPLASH_EXIT_MILLIS = 400L
        const val SPLASH_SWELL_SCALE = 1.25f

        // Material 3 "emphasized decelerate" control points.
        const val EMPHASIZED_X1 = 0.05f
        const val EMPHASIZED_Y1 = 0.7f
        const val EMPHASIZED_X2 = 0.1f
        const val EMPHASIZED_Y2 = 1f
    }
}
