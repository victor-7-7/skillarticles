package ru.skillbranch.skillarticles.ui.custom.markdown

import android.text.Spannable
import android.text.SpannableString
import androidx.core.text.getSpans
import ru.skillbranch.skillarticles.ui.custom.spans.SearchFocusSpan
import ru.skillbranch.skillarticles.ui.custom.spans.SearchSpan

interface IMarkdownView {
    var fontSize: Float
    val spannableContent: Spannable

    fun renderSearchResult(
        result: List<Pair<Int, Int>>,
        offset: Int
    ) {
        clearSearchResult()
        val offsetResult = result.map { (start, end) ->
            start.minus(offset) to end.minus(offset)
        }
        offsetResult.forEach { (start, end) ->
            spannableContent.setSpan(
                SearchSpan(), start, end,
                SpannableString.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }
    }

    fun renderSearchPosition(
        position: Pair<Int, Int>,
        offset: Int
    ) {
        spannableContent.getSpans<SearchFocusSpan>().forEach {
            spannableContent.removeSpan(it)
        }
        spannableContent.setSpan(
            SearchFocusSpan(),
            position.first.minus(offset),
            position.second.minus(offset),
            SpannableString.SPAN_EXCLUSIVE_EXCLUSIVE
        )
    }

    fun clearSearchResult() {
        spannableContent.getSpans<SearchSpan>().forEach {
            spannableContent.removeSpan(it)
        }
    }
}