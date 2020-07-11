package ru.skillbranch.skillarticles

import android.app.Application
import com.facebook.stetho.Stetho

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
        // TODO: set default Night Mode

        Stetho.initializeWithDefaults(this)
    }
}