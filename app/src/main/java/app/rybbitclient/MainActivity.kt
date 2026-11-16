package app.rybbitclient

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
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
        lifecycleScope.launch {
            container.instanceRepository.instances.first()
            instancesLoaded.value = true
        }

        setContent {
            RybbitTheme {
                CompositionLocalProvider(LocalAppContainer provides container) {
                    Surface(modifier = Modifier.fillMaxSize()) {
                        AppNavHost()
                    }
                }
            }
        }
    }
}
