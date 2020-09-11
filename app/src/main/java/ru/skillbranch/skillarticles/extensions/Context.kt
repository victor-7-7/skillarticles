package ru.skillbranch.skillarticles.extensions

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.util.DisplayMetrics
import android.util.TypedValue
import android.view.View
import android.view.inputmethod.InputMethodManager
import androidx.annotation.AttrRes

fun Context.dpToPx(dp: Int) = TypedValue.applyDimension(
    TypedValue.COMPLEX_UNIT_DIP, dp.toFloat(), resources.displayMetrics
)

fun Context.dpToIntPx(dp: Int) = dpToPx(dp).toInt()

fun Context.attrValue(@AttrRes res: Int): Int {
    val value: Int
    val tv = TypedValue()
    if (theme.resolveAttribute(res, tv, true)) value = tv.data
    else throw IllegalArgumentException("Resource with id $res not found")
    return value

}

fun Context.convertDpToPx(dp: Float): Float = TypedValue.applyDimension(
    TypedValue.COMPLEX_UNIT_DIP, dp, resources.displayMetrics
)

fun Context.convertPxToDp(px: Float): Float =
    px / (resources.displayMetrics.xdpi / DisplayMetrics.DENSITY_DEFAULT
            )

fun Context.convertSpToPx(sp: Float): Float = TypedValue.applyDimension(
    TypedValue.COMPLEX_UNIT_SP, sp, resources.displayMetrics
)

fun Context.hideKeyboard(view: View) {
    val imm: InputMethodManager = getSystemService(
        Context.INPUT_METHOD_SERVICE
    ) as InputMethodManager
    imm.hideSoftInputFromWindow(view.windowToken, 0)
}

fun Context.showKeyboard(view: View) {
    val imm: InputMethodManager = getSystemService(
        Context.INPUT_METHOD_SERVICE
    ) as InputMethodManager
    imm.showSoftInput(view, InputMethodManager.SHOW_IMPLICIT)
}

val Context.isNetworkAvailable: Boolean
    get() {
        val cm = this.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            cm.activeNetwork?.run {
                val nc = cm.getNetworkCapabilities(this)
                nc!!.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)
                        || nc.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
            } ?: false
        } else {
            cm.activeNetworkInfo?.run { isConnectedOrConnecting } ?: false
        }
    }


const val chSamsara = "\u058D"
const val chShekel = "\u20AA"
const val chIdentical = "\u2261"
const val chSquare = "\u2588"
const val chCircle = "\u25CF"
const val chAin = "\u1D25"
const val chBrick = "\u25AC"

fun getMeasureSpecMode(spec: Int) =
    when (View.MeasureSpec.getMode(spec)) {
        View.MeasureSpec.UNSPECIFIED -> "UNSPECIFIED"
        View.MeasureSpec.AT_MOST -> "AT_MOST"
        View.MeasureSpec.EXACTLY -> "EXACTLY"
        else -> throw IllegalArgumentException("Invalid measure spec value")
    }
