package ru.skillbranch.skillarticles.ui.custom.delegates

import ru.skillbranch.skillarticles.ui.base.Binding
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

/** Делегат для переменной типа Т. Объект RenderProp должен быть
 * объявлен внутри производного от Binding класса. У делегата может
 * быть хэндлер, заданный при создании RenderProp, а также могут быть
 * хэндлеры, добавленные (методом addListener) впоследствии. Все
 * эти хэндлеры будут срабатывать при изменении переменной Т */
class RenderProp<T : Any>(
    var variable: T,
    private val needInit: Boolean = true,
    private val onChange: ((T) -> Unit)? = null
) : ReadWriteProperty<Binding, T> {
    // Список хэндлеров, срабатывающих при изменении
    // инкапсулированной переменной
    private val listeners: MutableList<() -> Unit> = mutableListOf()

    /** Метод вызывается при восстановлении фрагмента, в котором есть
     * binding (не равный null) и данный объект RenderProp принадлежит
     * этому binding. Если у объекта RenderProp свойство needInit не
     * равно false и лямбда onChange не пустая, то эта лямбда будет
     * вызвана с передачей ей значения variable */
    fun initialBind() {
        if (needInit) onChange?.invoke(variable)
    }

    /** Метод вызывается при создании объекта RenderProp. С его помощью можно
     * расширить логику СОЗДАНИЯ объекта. Когда объект (справа от by) определяет
     * метод provideDelegate, он будет вызван ДЛЯ СОЗДАНИЯ экземпляра делегата */
    operator fun provideDelegate(
        thisRef: Binding,
        prop: KProperty<*>
    ): ReadWriteProperty<Binding, T> {
        // Создаем делегат для переменной типа Т
        val delegate = RenderProp(variable, needInit, onChange)
        // Регистрируем его в экземпляре байндинга, которому
        // принадлежит переменная
        registerDelegate(thisRef, prop.name, delegate)
        return delegate
    }

    override fun getValue(thisRef: Binding, property: KProperty<*>) = variable

    override fun setValue(thisRef: Binding, property: KProperty<*>, value: T) {
        if (value == variable) return
        // Меняем значение переменной
        variable = value
        // Если есть хэндлер, заданный при создании делегата,
        // вызываем его
        onChange?.invoke(variable)
        // Если есть слушатели, добавленные к делегату впоследствии,
        // вызываем их
        if (listeners.isNotEmpty()) listeners.forEach { it.invoke() }
    }

    /** Добавляем хэндлер в список хэндлеров для данного
     * RenderProp делегата. Хэндлер сработает всякий раз,
     * когда изменится переменная, инкапсулированная делегатом */
    fun addListener(listener: () -> Unit) {
        listeners.add(listener)
    }

    /** Добавляем данный делегат в map-коллекцию делегатов.
     * Коллекция находится в экземпляре класса (производного
     * от Binding), владеющего данным делегатом */
    private fun registerDelegate(
        thisRef: Binding,
        name: String,
        delegate: RenderProp<T>
    ) {
        thisRef.renderPropsMap[name] = delegate
    }
}

