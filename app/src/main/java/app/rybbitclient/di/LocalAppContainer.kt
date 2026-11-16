package app.rybbitclient.di

import androidx.compose.runtime.staticCompositionLocalOf

/** Provides the [AppContainer] to composables. Provided once in MainActivity via `CompositionLocalProvider`. */
val LocalAppContainer = staticCompositionLocalOf<AppContainer> {
    error("AppContainer was not provided. Wrap content in CompositionLocalProvider(LocalAppContainer provides container).")
}
