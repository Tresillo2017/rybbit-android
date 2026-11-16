package app.rybbitclient.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.LoadingIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import app.rybbitclient.core.model.Instance
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

/**
 * Shown while the instance list loads from DataStore. Reports once whether any instance exists,
 * and the caller then replaces this route.
 */
@Composable
fun StartupRoute(
    instances: Flow<List<Instance>>,
    onReady: (hasInstances: Boolean) -> Unit,
) {
    val currentOnReady by rememberUpdatedState(onReady)
    LaunchedEffect(instances) {
        currentOnReady(instances.first().isNotEmpty())
    }
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        LoadingIndicator()
    }
}
