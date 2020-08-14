package ru.skillbranch.skillarticles.data.repositories

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import ru.skillbranch.skillarticles.data.local.PrefManager
import ru.skillbranch.skillarticles.data.models.AppSettings

object RootRepository {

    private var isAuth = MutableLiveData(PrefManager.isAuthorized)

//    private var appSettings = MutableLiveData(
//        AppSettings(
//            PrefManager.isDarkMode, PrefManager.isBigText
//        )
//    )

    fun isAuth(): LiveData<Boolean> = isAuth

    fun setAuth(auth: Boolean) {
        PrefManager.isAuthorized = auth
        isAuth.value = auth
    }

    fun appSettings(): LiveData<AppSettings> = PrefManager.appSettings

    fun updateSettings(settings: AppSettings) {
        PrefManager.appSettings.value = settings
    }
}