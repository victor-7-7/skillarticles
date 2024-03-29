package ru.skillbranch.skillarticles.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import ru.skillbranch.skillarticles.BuildConfig
import ru.skillbranch.skillarticles.data.local.dao.*
import ru.skillbranch.skillarticles.data.local.entities.*

@Database(
    entities = [Article::class, ArticleCounts::class, Category::class,
        ArticlePersonalInfo::class, Tag::class, ArticleTagXRef::class,
        ArticleContent::class],
    version = AppDb.DATABASE_VERSION,
    exportSchema = false,
    views = [ArticleItem::class, ArticleFull::class]
)
@TypeConverters(DateConverter::class)
abstract class AppDb : RoomDatabase() {
    companion object {
        const val DATABASE_NAME = BuildConfig.APPLICATION_ID + ".db"
        const val DATABASE_VERSION = 5
    }

    abstract fun articlesDao(): ArticlesDao
    abstract fun articleCountsDao(): ArticleCountsDao
    abstract fun categoriesDao(): CategoriesDao
    abstract fun articlePersonalInfosDao(): ArticlePersonalInfosDao
    abstract fun tagsDao(): TagsDao
    abstract fun articleContentsDao(): ArticleContentsDao
}



