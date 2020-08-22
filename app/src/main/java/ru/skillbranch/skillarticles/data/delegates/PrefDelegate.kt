package ru.skillbranch.skillarticles.data.delegates

import com.squareup.moshi.JsonAdapter
import ru.skillbranch.skillarticles.data.local.PrefManager
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

class PrefDelegate<T>(private val defaultValue: T) : ReadWriteProperty<PrefManager, T> {

    private var storedValue: T? = null

    override fun getValue(thisRef: PrefManager, property: KProperty<*>): T {
        if (storedValue == null) {
            with(thisRef.preferences) {
                @Suppress("UNCHECKED_CAST")
                storedValue = when (defaultValue) {
                    is Int -> getInt(property.name, defaultValue) as T
                    is Long -> getLong(property.name, defaultValue) as T
                    is Float -> getFloat(property.name, defaultValue) as T
                    is String -> getString(property.name, defaultValue) as T
                    is Boolean -> getBoolean(property.name, defaultValue) as T
                    else -> throw IllegalArgumentException(
                        "Illegal preference type $storedValue"
                    )
                }
            }
        }
        return storedValue!!
    }

    override fun setValue(thisRef: PrefManager, property: KProperty<*>, value: T) {
        if (storedValue != null && storedValue == value) return
        with(thisRef.preferences.edit()) {
            when (value) {
                is Boolean -> putBoolean(property.name, value)
                is String -> putString(property.name, value)
                is Float -> putFloat(property.name, value)
                is Int -> putInt(property.name, value)
                is Long -> putLong(property.name, value)
                else -> throw IllegalArgumentException("Illegal preference type $value")
            }
            apply()
        }
        storedValue = value
    }
}

class PrefObjDelegate<T>(
    private val adapter: JsonAdapter<T>
) : ReadWriteProperty<PrefManager, T?> {

    private var storedValue: T? = null

    override fun getValue(thisRef: PrefManager, property: KProperty<*>): T? {
        if (storedValue == null) {
            thisRef.preferences.getString(property.name, null)
                ?.let { adapter.fromJson(it) }
        }
        return storedValue
    }

    override fun setValue(thisRef: PrefManager, property: KProperty<*>, value: T?) {
        storedValue = value
        thisRef.preferences.edit()
            .putString(property.name, value?.let { adapter.toJson(it) })
            .apply()
    }
}

