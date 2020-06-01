package ru.skillbranch.skillarticles.extensions

import android.text.Layout

/** Height of the line */
fun Layout.getLineHeight(line: Int): Int {
    return getLineTop(line.inc()) - getLineTop(line)
}

/** Line top without extra padding applied by layout */
fun Layout.getLineTopWithoutPadding(line: Int): Int {
    var lineTop = getLineTop(line)
    if (line == 0) lineTop -= topPadding
    return lineTop
}

/** Line bottom without extra padding applied by layout */
fun Layout.getLineBottomWithoutPadding(line: Int): Int {
    var lineBottom = getLineBottomWithoutSpacing(line)
    if (line == lineCount.dec()) lineBottom -= bottomPadding
    return lineBottom
}

/** Line bottom without spacing */
private fun Layout.getLineBottomWithoutSpacing(line: Int): Int {
    val lineBottom = getLineBottom(line)
    val isLastLine = line == lineCount.dec()
    val hasLineSpacing = spacingAdd != 0f
    return if (!hasLineSpacing || isLastLine) {
        lineBottom + spacingAdd.toInt()
    } else {
        lineBottom - spacingAdd.toInt()
    }
}



