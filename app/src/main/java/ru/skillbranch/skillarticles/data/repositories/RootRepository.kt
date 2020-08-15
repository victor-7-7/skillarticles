package ru.skillbranch.skillarticles.data.repositories

import androidx.lifecycle.LiveData
import ru.skillbranch.skillarticles.data.local.PrefManager
import ru.skillbranch.skillarticles.data.models.AppSettings

object RootRepository {

    fun isAuth() = PrefManager.isAuthLive

    fun setAuth(auth: Boolean) {
        PrefManager.isAuthorized = auth
    }

    fun appSettings(): LiveData<AppSettings> = PrefManager.appSettingsLive

    fun updateSettings(settings: AppSettings) {
        PrefManager.isDarkMode = settings.isDarkMode
        PrefManager.isBigText = settings.isBigText
    }
}