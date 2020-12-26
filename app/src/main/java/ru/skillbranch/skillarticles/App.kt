package ru.skillbranch.skillarticles

import android.app.Application
import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import dagger.hilt.android.HiltAndroidApp
import ru.skillbranch.skillarticles.data.local.PrefManager
import ru.skillbranch.skillarticles.data.remote.NetworkMonitor
import javax.inject.Inject

@HiltAndroidApp
class App : Application() {
    companion object {
        private var instance: App? = null
        fun appContext(): Context = instance!!.applicationContext
    }

    @Inject
    lateinit var monitor: NetworkMonitor

    @Inject
    lateinit var prefs: PrefManager

    init {
        instance = this
    }

    override fun onCreate() {
        super.onCreate()

        // start network monitoring
        monitor.registerNetworkMonitor()

        val mode = if (prefs.isDarkMode) AppCompatDelegate.MODE_NIGHT_YES
        else AppCompatDelegate.MODE_NIGHT_NO
        AppCompatDelegate.setDefaultNightMode(mode)
    }
}