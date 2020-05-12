package ru.skillbranch.skillarticles.extensions

import android.view.View
import android.view.ViewGroup

// https://stackoverflow.com/questions/4472429/change-the-right-margin-of-a-view-programmatically
fun View.setMarginOptionally(
    leftPx: Int? = null, topPx: Int? = null,
    rightPx: Int? = null, bottomPx: Int? = null
) {
    if (layoutParams is ViewGroup.MarginLayoutParams) {
        val params = layoutParams as ViewGroup.MarginLayoutParams
        leftPx?.let { params.leftMargin = leftPx }
        topPx?.let { params.topMargin = topPx }
        rightPx?.let { params.rightMargin = rightPx }
        bottomPx?.let { params.bottomMargin = bottomPx }
        layoutParams = params
        requestLayout()
    }
}