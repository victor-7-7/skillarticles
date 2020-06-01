package ru.skillbranch.skillarticles.extensions

import android.view.View
import android.view.ViewGroup
import androidx.core.view.marginBottom
import androidx.core.view.marginLeft
import androidx.core.view.marginRight
import androidx.core.view.marginTop

// https://stackoverflow.com/questions/4472429/change-the-right-margin-of-a-view-programmatically
fun View.setMarginOptionally(
    left: Int? = marginLeft, top: Int? = marginTop,
    right: Int? = marginRight, bottom: Int? = marginBottom
) {
    if (layoutParams is ViewGroup.MarginLayoutParams) {
        val params = layoutParams as ViewGroup.MarginLayoutParams
        left?.let { params.leftMargin = left }
        top?.let { params.topMargin = top }
        right?.let { params.rightMargin = right }
        bottom?.let { params.bottomMargin = bottom }
        layoutParams = params
        requestLayout()
    }
}

fun View.setPaddingOptionally(
    left: Int? = paddingLeft, top: Int? = paddingTop,
    right: Int? = paddingRight, bottom: Int? = paddingBottom
) {
    if (left == null && top == null && right == null && bottom == null) return
    val l = left ?: paddingLeft
    val t = top ?: paddingTop
    val r = right ?: paddingRight
    val b = bottom ?: paddingBottom
    setPadding(l, t, r, b)
    requestLayout()
}