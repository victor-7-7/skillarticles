package ru.skillbranch.skillarticles.data.delegates

import com.squareup.moshi.JsonAdapter
import ru.skillbranch.skillarticles.data.local.PrefManager
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

/** Делегат для переменной типа T. Тип Т должен быть одним из встроенных типов
 * T (Int, Long, Float, String, Boolean). ПРИСВАИВАНИЕ ЗНАЧЕНИЯ ПЕРЕМЕННОЙ.
 * Если тип не встроенный, то выбрасывается исключение. В остальных случаях
 * в настройках делается/меняется запись с ключом - имя переменной.
 * СЧИТЫВАНИЕ ЗНАЧЕНИЯ ПЕРЕМЕННОЙ. Если в настройках нет записи с ключом,
 * совпадающим с именем переменной, то будет возвращено дефолтное значение
 * defaultValue. Если такая запись в настройках есть, то ее значение (по
 * ключу - имя переменной) и будет возвращено. Цимес данного делегата в том,
 * что в течение сеанса работы приложения на девайсе ПРИ СЧИТЫВАНИИ переменной
 * обращение к настройкам будет лишь один единственный раз (и то если
 * считывание случилось раньше записи в переменную). Во всех остальных случаях
 * ПРИ СЧИТЫВАНИИ делегат будет возвращать значение внутреннего приватного поля
 * */
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

/** Делегат для переменной типа T. Тип Т должен быть способен конвертироваться
 * в Json-строку адаптером adapter и наоборот, извлекаться адаптером из строки.
 * ПРИСВАИВАНИЕ ЗНАЧЕНИЯ ПЕРЕМЕННОЙ. Если переменной присваивается значение
 * типа Т, то оно конвертируется адаптером в строку и строка записывается в
 * настройки с ключом - имя переменной. При присваивание переменной значения
 * null, соответствующая запись в настройках (если таковая имелась) удаляется
 * из настроек. СЧИТЫВАНИЕ ЗНАЧЕНИЯ ПЕРЕМЕННОЙ. Если в настройках нет записи
 * (типа String) с ключом, совпадающим с именем переменной, то будет возвращено
 * значение null. Если такая запись в настройках есть, то строка будет
 * конвертирована в объект Т и он будет возвращен в качестве значения переменной
 * из делегата (строка не может быть невалидной, поскольку создается этим же
 * самым адаптером при присваивании значения данной переменной). Цимес данного
 * делегата в том, что в течение сеанса работы приложения на девайсе ПРИ СЧИТЫВАНИИ
 * переменной (если она не равна null) обращение к настройкам будет лишь один
 * единственный раз (и то если считывание случилось раньше записи в переменную).
 * Во всех остальных случаях ПРИ СЧИТЫВАНИИ делегат будет возвращать значение
 * внутреннего приватного поля
 * */
class PrefObjDelegate<T>(
    private val adapter: JsonAdapter<T>
) : ReadWriteProperty<PrefManager, T?> {

    private var storedValue: T? = null

    override fun getValue(thisRef: PrefManager, property: KProperty<*>): T? {
        if (storedValue == null) {
            storedValue = thisRef.preferences.getString(property.name, null)
                ?.let { adapter.fromJson(it) }
        }
        return storedValue
    }

    override fun setValue(thisRef: PrefManager, property: KProperty<*>, value: T?) {
        storedValue = value
        thisRef.preferences.edit()
            // Если в метод putString вторым аргументом передается null, то
            // переменная с ключом property.name будет просто удалена из настроек
            .putString(property.name, value?.let { adapter.toJson(it) })
            .apply()
    }
}

