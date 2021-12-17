package ru.skillbranch.skillarticles.ui.base

import android.os.Bundle
import ru.skillbranch.skillarticles.ui.delegates.RenderProp
import ru.skillbranch.skillarticles.viewmodels.base.IViewModelState
import kotlin.reflect.KProperty

/** Класс-хэлпер, помогающий связать UI виджеты фрагмента со свойствами
 * стейта вьюмодели, относящейся к данному фрагменту. Каждый раз при
 * изменении свойств стейта вьюмодели будет вызван метод bind и изменены
 * соответствующие свойства производного (от Binding) класса. В результате
 * изменения соответствующего свойства сработает его хэндлер (если свойство
 * делегирует этот функционал объекту RenderProp). А уже хэндлер изменит
 * состояние соответствующего UI виджета фрагмента */
abstract class Binding {

    /** Коллекция всех делегатов RenderProp, созданных в
     * производном от Binding классе */
    val renderPropsMap = mutableMapOf<String, RenderProp<out Any>>()

    private var isFragmentInflated = false

    /** Эту лямбду (в производном от Binding классе) невозможно
     * вызвать (из метода onFinishFragmentInflate) более одного раза */
    open val afterFragmentInflatedHandler: (() -> Unit)? = null

    /** Метод вызывает лямбду afterFragmentInflatedHandler единожды.
     * При вызове данного метода повторно, лямбда не будет вызвана */
    open fun onFinishFragmentInflate() {
        if (!isFragmentInflated) {
            afterFragmentInflatedHandler?.invoke()
            isFragmentInflated = true
        }
    }

    /** Метод вызывается (как хэндлер) при каждом изменении стейта
     * вью модели фрагмента. Подписка о наблюдении за стейтом выполнена
     * при создании фрагмента (в базовом классе BaseFragment) */
    abstract fun bind(data: IViewModelState)

    /**
     * override this if need save binding in bundle
     */
    open fun saveUi(outState: Bundle) {
        //empty default implementation
    }

    /**
     * override this if need restore binding from bundle
     */
    open fun restoreUi(savedState: Bundle?) {
        //empty default implementation
    }

    /** У восстановленного фрагмента свойство binding не null,
     * поэтому вызывается данный метод, который реинициализирует
     * каждый RenderProp делегат, принадлежащий объекту binding */
    fun rebind() {
        renderPropsMap.forEach { map -> map.value.initialBind() }
    }

    @Suppress("UNCHECKED_CAST")
    /** Метод добавляет единый хэндлер onChange сразу для четырех RenderProp
     * делегатов (инкапсулирующих переменные типов A, B, C, D). Этот хэндлер
     * будет добавлен в списки хэндлеров КАЖДОГО из делегатов. При изменении
     * ЛЮБОЙ (инкапсулированной в делегате) переменной (из четырех) сработает
     * хэндлер onChange с передачей ему в качестве аргументов всех четырех
     * переменных A, B, C, D */
    fun <A, B, C, D> dependsOn(
        vararg fields: KProperty<*>,
        onChange: (A, B, C, D) -> Unit
    ) {
        check(fields.size == 4) { "Names size must be 4, current ${fields.size}" }
        val names = fields.map { it.name }
        names.forEach {
            renderPropsMap[it]?.addListener {
                onChange(
                    renderPropsMap[names[0]]?.variable as A,
                    renderPropsMap[names[1]]?.variable as B,
                    renderPropsMap[names[2]]?.variable as C,
                    renderPropsMap[names[3]]?.variable as D
                )
            }
        }
    }

    @Suppress("UNCHECKED_CAST")
            /** Метод добавляет единый хэндлер onChange сразу для двух RenderProp
             * делегатов (инкапсулирующих переменные типов A, B). Этот хэндлер
             * будет добавлен в списки хэндлеров КАЖДОГО из делегатов. При изменении
             * ЛЮБОЙ (инкапсулированной в делегате) переменной (из двух) сработает
             * хэндлер onChange с передачей ему в качестве аргументов обоих
             * переменных A, B */
    fun <A, B> dependsOn(
        vararg fields: KProperty<*>,
        onChange: (A, B) -> Unit
    ) {
        check(fields.size == 2) { "Names size must be 2, current ${fields.size}" }
        val names = fields.map { it.name }
        names.forEach {
            renderPropsMap[it]?.addListener {
                onChange(
                    renderPropsMap[names[0]]?.variable as A,
                    renderPropsMap[names[1]]?.variable as B
                )
            }
        }
    }
}