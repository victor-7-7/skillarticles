package ru.skillbranch.skillarticles.data.repositories

import androidx.lifecycle.LiveData
import okhttp3.MultipartBody
import ru.skillbranch.skillarticles.data.local.PrefManager
import ru.skillbranch.skillarticles.data.models.User
import ru.skillbranch.skillarticles.data.remote.NetworkManager

object ProfileRepository {
    private val prefs = PrefManager
    private val network by lazy { NetworkManager.api }

    fun getProfile(): LiveData<User?> = prefs.profileLive

    suspend fun uploadAvatar(reqPart: MultipartBody.Part) {
        // Круглые скобки позволяют деструктурировать возвращаемый
        // объект UploadRes
        val (url) = network.upload(reqPart, prefs.accessToken)
        prefs.replaceAvatarUrl(url)
    }

    suspend fun removeAvatar() {
        network.remove(prefs.accessToken)
        prefs.removeAvatar()
    }
}