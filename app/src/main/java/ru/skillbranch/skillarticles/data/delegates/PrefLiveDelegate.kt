package ru.skillbranch.skillarticles.data.delegates

import android.content.SharedPreferences
import androidx.lifecycle.LiveData
import com.squareup.moshi.JsonAdapter
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

class PrefLiveDelegate<T>(
    private val key: String,
    private val defaultValue: T,
    private val prefs: SharedPreferences
) : ReadOnlyProperty<Any?, LiveData<T>> {

    private var storedLiveValue: LiveData<T>? = null

    override fun getValue(thisRef: Any?, property: KProperty<*>): LiveData<T> {
        if (storedLiveValue == null) {
            storedLiveValue = PrefLiveData(key, defaultValue, prefs)
        }
        return storedLiveValue!!
    }
}

internal class PrefLiveData<T>(
    private var key: String,
    private var defValue: T,
    private var prefs: SharedPreferences
) : LiveData<T>() {
    // https://developer.android.com/reference/kotlin/android/content/SharedPreferences.OnSharedPreferenceChangeListener
    // Called when a shared preference is changed, added, or removed.
    // This may be called even if a preference is set to its existing value.
    // Callback will be run on your main thread. First param - SharedPreferences.
    // Second param - key of the preference that was changed, added, or removed.
    // Коллбэк не будет запущен, когда preferences очищены через Editor#clear().
    // Если приложение таргетирует SDK 30 и запущено на девайсе с версией ОС 30
    // или выше, то коллбэк сработает, но во втором параметре будет null
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

//=======================================================================
class PrefObjLiveDelegate<T>(
    private val key: String,
    private val adapter: JsonAdapter<T>,
    private val prefs: SharedPreferences
) : ReadOnlyProperty<Any?, LiveData<T?>> {

    private var storedLiveValue: LiveData<T?>? = null

    override fun getValue(thisRef: Any?, property: KProperty<*>): LiveData<T?> {
        if (storedLiveValue == null) {
            storedLiveValue = PrefObjLiveData(key, adapter, prefs)
        }
        return storedLiveValue!!
    }
}

internal class PrefObjLiveData<T>(
    private var key: String,
    private var adapter: JsonAdapter<T>,
    private var prefs: SharedPreferences
) : LiveData<T?>() {
    private val prefsChangeListener =
        SharedPreferences.OnSharedPreferenceChangeListener { _, shKey ->
            if (shKey == key) {
                value = readValue()
            }
        }

    override fun onActive() {
        super.onActive()
        value = readValue()
        prefs.registerOnSharedPreferenceChangeListener(prefsChangeListener)
    }

    override fun onInactive() {
        prefs.unregisterOnSharedPreferenceChangeListener(prefsChangeListener)
        super.onInactive()
    }

    @Suppress("UNCHECKED_CAST")
    private fun readValue(): T? = prefs.getString(key, null)
        ?.let { adapter.fromJson(it) }
}

