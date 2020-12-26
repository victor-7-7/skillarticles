package ru.skillbranch.skillarticles.data.repositories

import androidx.lifecycle.LiveData
import okhttp3.MultipartBody
import ru.skillbranch.skillarticles.data.local.PrefManager
import ru.skillbranch.skillarticles.data.models.User
import ru.skillbranch.skillarticles.data.remote.RestService
import ru.skillbranch.skillarticles.data.remote.req.EditProfileReq
import javax.inject.Inject

interface IProfileRepository : IRepository {
    fun getProfile(): LiveData<User?>
    suspend fun uploadAvatar(reqPart: MultipartBody.Part)
    suspend fun removeAvatar()
    suspend fun editProfile(name: String, about: String)
}

class ProfileRepository @Inject constructor(
    private val prefs: PrefManager,
    private val network: RestService
) : IProfileRepository {
    override fun getProfile(): LiveData<User?> = prefs.profileLive

    override suspend fun uploadAvatar(reqPart: MultipartBody.Part) {
        // Круглые скобки позволяют деструктурировать возвращаемый
        // объект UploadRes
        val (url) = network.upload(reqPart, prefs.accessToken)
        prefs.replaceAvatarUrl(url)
    }

    override suspend fun removeAvatar() {
        network.remove(prefs.accessToken)
        prefs.removeAvatar()
    }

    override suspend fun editProfile(name: String, about: String) {
        network.edit(EditProfileReq(name, about), prefs.accessToken)
        prefs.editProfile(name, about)
    }
}