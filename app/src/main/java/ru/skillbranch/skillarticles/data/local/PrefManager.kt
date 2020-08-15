package ru.skillbranch.skillarticles.data.local

import android.content.SharedPreferences
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.preference.PreferenceManager
import ru.skillbranch.skillarticles.App
import ru.skillbranch.skillarticles.data.delegates.PrefDelegate
import ru.skillbranch.skillarticles.data.delegates.PrefLiveDelegate
import ru.skillbranch.skillarticles.data.models.AppSettings

object PrefManager {
    internal val preferences: SharedPreferences by lazy {
        PreferenceManager.getDefaultSharedPreferences(App.appContext())
    }

    var isAuthorized by PrefDelegate(false)

    var isDarkMode by PrefDelegate(false)
    var isBigText by PrefDelegate(false)

    val isAuthLive: LiveData<Boolean> by PrefLiveDelegate(
        "isAuthorized", false, preferences
    )

    val appSettingsLive = MediatorLiveData<AppSettings>().apply {
        val isDarkModeLive: LiveData<Boolean> by PrefLiveDelegate(
            "isDarkMode", false, preferences
        )
        val isBigTextLive: LiveData<Boolean> by PrefLiveDelegate(
            "isBigText", false, preferences
        )
        value = AppSettings()

        addSource(isDarkModeLive) {
            value = value!!.copy(isDarkMode = it)
        }
        addSource(isBigTextLive) {
            value = value!!.copy(isBigText = it)
        }
    }
//        .distinctUntilChanged()

    fun clearAll() {
        preferences.edit()
            .clear()
            .apply()
    }
/*
    var storedString by PrefDelegate("")
    var storedFloat by PrefDelegate(0f)
    var storedInt by PrefDelegate(0)
    var storedLong by PrefDelegate(0L)
    */
}