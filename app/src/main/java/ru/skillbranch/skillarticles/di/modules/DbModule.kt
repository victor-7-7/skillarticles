package ru.skillbranch.skillarticles.di.modules

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ApplicationComponent
import dagger.hilt.android.qualifiers.ApplicationContext
import ru.skillbranch.skillarticles.data.local.AppDb
import javax.inject.Singleton

@InstallIn(ApplicationComponent::class)
@Module
class DbModule {
    @Provides
    @Singleton
    fun provideAppDb(@ApplicationContext ctx: Context): AppDb = Room
        .databaseBuilder(ctx, AppDb::class.java, AppDb.DATABASE_NAME).build()

    @Provides
    fun provideArticlesDao(db: AppDb) = db.articlesDao()

    @Provides
    fun provideArticleCountsDao(db: AppDb) = db.articleCountsDao()

    @Provides
    fun provideCategoriesDao(db: AppDb) = db.categoriesDao()

    @Provides
    fun provideArticlePersonalInfosDao(db: AppDb) = db.articlePersonalInfosDao()

    @Provides
    fun provideTagsDao(db: AppDb) = db.tagsDao()

    @Provides
    fun provideArticleContentsDao(db: AppDb) = db.articleContentsDao()
}