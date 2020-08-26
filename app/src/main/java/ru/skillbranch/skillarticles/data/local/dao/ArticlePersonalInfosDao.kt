package ru.skillbranch.skillarticles.data.local.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import ru.skillbranch.skillarticles.data.local.entities.ArticlePersonalInfo

@Dao
interface ArticlePersonalInfosDao : BaseDao<ArticlePersonalInfo> {

    @Transaction
    suspend fun upsert(list: List<ArticlePersonalInfo>) {
        insert(list).mapIndexed { index, recordResult ->
            if (recordResult == -1L) list[index] else null
        }.filterNotNull().also { if (it.isNotEmpty()) update(it) }
    }

    @Query(
        """
        SELECT * FROM article_personal_infos
    """
    )
    fun findPersonalInfos(): LiveData<List<ArticlePersonalInfo>>

    @Query("SELECT * FROM article_personal_infos WHERE article_id = :articleId")
    // Для тестов
    suspend fun findPersonalInfosTest(articleId: String): ArticlePersonalInfo

    @Query(
        """
        SELECT * FROM article_personal_infos WHERE article_id = :articleId
    """
    )
    fun findPersonalInfos(articleId: String): LiveData<ArticlePersonalInfo>

    @Query(
        """
        UPDATE article_personal_infos 
        SET is_like = NOT is_like, updated_at = CURRENT_TIMESTAMP
        WHERE article_id = :articleId
    """
    )
    // https://developer.android.com/reference/androidx/room/Query
    // UPDATE or DELETE queries can return void or int. If it is an int,
    // the value is the number of rows affected by this query
    // Возвращает количество обновленных строк
    suspend fun toggleLike(articleId: String): Int

    @Transaction
    suspend fun toggleLikeOrInsert(articleId: String): Boolean {
        // Делаем попытку обновить запись в article_personal_infos.
        // Если метод toggleLike вернул 0, значит в таблице еще
        // нет записи с article_id == :articleId, а значит эту статью
        // юзер никогда не лайкал / не закладывал. Поэтому создаем сущность
        // ArticlePersonalInfo [isLike = true] и вставляем ее в таблицу
        if (toggleLike(articleId) == 0) insert(
            ArticlePersonalInfo(articleId = articleId, isLike = true)
        )
        return isLiked(articleId)
    }

    @Query(
        """
        SELECT is_like FROM article_personal_infos WHERE article_id = :articleId
    """
    )
    suspend fun isLiked(articleId: String): Boolean

    @Query(
        """
        UPDATE article_personal_infos 
        SET is_bookmark = NOT is_bookmark, updated_at = CURRENT_TIMESTAMP
        WHERE article_id = :articleId
    """
    )
    // Возвращает количество обновленных строк
    suspend fun toggleBookmark(articleId: String): Int

    @Transaction
    suspend fun toggleBookmarkOrInsert(articleId: String): Boolean {
        if (toggleBookmark(articleId) == 0) insert(
            ArticlePersonalInfo(articleId = articleId, isBookmark = true)
        )
        return isBookmarked(articleId)
    }

    @Query(
        """
        SELECT is_bookmark FROM article_personal_infos WHERE article_id = :articleId
    """
    )
    suspend fun isBookmarked(articleId: String): Boolean

}