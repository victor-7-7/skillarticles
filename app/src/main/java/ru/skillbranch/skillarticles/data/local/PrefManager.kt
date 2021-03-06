package ru.skillbranch.skillarticles.data.local

import android.content.Context
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatDelegate
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.distinctUntilChanged
import androidx.lifecycle.map
import androidx.preference.PreferenceManager
import com.squareup.moshi.Moshi
import ru.skillbranch.skillarticles.data.delegates.PrefDelegate
import ru.skillbranch.skillarticles.data.delegates.PrefLiveDelegate
import ru.skillbranch.skillarticles.data.delegates.PrefObjDelegate
import ru.skillbranch.skillarticles.data.delegates.PrefObjLiveDelegate
import ru.skillbranch.skillarticles.data.models.AppSettings
import ru.skillbranch.skillarticles.data.models.User
import ru.skillbranch.skillarticles.ui.RootActivity

class PrefManager(ctx: Context, moshi: Moshi) {
    internal val preferences: SharedPreferences by lazy {
        PreferenceManager.getDefaultSharedPreferences(ctx)
    }

    var isDarkMode by PrefDelegate(false)
    var isBigText by PrefDelegate(false)

    var accessToken by PrefDelegate("")
    var refreshToken by PrefDelegate("")
    var profile: User? by PrefObjDelegate(moshi.adapter(User::class.java))

    fun replaceAvatarUrl(url: String) {
        // Вызывается со страницы профиля, значит profile != null
        profile = profile!!.copy(avatar = url)
    }

    fun removeAvatar() {
        // Вызывается со страницы профиля, значит profile != null
        profile = profile!!.copy(avatar = "")
    }

    fun editProfile(name: String, about: String) {
        // Вызывается со страницы профиля, значит profile != null
        profile = profile!!.copy(name = name, about = about)
    }


    //===============================================================

    val isAuthLive: LiveData<Boolean> by lazy {
        val token: LiveData<String> by PrefLiveDelegate(
            "accessToken", "", preferences
        )
        // lecture 11, time code 02:14:35 &&
        // https://proandroiddev.com/livedata-transformations-4f120ac046fc
        token.map { it.isNotEmpty() }
    }
    val profileLive: LiveData<User?> by PrefObjLiveDelegate(
        "profile", moshi.adapter(User::class.java), preferences
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
    }.distinctUntilChanged()

    fun resetAllPrefs(root: RootActivity) {
        isDarkMode = false
        root.delegate.localNightMode = AppCompatDelegate.MODE_NIGHT_NO
        isBigText = false
        // Do user logout
        accessToken = ""
        refreshToken = ""
        profile = User("", "", "")
    }

    // Callback SharedPreferences.OnSharedPreferenceChangeListener
    // will not be triggered when preferences are cleared
    fun clearAllPrefs() = preferences.edit().clear().apply()
}