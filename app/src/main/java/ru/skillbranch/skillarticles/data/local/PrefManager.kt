package ru.skillbranch.skillarticles.data.local

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import ru.skillbranch.skillarticles.data.delegates.PrefDelegate

@SuppressLint("RestrictedApi")
class PrefManager(context: Context) {
    internal val preferences: SharedPreferences by lazy {
        context.getSharedPreferences("default", Context.MODE_PRIVATE)
    }

    fun clearAll() {
        preferences.edit()
            .clear()
            .apply()
    }

    var storedBoolean by PrefDelegate(false)
    var storedString by PrefDelegate("")
    var storedFloat by PrefDelegate(0f)
    var storedInt by PrefDelegate(0)
    var storedLong by PrefDelegate(0L)
}