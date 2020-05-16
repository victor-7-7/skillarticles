package ru.skillbranch.skillarticles.data.local

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import androidx.preference.PreferenceManager

@SuppressLint("RestrictedApi")
class PrefManager(context: Context) : PreferenceManager(context) {
    val preferences: SharedPreferences = getDefaultSharedPreferences(context)

    fun clearAll() {
        preferences.edit()
            .clear()
            .apply()
    }
}