package ru.skillbranch.skillarticles.data.local.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import java.util.*

@Entity(
    tableName = "article_content",
    foreignKeys = [ForeignKey(
        entity = Article::class,
        // Идентификатор статьи в таблице articles (entity:Article)
        parentColumns = ["id"],
        // Идентификатор статьи в этой таблице (для связи)
        childColumns = ["article_id"],
        // Если запись с идентификаторм id = N будет удалена из родительской
        // таблицы articles, то будет также удалена и запись с идентификатором
        // article_id = N и в этой таблице (CASCADE)
        onDelete = ForeignKey.CASCADE
    )]
)
data class ArticleContent(
    @PrimaryKey
    @ColumnInfo(name = "article_id")
    val articleId: String,
    val content: String,
    val source: String? = null,
    @ColumnInfo(name = "share_link")
    val shareLink: String,
    @ColumnInfo(name = "updated_at")
    val updatedAt: Date = Date()
)