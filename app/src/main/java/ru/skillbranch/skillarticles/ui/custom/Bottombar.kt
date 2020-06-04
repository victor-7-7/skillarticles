package ru.skillbranch.skillarticles.ui.custom

import android.content.Context
import android.os.Parcel
import android.os.Parcelable
import android.util.AttributeSet
import android.view.View
import android.view.ViewAnimationUtils
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.animation.doOnEnd
import androidx.core.view.isVisible
import com.google.android.material.shape.MaterialShapeDrawable
import kotlinx.android.synthetic.main.layout_bottombar.view.*
import ru.skillbranch.skillarticles.R
import ru.skillbranch.skillarticles.ui.custom.behaviors.BottombarBehavior
import kotlin.math.hypot

class Bottombar @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr), CoordinatorLayout.AttachedBehavior {

    var isSearchMode = false

    init {
        View.inflate(context, R.layout.layout_bottombar, this)
        val materialBg = MaterialShapeDrawable.createWithElevationOverlay(context)
        materialBg.elevation = elevation
        background = materialBg
    }

    override fun getBehavior(): CoordinatorLayout.Behavior<*> = BottombarBehavior()

    fun setSearchState(search: Boolean) {
        if (isSearchMode == search || !isAttachedToWindow) return
        isSearchMode = search
        if (isSearchMode) animateShowSearchPanel()
        else animateHideSearchPanel()
    }

    private fun animateShowSearchPanel() {
        reveal.isVisible = true
        val endRadius = hypot(width.toFloat(), height / 2f)
        val va = ViewAnimationUtils.createCircularReveal(
            reveal,
            width,
            height / 2,
            0f,
            endRadius
        )
        va.doOnEnd { group_bottom.isVisible = false }
        va.start()
    }

    private fun animateHideSearchPanel() {
        group_bottom.isVisible = true
        val startRadius = hypot(width.toFloat(), height / 2f)
        val va = ViewAnimationUtils.createCircularReveal(
            reveal,
            width,
            height / 2,
            startRadius,
            0f
        )
        va.doOnEnd { reveal.isVisible = false }
        va.start()
    }

    fun bindSearchInfo(searchCount: Int = 0, position: Int = 0) {
        if (searchCount == 0) {
            tv_search_result.text = "Not found"
            btn_result_up.isEnabled = false
            btn_result_down.isEnabled = false
        } else {
            val info = "${position.inc()} of $searchCount"
            tv_search_result.text = info
            btn_result_up.isEnabled = true
            btn_result_down.isEnabled = true
        }
        // lock btn in min/max positions
        when (position) {
            0 -> btn_result_up.isEnabled = false
            searchCount - 1 -> btn_result_down.isEnabled = false
        }
    }

    // save bottom state
    override fun onSaveInstanceState(): Parcelable? {
        val savedState = SavedState(super.onSaveInstanceState())
        savedState.ssIsSearchMode = isSearchMode
        return savedState
    }

    // restore bottom state
    override fun onRestoreInstanceState(state: Parcelable?) {
        super.onRestoreInstanceState(state)
        if (state is SavedState) {
            isSearchMode = state.ssIsSearchMode
            reveal.isVisible = isSearchMode
            group_bottom.isVisible = !isSearchMode
        }
    }

    private class SavedState : BaseSavedState, Parcelable {
        var ssIsSearchMode: Boolean = false

        constructor(superState: Parcelable?) : super(superState)

        constructor(src: Parcel) : super(src) {
            ssIsSearchMode = src.readInt() == 1
        }

        override fun writeToParcel(out: Parcel, flags: Int) {
            super.writeToParcel(out, flags)
            out.writeInt(if (ssIsSearchMode) 1 else 0)
        }

        override fun describeContents() = 0

        companion object CREATOR : Parcelable.Creator<SavedState> {
            override fun createFromParcel(source: Parcel) = SavedState(source)
            override fun newArray(size: Int): Array<SavedState?> = arrayOfNulls(size)
        }
    }
}