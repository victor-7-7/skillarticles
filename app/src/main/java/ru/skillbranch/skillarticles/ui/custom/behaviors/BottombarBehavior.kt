package ru.skillbranch.skillarticles.ui.custom.behaviors

import android.content.Context
import android.util.AttributeSet
import android.view.View
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.widget.NestedScrollView
import com.google.android.material.behavior.HideBottomViewOnScrollBehavior
import ru.skillbranch.skillarticles.ui.custom.Bottombar

class BottombarBehavior @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : HideBottomViewOnScrollBehavior<Bottombar>() {

    override fun layoutDependsOn(
        parent: CoordinatorLayout,
        child: Bottombar,
        dependency: View
    ): Boolean {
        /** мы хотим реагировать только на изменения разметки
         * виджета NestedScrollView */
        return dependency is NestedScrollView
    }

    /** Метод вызывается всякий раз, когда с виджетом, который находится
     * в CoordinatorLayout и изменения которого мы отслеживаем (NestedScrollView),
     * что-то происходит */
    override fun onDependentViewChanged(
        parent: CoordinatorLayout,
        child: Bottombar,
        dependency: View
    ): Boolean {
//        val translationY = 0f.coerceAtMost(
//            dependency.translationY - dependency.height)
//        child.translationY = translationY
//        return true
        return super.onDependentViewChanged(parent, child, dependency)
    }
}