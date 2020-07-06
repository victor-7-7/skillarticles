package ru.skillbranch.skillarticles.extensions

import android.os.Parcelable
import android.util.SparseArray
import android.view.View
import android.view.ViewGroup
import androidx.core.view.*
import androidx.navigation.NavDestination
import com.google.android.material.bottomnavigation.BottomNavigationView
import ru.skillbranch.skillarticles.R

// https://stackoverflow.com/questions/4472429/change-the-right-margin-of-a-view-programmatically
/** This method will be useless if this View is not attached to a parent ViewGroup */
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

/** This method will be useless if this View is not attached to a parent ViewGroup */
fun View.setMargins(margins: Int) {
    setMarginOptionally(margins, margins, margins, margins)
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

fun View.getIdName() = if (id == View.NO_ID) "NO_ID"
else try {
    // Если view-объект создается программно и ему в рантайм присваивается
    // id методом setId(View.generateViewId()), то у view будет id, но
    // не будет имени и бросится ошибка -
    // Resources$NotFoundException: Unable to find resource ID #0x1
    // No package identifier when getting name for resource number 0x00000001
    resources.getResourceEntryName(id)
} catch (e: Exception) {
    "NO_NAME"
}

fun ViewGroup.saveChildViewStates(): SparseArray<Parcelable> {
    val childViewStates = SparseArray<Parcelable>()
    children.forEach { child -> child.saveHierarchyState(childViewStates) }
    return childViewStates
}

fun ViewGroup.restoreChildViewStates(childViewStates: SparseArray<Parcelable>) {
    children.forEach { child -> child.restoreHierarchyState(childViewStates) }
}

fun BottomNavigationView.selectDestination(destination: NavDestination) {
    val item = menu.findItem(destination.id)
    if (item != null)
        item.isChecked = true
    else {
        menu.findItem(R.id.nav_profile).isChecked = true
    }
}


