package ru.skillbranch.skillarticles.data.local.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.*

@Entity(tableName = "article_personal_infos")
/** источник правды для непостоянных полей isLike/isBookmark -> локальная БД */
data class ArticlePersonalInfo(
    @PrimaryKey
    @ColumnInfo(name = "article_id")
    val articleId: String,
    @ColumnInfo(name = "is_like")
    // Лайкнул ли статью пользователь данного девайса
    val isLike: Boolean = false,
    @ColumnInfo(name = "is_bookmark")
    // Добавил ли статью в закладки пользователь данного девайса
    val isBookmark: Boolean = false,
    /** Момент, когда юзер на девайсе тапнул по лайку или закладке у статьи.
     * При первом тап-событии в таблице будет создана запись. При последующих
     * тапах на этой статье поле updatedAt в таблице БД будет менятся */
    @ColumnInfo(name = "updated_at")
    val updatedAt: Date = Date()
)