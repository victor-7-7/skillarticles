package ru.skillbranch.skillarticles.data.local.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import ru.skillbranch.skillarticles.data.local.entities.ArticleTagXRef
import ru.skillbranch.skillarticles.data.local.entities.Tag

@Dao
interface TagsDao : BaseDao<Tag> {
    @Query(
        """
        SELECT tag FROM article_tags ORDER BY use_count DESC
    """
    )
    fun findTags(): LiveData<List<String>>

    @Query(
        """
        SELECT tag FROM article_tags 
        INNER JOIN article_tag_x_ref AS refs ON refs.t_id = tag 
        WHERE refs.a_id = :articleId
    """
    )
    fun findTagsByArticleId(articleId: String): LiveData<List<String>>

    @Query(
        """
        UPDATE article_tags SET use_count = use_count + 1
        WHERE tag = :tag
    """
    )
    suspend fun incrementTagUseCount(tag: String)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertRefs(refs: List<ArticleTagXRef>): List<Long>
}