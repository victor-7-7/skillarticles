package ru.skillbranch.skillarticles.viewmodels.base

import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProviders
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

class ViewModelDelegate<T : ViewModel>(
    private val clazz: Class<T>,
    private val arg: Any?
) : ReadOnlyProperty<FragmentActivity, T> {
    private var value: T? = null

    override fun getValue(thisRef: FragmentActivity, property: KProperty<*>): T {
        if (value == null) {
            val vmFactory = arg?.let { ViewModelFactory(it) }
            value = ViewModelProviders.of(thisRef, vmFactory).get(clazz)
        }
        return value!!
    }
}