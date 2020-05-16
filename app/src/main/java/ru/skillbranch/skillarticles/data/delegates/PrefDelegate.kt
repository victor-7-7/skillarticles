package ru.skillbranch.skillarticles.data.delegates

import ru.skillbranch.skillarticles.data.local.PrefManager
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

class PrefDelegate<T>(private val defaultValue: T) : ReadWriteProperty<PrefManager, T?> {

    @Suppress("UNCHECKED_CAST")
    override fun getValue(thisRef: PrefManager, property: KProperty<*>): T? =
        if (thisRef.preferences.contains(property.name))
            thisRef.preferences.all[property.name] as T
        else defaultValue

    override fun setValue(thisRef: PrefManager, property: KProperty<*>, value: T?) {
        with(thisRef.preferences.edit()) {
            when (value) {
                is Boolean -> putBoolean(property.name, value as Boolean).apply()
                is String -> putString(property.name, value as String).apply()
                is Float -> putFloat(property.name, value as Float).apply()
                is Int -> putInt(property.name, value as Int).apply()
                is Long -> putLong(property.name, value as Long).apply()
                else -> throw IllegalStateException(
                    "Invalid pref type $value. " +
                            "Only Boolean, String, Float, Int, Long types are allowed"
                )
            }
        }
    }
}