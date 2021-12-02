package ru.skillbranch.skillarticles.ui.custom

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.view.View
import androidx.annotation.IdRes
import androidx.annotation.Px
import androidx.core.widget.NestedScrollView
import ru.skillbranch.skillarticles.R
import ru.skillbranch.skillarticles.extensions.screenHeight

/** Вьюха, в которой скроллируется контент статьи, а когда комментарии
 * займут весь экран приложения, то поле ввода прилипнет к верхней границе
 * экрана и во вложенной вьюхе RecycleView начнут скроллиться комментарии */
class StickyScrollView @JvmOverloads constructor(
    ctx: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : NestedScrollView(ctx, attrs, defStyleAttr) {
    /** Ресурсный идентификатор поля ввода комментов (wrap_comments) */
    @IdRes private val targetId: Int
    // Расстояние до пороговых линий (снизу и сверху), пересечение которых
    // полем ввод stickyView триггерит плавную анимацию скролла
    @Px private val threshold: Int
    private val screenH = screenHeight()
    private var stickyState = StickyState.IDLE
    /** Липкое поле ввода -> wrap_comments */
    private lateinit var stickyView: View

    init {
        ctx.theme.obtainStyledAttributes(
            attrs, R.styleable.StickyScrollView, 0, 0
        ).apply {
            try {
                targetId = getResourceId(R.styleable.StickyScrollView_stickyView, -1)
                threshold = getDimensionPixelSize(R.styleable.StickyScrollView_threshold, 0)
            } finally {
                recycle()
            }
        }
    }

    override fun onFinishInflate() {
        super.onFinishInflate()
        stickyView = findViewById(targetId)
    }

    // This is called in response to an internal scroll in this view
    // (i.e., the view scrolled its own contents)
    // Параметр t -> y-координата точек данной вьюхи (относительно самой вьюхи),
    // попавших в данный момент на верхнюю границу контентного окна приложения.
    // oldt - y-координата тех точек, которые пересекали границу окна в предыдущий момент
    override fun onScrollChanged(l: Int, t: Int, oldl: Int, oldt: Int) {
        super.onScrollChanged(l, t, oldl, oldt)
        // Когда t > oldt -> вьюшный контент перемещается вверх (скролл идет вниз)
        val isScrollDown = t > oldt
        // Неподвижная линия, которая всегда ниже верхней границы контентного окна
        // на величину threshold, в данный момент имеет y-координату topEdge
        val topEdge = t + threshold
        // Неподвижная линия, которая всегда выше нижней границы контентного окна
        // на величину threshold, в данный момент имеет y-координату bottomEdge
        val bottomEdge = t + screenH - threshold

        /*Log.d("M_S_Paging", "StickyScrollView onScrollChanged [t: $t] " +
                "[oldt: $oldt] [top: $top] [isScrollDown: $isScrollDown] " +
                "[topEdge: $topEdge] [bottomEdge: $bottomEdge]")*/

        // stickyView.top -> фиксированное число - y-координата stickyView
        // относительно данной (родительской) вьюхи
        when {
            // Когда поле ввода (stickyView) находится ниже нижней линии
            bottomEdge < stickyView.top -> stickyState = StickyState.IDLE
            // Когда вьюшный контент перемещается вверх и топ поля ввода пересек снизу нижнюю линию
            isScrollDown && bottomEdge > stickyView.top -> stickyState = StickyState.TOP
            // Когда вьюшный контент перемещается вниз и боттом поля ввода пересек сверху верхнюю линию
            ! isScrollDown && topEdge < stickyView.bottom ->
                if (stickyState == StickyState.TOP) stickyState = StickyState.BOTTOM
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent?): Boolean {
        // Если мы оторвали палец от дисплея
        if (event?.action == MotionEvent.ACTION_UP) {
            // Делаем (при необходимости) анимацию
            post { animateState() }
        }
        return super.onTouchEvent(event)
    }

    private fun animateState() {
        val y = when(stickyState) {
            // Надо проскроллить контент вьюхи так, чтобы топ виджета stickyView
            // (wrap_comments) оказался прижат к верхней границы окна приложения
            StickyState.TOP -> stickyView.top
            // Надо проскроллить контент вьюхи так, чтобы топ виджета stickyView
            // (wrap_comments) оказался прижат к нижней границы окна приложения, т.е. был скрыт
            StickyState.BOTTOM -> stickyView.top - screenH
            // IDLE
            else -> return
        }
        // Плавно скроллим контент вьюхи по вертикали
        smoothScrollTo(0, y)
    }

    private enum class StickyState {
        TOP, BOTTOM, IDLE
    }
}