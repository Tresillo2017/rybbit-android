package app.rybbitclient

import android.app.Application
import app.rybbitclient.di.AppContainer

class RybbitApp : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
