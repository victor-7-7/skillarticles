package ru.skillbranch.skillarticles.ui.article

import ru.skillbranch.skillarticles.data.remote.res.CommentRes

interface IArticleView {
    /** Показать search bar */
    fun showSearchBar()

    /** Скрыть search bar */
    fun hideSearchBar()

    fun clickOnComment(comment: CommentRes)
}