package ru.skillbranch.skillarticles.data.delegates

import android.content.SharedPreferences
import androidx.lifecycle.LiveData
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

class PrefLiveDelegate<T>(
    private val fieldKey: String,
    private val defaultValue: T,
    private val prefs: SharedPreferences
) : ReadOnlyProperty<Any?, LiveData<T>> {

    private var storedValue: LiveData<T>? = null

    override fun getValue(thisRef: Any?, property: KProperty<*>): LiveData<T> {
        if (storedValue == null) {
            storedValue = SharedPreferenceLiveData(prefs, fieldKey, defaultValue)
        }
        return storedValue!!
    }
}

internal class SharedPreferenceLiveData<T>(
    var prefs: SharedPreferences,
    var key: String,
    var defValue: T
) : LiveData<T>() {
    private val prefsChangeListener =
        SharedPreferences.OnSharedPreferenceChangeListener { _, shKey ->
            if (shKey == key) {
                value = readValue(defValue)
            }
        }

    override fun onActive() {
        super.onActive()
        value = readValue(defValue)
        prefs.registerOnSharedPreferenceChangeListener(prefsChangeListener)
    }

    override fun onInactive() {
        prefs.unregisterOnSharedPreferenceChangeListener(prefsChangeListener)
        super.onInactive()
    }

    @Suppress("UNCHECKED_CAST")
    private fun readValue(default: T): T {
        return when (default) {
            is Int -> prefs.getInt(key, default as Int) as T
            is Long -> prefs.getLong(key, default as Long) as T
            is Float -> prefs.getFloat(key, default as Float) as T
            is String -> prefs.getString(key, default as String) as T
            is Boolean -> prefs.getBoolean(key, default as Boolean) as T
            else -> throw IllegalArgumentException(
                "Illegal preference type $default"
            )
        }
    }
}

