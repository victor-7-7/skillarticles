package ru.skillbranch.skillarticles.data.repositories

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.paging.DataSource
import androidx.paging.ItemKeyedDataSource
import ru.skillbranch.skillarticles.data.NetworkDataHolder
import ru.skillbranch.skillarticles.data.local.DbManager.db
import ru.skillbranch.skillarticles.data.local.dao.ArticleContentsDao
import ru.skillbranch.skillarticles.data.local.dao.ArticleCountsDao
import ru.skillbranch.skillarticles.data.local.dao.ArticlePersonalInfosDao
import ru.skillbranch.skillarticles.data.local.dao.ArticlesDao
import ru.skillbranch.skillarticles.data.local.entities.ArticleFull
import ru.skillbranch.skillarticles.data.models.AppSettings
import ru.skillbranch.skillarticles.data.models.CommentItemData
import ru.skillbranch.skillarticles.data.models.User
import ru.skillbranch.skillarticles.extensions.data.toArticleContent
import java.lang.Thread.sleep
import kotlin.math.abs

interface IArticleRepository {
    fun findArticle(articleId: String): LiveData<ArticleFull>
    fun getAppSettings(): LiveData<AppSettings>
    fun updateSettings(appSettings: AppSettings)
    fun isAuth(): LiveData<Boolean>
    fun toggleLike(articleId: String)
    fun toggleBookmark(articleId: String)
    fun loadCommentsByRange(slug: String?, size: Int, articleId: String): List<CommentItemData>
    fun sendMessage(articleId: String, text: String, answerToSlug: String?)
    fun loadAllComments(articleId: String, totalCount: Int): CommentsDataFactory
    fun decrementLike(articleId: String)
    fun incrementLike(articleId: String)
    fun fetchArticleContent(articleId: String)
    fun findArticleCommentCount(articleId: String): LiveData<Int>
}

object ArticleRepository : IArticleRepository {
    private val network = NetworkDataHolder
    private val rootRepository = RootRepository
    private var articlesDao = db.articlesDao()
    private var articlePersonalInfosDao = db.articlePersonalInfosDao()
    private var articleCountsDao = db.articleCountsDao()
    private var articleContentsDao = db.articleContentsDao()

    override fun findArticle(articleId: String): LiveData<ArticleFull> =
        articlesDao.findFullArticle(articleId)

    override fun fetchArticleContent(articleId: String) {
        val content = network.loadArticleContent(articleId).apply { sleep(1500) }
        articleContentsDao.insert(content.toArticleContent())
    }

    override fun findArticleCommentCount(articleId: String): LiveData<Int> =
        articleCountsDao.getCommentsCount(articleId)

    //from preferences
    override fun getAppSettings(): LiveData<AppSettings> = rootRepository.appSettings()

    override fun updateSettings(appSettings: AppSettings) {
        rootRepository.updateSettings(appSettings)
    }

    override fun isAuth(): LiveData<Boolean> = rootRepository.isAuth()

    override fun toggleLike(articleId: String) {
        articlePersonalInfosDao.toggleLikeOrInsert(articleId)
    }

    override fun toggleBookmark(articleId: String) {
        articlePersonalInfosDao.toggleBookmarkOrInsert(articleId)
    }

    override fun loadAllComments(articleId: String, totalCount: Int) =
        CommentsDataFactory(
            itemProvider = ::loadCommentsByRange,
            articleId = articleId, totalCount = totalCount
        )

    override fun decrementLike(articleId: String) {
        articleCountsDao.decrementLike(articleId)
    }

    override fun incrementLike(articleId: String) {
        articleCountsDao.incrementLike(articleId)
    }

    override fun loadCommentsByRange(
        slug: String?,
        size: Int,
        articleId: String
    ): List<CommentItemData> {
        val data = network.commentsData.getOrElse(articleId) { mutableListOf() }
        return when {
            // Если ключа нет, то берем пачку комментов от начала
            slug == null -> data.take(size)
            // Находим коммент по ключу и берем пачку комментов после него
            size > 0 -> data.dropWhile { it.slug != slug }
                .drop(1).take(size)
            // Находим коммент по ключу (идя снизу) и берем пачку комментов перед ним
            size < 0 -> data.dropLastWhile { it.slug != slug }
                .dropLast(1).takeLast(abs(size))
            else -> emptyList()
        } //.apply { sleep(3000) }
    }

    override fun sendMessage(articleId: String, text: String, answerToSlug: String?) {
        network.sendMessage(
            articleId, text, answerToSlug,
            User(
                "777", "John Doe John Doe John Doe",
                "https://skill-branch.ru/img/mail/bot/android-category.png"
            )
        )
        articleCountsDao.incrementCommentsCount(articleId)
    }

    // Для тестов
    fun setupTestDao(
        articlesDao: ArticlesDao,
        articleCountsDao: ArticleCountsDao,
        articleContentDao: ArticleContentsDao,
        articlePersonalDao: ArticlePersonalInfosDao
    ) {
        this.articlesDao = articlesDao
        this.articleCountsDao = articleCountsDao
        this.articleContentsDao = articleContentDao
        this.articlePersonalInfosDao = articlePersonalDao
    }
}

//============================================================================

class CommentsDataFactory(
    private val itemProvider: (String?, Int, String) -> List<CommentItemData>,
    private val articleId: String,
    private val totalCount: Int
) : DataSource.Factory<String?, CommentItemData>() {
    override fun create() =
        CommentsDataSource(itemProvider, articleId, totalCount)
}

//============================================================================

class CommentsDataSource(
    private val itemProvider: (String?, Int, String) -> List<CommentItemData>,
    private val articleId: String,
    private val totalCount: Int
) : ItemKeyedDataSource<String, CommentItemData>() {

    override fun loadInitial(
        params: LoadInitialParams<String>,
        callback: LoadInitialCallback<CommentItemData>
    ) {
        val result = itemProvider(
            params.requestedInitialKey,
            params.requestedLoadSize, articleId
        )
        Log.d(
            "M_ArticleRepository", "loadInitial: " +
                    "key: ${params.requestedInitialKey} " +
                    "size: ${result.size} " +
                    "total: $totalCount"
        )
        callback.onResult(
            if (totalCount > 0) result else emptyList(),
            0,
            totalCount
        )
    }

    override fun loadAfter(
        params: LoadParams<String>,
        callback: LoadCallback<CommentItemData>
    ) {
        val result = itemProvider(params.key, params.requestedLoadSize, articleId)
        Log.d(
            "M_ArticleRepository", "loadAfter: " +
                    "key: ${params.key} " +
                    "size: ${result.size}"
        )
        callback.onResult(result)
    }

    override fun loadBefore(
        params: LoadParams<String>,
        callback: LoadCallback<CommentItemData>
    ) {
        val result = itemProvider(params.key, -params.requestedLoadSize, articleId)
        Log.d(
            "M_ArticleRepository", "loadBefore: " +
                    "key: ${params.key} " +
                    "size: ${result.size}"
        )
        callback.onResult(result)
    }

    override fun getKey(item: CommentItemData): String = item.slug
}
