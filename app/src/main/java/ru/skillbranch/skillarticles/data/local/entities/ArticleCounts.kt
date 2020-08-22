package ru.skillbranch.skillarticles.data.local.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import java.util.*

@Entity(
    tableName = "article_counts",
    foreignKeys = [ForeignKey(
        entity = Article::class,
        parentColumns = ["id"],
        childColumns = ["article_id"],
        onDelete = ForeignKey.CASCADE
    )]
)
/** источник правды для непостоянных полей likes/comments - сервер */
data class ArticleCounts(
    @PrimaryKey
    @ColumnInfo(name = "article_id")
    val articleId: String,
    // Всеобщее число лайков у статьи
    val likes: Int = 0,
    // Всеобщее число комментов у статьи
    val comments: Int = 0,
    @ColumnInfo(name = "read_duration")
    val readDuration: Int = 0,
    @ColumnInfo(name = "updated_at")
    val updatedAt: Date = Date()
)