package ru.skillbranch.skillarticles.data.repositories

import androidx.lifecycle.LiveData
import ru.skillbranch.skillarticles.data.local.PrefManager
import ru.skillbranch.skillarticles.data.models.AppSettings
import ru.skillbranch.skillarticles.data.remote.NetworkManager
import ru.skillbranch.skillarticles.data.remote.req.LoginReq

object RootRepository {
    private val prefManager = PrefManager
    private val network = NetworkManager.api

    fun isAuth() = prefManager.isAuthLive

    /*
        fun setAuth(auth: Boolean) {
            prefManager.isAuthorized = auth
        }
    */
    fun appSettings(): LiveData<AppSettings> = prefManager.appSettingsLive

    fun updateSettings(settings: AppSettings) {
        prefManager.isDarkMode = settings.isDarkMode
        prefManager.isBigText = settings.isBigText
    }

    // look at video (lecture 11, time code 01:49:00)
    suspend fun login(login: String, pass: String) {
        val auth = network.login(LoginReq(login, pass))
        // Если логин или пароль будет неверный, то возврата из
        // функции network.login не будет. Появится снэкбар с
        // сообщение "Wrong login or password" и все.
        prefManager.profile = auth.user
        // lecture 11, time code 01:59:22
        prefManager.accessToken = "Bearer ${auth.accessToken}"
        prefManager.refreshToken = auth.refreshToken // Bearer is absent (02:13:57)
    }
}