package ru.skillbranch.skillarticles.extensions

import android.text.Spannable

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
        index = findAnyOf(listOf(query), start, ignoreCase)?.first
        if (index == null) wip = false
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
