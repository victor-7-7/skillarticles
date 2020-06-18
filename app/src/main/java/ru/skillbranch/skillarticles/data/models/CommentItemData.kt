package ru.skillbranch.skillarticles.data.models

import java.util.*

data class CommentItemData(
    val id: String,
    val articleId: String,
    val user: User,
    val body: String,
    val date: Date,
    /** Текстовый ключ, указывающий место данного комментария в дереве
     * комментариев к статье. Если данный коммент 0 уровня (комментируется
     * статья), то ключ имеет вид - "id/", где id это идентификатор коммента.
     * Если данный коммент сделан в ответ на коммент со slug-ключом "5/12/",
     * то ключ будет вида - "5/12/id/" */
    val slug: String,
    /** Имя юзера, в ответ на коммент которого написан данный коммент. Если
     * null, то значит данный коммент 0 уровня */
    val answerTo: String? = null
) {

}