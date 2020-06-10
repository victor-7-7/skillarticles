package ru.skillbranch.skillarticles.ui.custom.markdown

import android.content.Context
import android.os.Parcel
import android.os.Parcelable
import android.util.AttributeSet
import android.util.SparseArray
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.util.isEmpty
import androidx.core.view.children
import ru.skillbranch.skillarticles.data.repositories.MarkdownElement
import ru.skillbranch.skillarticles.extensions.dpToIntPx
import ru.skillbranch.skillarticles.extensions.groupByBounds
import ru.skillbranch.skillarticles.extensions.setPaddingOptionally
import kotlin.properties.Delegates

class MarkdownContentView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ViewGroup(context, attrs, defStyleAttr) {
    private lateinit var elements: List<MarkdownElement>
    private var layoutManager = LayoutManager()

    var textSize by Delegates.observable(14f) { _, old, value ->
        if (value == old) return@observable
        this.children.forEach {
            it as IMarkdownView
            it.fontSize = value
        }
    }
    var isLoading: Boolean = true
    val padding = context.dpToIntPx(8)

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        var usedHeight = paddingTop
        val width = View.getDefaultSize(suggestedMinimumWidth, widthMeasureSpec)
        children.forEach {
            measureChild(it, widthMeasureSpec, heightMeasureSpec)
            usedHeight += it.measuredHeight
        }
        usedHeight += paddingBottom
        setMeasuredDimension(width, usedHeight)
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        var usedHeight = paddingTop
        val bodyWidth = right - left - paddingLeft - paddingRight
        val left = paddingLeft
        val right = paddingLeft + bodyWidth

        children.forEach {
            if (it is MarkdownTextView) {
                it.layout(
                    left - paddingLeft / 2,
                    usedHeight,
                    r - paddingRight / 2,
                    usedHeight + it.measuredHeight
                )
            } else {
                it.layout(
                    left,
                    usedHeight,
                    right,
                    usedHeight + it.measuredHeight
                )
            }
            usedHeight += it.measuredHeight
        }
    }

    fun setContent(content: List<MarkdownElement>) {
        elements = content
        var index = 0
        content.forEach { elem ->
            when (elem) {
                is MarkdownElement.Text -> {
                    val tv = MarkdownTextView(context, textSize).apply {
                        // Чтобы поисковое выделение не обрезалось по бокам
                        setPaddingOptionally(left = padding, right = padding)
                        setLineSpacing(fontSize * .5f, 1f)
                    }
                    MarkdownBuilder(context).markdownToSpan(elem).run {
                        tv.setText(this, TextView.BufferType.SPANNABLE)
                    }
                    addView(tv)
                }
                is MarkdownElement.Image -> {
                    val iv = MarkdownImageView(
                        context,
                        textSize,
                        elem.image.url,
                        elem.image.text,
                        elem.image.alt
                    )
                    addView(iv)
                    layoutManager.attachToParent(iv, index)
                    index++
                }
                is MarkdownElement.Scroll -> {
                    val sv = MarkdownCodeView(
                        context,
                        textSize,
                        elem.blockCode.text
                    )
                    addView(sv)
                    layoutManager.attachToParent(sv, index)
                    index++
                }
            }
        }
    }

    fun renderSearchResult(searchResult: List<Pair<Int, Int>>) {
        children.forEach { view ->
            view as IMarkdownView
            view.clearSearchResult()
        }
        if (searchResult.isEmpty()) return
        // Список границ элементов MarkdownElement
        val bounds = elements.map { it.bounds }
        // Список списков пар результатов поиска, разбитый
        // по границам элементов MarkdownElement
        val result = searchResult.groupByBounds(bounds)

        children.forEachIndexed { index, view ->
            view as IMarkdownView
            // search for child with markdown element offset
            view.renderSearchResult(result[index], elements[index].offset)
        }
    }

    fun renderSearchPosition(
        searchPosition: Pair<Int, Int>?,
        force: Boolean = false
    ) {
        searchPosition ?: return
        val bounds = elements.map { it.bounds }
        val index = bounds.indexOfFirst { (start, end) ->
            val boundRange = start..end
            val (startPos, endPos) = searchPosition
            startPos in boundRange && endPos in boundRange
        }
        if (index == -1) return
        val view = getChildAt(index)
        view as IMarkdownView
        view.renderSearchPosition(searchPosition, elements[index].offset)
    }

    fun clearSearchResult() {
        children.forEach { view ->
            view as IMarkdownView
            view.clearSearchResult()
        }
    }

    fun setCopyListener(listener: (String) -> Unit) {
        children.filterIsInstance<MarkdownCodeView>()
            .forEach {
                it.copyListener = listener
            }
    }

    override fun dispatchSaveInstanceState(container: SparseArray<Parcelable>?) {
        // save children manually without markdown text views
        children.filter { it !is MarkdownTextView }
            .forEach { it.saveHierarchyState(layoutManager.container) }
        // save only markdownContentView
        dispatchFreezeSelfOnly(container)
    }

    override fun onSaveInstanceState(): Parcelable? {
        val state = SavedState(super.onSaveInstanceState())
        state.ssLayoutManager = layoutManager
        return state
    }

    override fun onRestoreInstanceState(state: Parcelable?) {
        super.onRestoreInstanceState(state)
        if (state is SavedState) layoutManager = state.ssLayoutManager
    }

    //=============================================================================

    private class LayoutManager() : Parcelable {
        var ids = mutableListOf<Int>()
        var container: SparseArray<Parcelable> = SparseArray()

        constructor(parcel: Parcel) : this() {
            ids = parcel.readArrayList(Int::class.java.classLoader) as ArrayList<Int>
            container = parcel.readSparseArray<Parcelable>(this::class.java.classLoader)
                    as SparseArray<Parcelable>
        }

        override fun writeToParcel(parcel: Parcel, flags: Int) {
            parcel.writeIntArray(ids.toIntArray())
            parcel.writeSparseArray(container)
        }

        fun attachToParent(view: View, index: Int) {
            if (container.isEmpty()) {
                view.id = View.generateViewId()
                ids.add(view.id)
            } else {
                view.id = ids[index]
                view.restoreHierarchyState(container)
            }
        }

        override fun describeContents() = 0

        companion object CREATOR : Parcelable.Creator<LayoutManager> {
            override fun createFromParcel(parcel: Parcel) = LayoutManager(parcel)
            override fun newArray(size: Int): Array<LayoutManager?> = arrayOfNulls(size)
        }
    }

    private class SavedState : BaseSavedState, Parcelable {
        lateinit var ssLayoutManager: LayoutManager

        // Called by derived class when creating its SavedState object
        constructor(superState: Parcelable?) : super(superState)

        // Used when reading from a parcel. Reads the state of the superclass
        constructor(src: Parcel) : super(src) {
            ssLayoutManager = src.readParcelable(LayoutManager::class.java.classLoader)!!
        }

        override fun writeToParcel(dst: Parcel, flags: Int) {
            super.writeToParcel(dst, flags)
            dst.writeParcelable(ssLayoutManager, flags)
        }

        companion object CREATOR : Parcelable.Creator<SavedState> {
            override fun createFromParcel(source: Parcel) = SavedState(source)
            override fun newArray(size: Int): Array<SavedState?> = arrayOfNulls(size)
        }
    }
}