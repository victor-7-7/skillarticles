package ru.skillbranch.skillarticles

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate
import com.facebook.stetho.Stetho
import ru.skillbranch.skillarticles.data.local.PrefManager

class App : Application() {
    companion object {
        private var instance: App? = null
        fun appContext() = instance!!.applicationContext
    }

    init {
        instance = this
    }

    override fun onCreate() {
        super.onCreate()
        val dark = PrefManager.getAppSettings().value?.isDarkMode
        if (dark != null) {
            val mode = if (dark) AppCompatDelegate.MODE_NIGHT_YES
            else AppCompatDelegate.MODE_NIGHT_NO
            AppCompatDelegate.setDefaultNightMode(mode)
        }
        Stetho.initializeWithDefaults(this)
    }
}