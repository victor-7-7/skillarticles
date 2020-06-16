package ru.skillbranch.skillarticles.data.models

import java.util.*

data class CommentItemData(
    val id: String,
    val articleId: String,
    val user: User,
    val body: String,
    val date: Date,
    /** Текстовый идентификатор, играющий роль первичного ключа, по которому
     * данный коммент отыскивается в CommentsDataSource */
    val slug: String,
    val answerTo: String? = null
) {

}