package ru.skillbranch.skillarticles.data.local.dao

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Query
import androidx.room.RawQuery
import androidx.room.Transaction
import androidx.sqlite.db.SimpleSQLiteQuery
import ru.skillbranch.skillarticles.data.local.entities.Article
import ru.skillbranch.skillarticles.data.local.entities.ArticleFull
import ru.skillbranch.skillarticles.data.local.entities.ArticleItem
import ru.skillbranch.skillarticles.extensions.logdu

@Dao
interface ArticlesDao : BaseDao<Article> {

    @Transaction
    suspend fun upsert(list: List<Article>) {
        // @Insert(onConflict = OnConflictStrategy.IGNORE):
        // An Insert method that returns the inserted rows ids, will
        // return -1 for rows that are not inserted since this strategy
        // will ignore the row if there is a conflict
        // Вставляем [insert] список статей в таблицу articles по стратегии IGNORE
        val resultList = insert(list)
        Log.d("M_S_Paging", "========= ArticlesDao insert() => List<Long>: $resultList")

        /** С костылем (фиксящем серверный баг) в виде добавления id-суффикса
         * (в ArticlesRepository) проигноренных статей не будет вовсе. Но на
         * безкостыльное будущее оставляем нижеследующий код */
        // Извлекаем список проигноренных статей
        val ignoredList = resultList.mapIndexed { index, recordResult ->
            // Для проигноренных статей (уже имевшихся в базе) recordResult будет -1
            if (recordResult == -1L) list[index] else null
        }.filterNotNull()
        logdu("M_S_Paging", "========= ArticlesDao ignored: ${ignoredList.size}")

        ignoredList.also {
            if (it.isNotEmpty()) {
                // Апдейтим [update] базу проигноренными статьями
                val updatedCount = update(it)
                Log.d("M_S_Paging", "========= ArticlesDao updated: $updatedCount")
            }
        }
    }

    @RawQuery(observedEntities = [ArticleItem::class])
    fun pagingSource(simpleSQLiteQuery: SimpleSQLiteQuery): PagingSource<Int, ArticleItem>

    @Query(
        """
        SELECT COUNT(1) FROM articles
    """
    )
    suspend fun articlesDbCount(): Long

    @Query(
        """
        SELECT id FROM articles ORDER BY cached_at DESC LIMIT 1
    """
    )
    suspend fun lastArticleId(): String?

    @Query(
        """
        SELECT * FROM ArticleFull WHERE id = :articleId
    """
    )
    fun findFullArticle(articleId: String): LiveData<ArticleFull>

    // Нисходящая/убывающая (DESC) сортировка по дате означает, что первой
    // записью будет та, у которой значение в столбце дата самое свежее
    // (недавнее), затем идут записи с более старыми датами. Например,
    // первая - с датой 21.08.20. Вторая - с датой 19.08.20, затем 15.07.20 и т.д.
    @Query(
        """
        SELECT id FROM articles ORDER BY date DESC LIMIT 1
    """
    )
    /** Последняя (по дате публикации) статья, сохраненная в БД на устройстве */
    suspend fun findLastArticleId(): String?

    @Query("SELECT * FROM articles")
    // Для тестов
    suspend fun findArticlesTest(): List<Article>

}

