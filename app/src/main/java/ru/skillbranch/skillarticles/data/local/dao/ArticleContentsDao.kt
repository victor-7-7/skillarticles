package ru.skillbranch.skillarticles.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import ru.skillbranch.skillarticles.data.local.entities.ArticleContent

@Dao
interface ArticleContentsDao : BaseDao<ArticleContent> {

    // REPLACE: если запись с таким ключом (id) уже есть в базе,
    // то она затирается данными из новой записи
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    override fun insert(obj: ArticleContent): Long

}