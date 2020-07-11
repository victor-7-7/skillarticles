package ru.skillbranch.skillarticles.data.local

import android.content.SharedPreferences
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.preference.PreferenceManager
import ru.skillbranch.skillarticles.App
import ru.skillbranch.skillarticles.data.delegates.PrefDelegate
import ru.skillbranch.skillarticles.data.models.AppSettings


object PrefManager {
    internal val preferences: SharedPreferences by lazy {
        PreferenceManager.getDefaultSharedPreferences(App.appContext())
    }
    private var isAuthorized by PrefDelegate(false)

    private var isDarkMode by PrefDelegate(false)
    private var isBigText by PrefDelegate(false)

    fun authorization() = isAuthorized

    fun setAuthorization(auth: Boolean) {
        isAuthorized = auth
    }

    fun getAppSettings(): LiveData<AppSettings> =
        MutableLiveData(AppSettings(isDarkMode, isBigText))


    fun updateAppSettings(settings: AppSettings) {
        if (isDarkMode != settings.isDarkMode) isDarkMode = settings.isDarkMode
        if (isBigText != settings.isBigText) isBigText = settings.isBigText
    }

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