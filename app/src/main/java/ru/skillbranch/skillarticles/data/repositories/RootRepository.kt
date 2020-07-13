package ru.skillbranch.skillarticles.data.repositories

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import ru.skillbranch.skillarticles.data.local.PrefManager

object RootRepository {

    private var isAuth = MutableLiveData(PrefManager.authorization())

    fun isAuth(): LiveData<Boolean> = isAuth

    fun setAuth(auth: Boolean) {
        PrefManager.setAuthorization(auth)
        isAuth.value = auth
    }
}