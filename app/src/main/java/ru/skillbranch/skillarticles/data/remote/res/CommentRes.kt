package ru.skillbranch.skillarticles.data.remote.res

import com.squareup.moshi.Json
import ru.skillbranch.skillarticles.data.models.User
import java.util.*

data class CommentRes(
    val id: String,
    @Json(name = "author")
    val user: User,
    @Json(name = "message")
    val body: String,
    val date: Date,
    /** Текстовый ключ, указывающий место данного комментария в дереве комментов
     * к статье. Если данный коммент 0 уровня (комментируется статья), то ключ
     * имеет вид: "xxxx/", где xxxx это последние 4 символа из идентификатора id
     * коммента. Если данный коммент сделан в ответ на коммент со slug-ключом
     * "zzzz/yyyy/", то ключ будет вида - "zzzz/yyyy/xxxx/" */
    val slug: String,
    /** Имя юзера, в ответ на коммент которого написан данный коммент. Если
     * null, то значит данный коммент 0 уровня */
    val answerTo: String? = null
)