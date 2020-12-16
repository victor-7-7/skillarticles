package ru.skillbranch.skillarticles.extensions

import android.text.Spannable
import androidx.core.util.PatternsCompat.EMAIL_ADDRESS
import java.util.regex.Pattern

const val MAX_CHARS = 16

fun String.truncate(maxChars: Int = MAX_CHARS): String {
    val inStr = this.trim()
    if (maxChars < 1 || inStr.length <= maxChars) return inStr
    val outStr = inStr.dropLast(inStr.length - maxChars)
    return "${outStr.trimEnd()}..."
}

fun String?.indexesOf(query: String, ignoreCase: Boolean = true): List<Int> {
    if (this.isNullOrEmpty() || query.isEmpty()) return emptyList()
    val list = mutableListOf<Int>()
    var start = 0
    var wip = true
    var index: Int?
    while (wip) {
        index = indexOf(query, start, ignoreCase)
        if (index == -1) wip = false
        else {
            list.add(index)
            start = index + query.length
        }
    }
    return list
}

fun String.stripHtml(): String = this.removeHtmlTags()
    .removeHtmlSpecialChars()
    .removeExtraSpaces()

fun String.removeHtmlTags(): String =
    this.replace(Regex("<[^<]*?>"), "")

fun String.removeHtmlSpecialChars(): String =
    this.replace(Regex("&#?\\w{2,6};"), "")

fun String.removeExtraSpaces(): String =
    this.trim().replace(Regex(" {2,}"), " ")

inline fun <reified T> Spannable.getSpans(): Array<T> =
    getSpans(0, lastIndex, T::class.java)

fun String.isValidEmail(): Boolean =
    this.isNotEmpty() && EMAIL_ADDRESS.matcher(this).matches()

fun String.isValidPassword(): Boolean {
    // пароль не короче 8 символов, без спецзнаков - только буквы и цифры
    val passwordPattern = Pattern.compile("[a-zA-Z0-9]{8,24}")
    return passwordPattern.matcher(this).matches()
}

