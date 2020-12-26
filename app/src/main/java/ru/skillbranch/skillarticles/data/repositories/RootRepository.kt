package ru.skillbranch.skillarticles.data.repositories

import ru.skillbranch.skillarticles.data.local.PrefManager
import ru.skillbranch.skillarticles.data.remote.RestService
import ru.skillbranch.skillarticles.data.remote.req.LoginReq
import ru.skillbranch.skillarticles.data.remote.req.RegistrationReq
import javax.inject.Inject

class RootRepository @Inject constructor(
    private val prefs: PrefManager,
    private val network: RestService
) {
    fun isAuth() = prefs.isAuthLive

    /*fun appSettings(): LiveData<AppSettings> = prefs.appSettingsLive

    fun updateSettings(settings: AppSettings) {
        prefs.isDarkMode = settings.isDarkMode
        prefs.isBigText = settings.isBigText
    }*/

    // look at video (lecture 11, time code 01:49:00)
    suspend fun login(login: String, pass: String) {
        val auth = network.login(LoginReq(login, pass))
        // Если логин или пароль будет неверный, то возврата из
        // функции network.login не будет. Появится снэкбар с
        // сообщение "Wrong login or password" и все.
        prefs.profile = auth.user
        // lecture 11, time code 01:59:22
        prefs.accessToken = "Bearer ${auth.accessToken}"
        prefs.refreshToken = auth.refreshToken // Bearer is absent (02:13:57)
    }

    suspend fun register(name: String, email: String, pass: String) {
        val registerReq = RegistrationReq(name, email, pass)
        val auth = network.register(registerReq)
        prefs.profile = auth.user
        prefs.accessToken = "Bearer ${auth.accessToken}"
        prefs.refreshToken = auth.refreshToken
    }
}