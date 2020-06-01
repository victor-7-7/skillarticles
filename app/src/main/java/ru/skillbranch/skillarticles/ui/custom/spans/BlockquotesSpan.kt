package ru.skillbranch.skillarticles.ui.custom.spans

import android.graphics.Canvas
import android.graphics.Paint
import android.text.Layout
import android.text.style.LeadingMarginSpan
import androidx.annotation.ColorInt
import androidx.annotation.Px

class BlockquotesSpan(
    @Px
    private val gapWidth: Float,
    @Px
    private val quoteWidth: Float,
    @ColorInt
    private val lineColor: Int
) : LeadingMarginSpan {

    override fun getLeadingMargin(first: Boolean) = (quoteWidth + gapWidth).toInt()

    override fun drawLeadingMargin(
        canvas: Canvas, paint: Paint, currentMarginLocation: Int,
        paragraphDirection: Int, lineTop: Int, lineBaseline: Int,
        lineBottom: Int, text: CharSequence?, lineStart: Int,
        lineEnd: Int, isFirstLine: Boolean, layout: Layout?
    ) {
        paint.withCustomColor {
            paint.color = lineColor
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = quoteWidth
            canvas.drawLine(
                quoteWidth / 2f,
                lineTop.toFloat(),
                quoteWidth / 2f,
                lineBottom.toFloat(),
                paint
            )
        }
    }

    private inline fun Paint.withCustomColor(block: () -> Unit) {
        val oldColor = color
        val oldStyle = style
        val oldWidth = strokeWidth
        block()
        color = oldColor
        style = oldStyle
        strokeWidth = oldWidth
    }
}