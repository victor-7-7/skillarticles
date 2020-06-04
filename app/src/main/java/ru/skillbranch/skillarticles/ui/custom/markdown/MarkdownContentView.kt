package ru.skillbranch.skillarticles.ui.custom.markdown

import android.content.Context
import android.os.Parcel
import android.os.Parcelable
import android.util.AttributeSet
import android.util.SparseArray
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.view.children
import androidx.core.view.forEachIndexed
import androidx.core.view.size
import kotlinx.android.synthetic.main.activity_root.view.*
import ru.skillbranch.skillarticles.data.repositories.MarkdownElement
import ru.skillbranch.skillarticles.extensions.*
import kotlin.properties.Delegates

class MarkdownContentView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ViewGroup(context, attrs, defStyleAttr) {
    private lateinit var elements: List<MarkdownElement>

    //for restore
    private var ids = arrayListOf<Int>()

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
        elements.forEachIndexed { idx, elem ->
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
                }
                is MarkdownElement.Scroll -> {
                    val sv = MarkdownCodeView(
                        context,
                        textSize,
                        elem.blockCode.text
                    )
                    addView(sv)
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

    // save image and scroll ids and their states
    override fun onSaveInstanceState(): Parcelable? {
        return SavedState(super.onSaveInstanceState()).apply {
            ids = IntArray(tv_text_content.size).apply {
                fill(-1, 0, size)
            }.toCollection(ArrayList())
            tv_text_content.forEachIndexed { idx, view ->
                if (view.id < 0) view.id = View.generateViewId()
                ids[idx] = view.id
            }
            ssIds = ids
            childrenStates = saveChildViewStates()
        }
    }

    // restore image and scroll ids and their states
    override fun onRestoreInstanceState(state: Parcelable?) {
        when (state) {
            is SavedState -> {
                super.onRestoreInstanceState(state.superState)
                ids = state.ssIds
                tv_text_content.forEachIndexed { idx, view ->
                    if (view.id < 0) view.id = ids[idx]
                }
                state.childrenStates?.let {
                    restoreChildViewStates(it)
                }
            }
            else -> super.onRestoreInstanceState(state)
        }

    }

    private class SavedState : BaseSavedState, Parcelable {
        var ssIds = arrayListOf<Int>()
        var childrenStates: SparseArray<Parcelable>? = null

        // Called by derived class when creating its SavedState object
        constructor(superState: Parcelable?) : super(superState)

        // Used when reading from a parcel. Reads the state of the superclass
        constructor(src: Parcel) : super(src) {
            val idsArray = intArrayOf()
            src.readIntArray(idsArray)
            ssIds = idsArray.toCollection(ArrayList())
            childrenStates = src.readSparseArray(javaClass.classLoader)
        }

        override fun writeToParcel(dst: Parcel, flags: Int) {
            super.writeToParcel(dst, flags)
            dst.writeIntArray(ssIds.toIntArray())
//            dst.writeSparseArray(childrenStates as SparseArray<Any>)
            dst.writeSparseArray(childrenStates)
        }

        companion object CREATOR : Parcelable.Creator<SavedState> {
            override fun createFromParcel(source: Parcel) = SavedState(source)
            override fun newArray(size: Int): Array<SavedState?> = arrayOfNulls(size)
        }
    }
}