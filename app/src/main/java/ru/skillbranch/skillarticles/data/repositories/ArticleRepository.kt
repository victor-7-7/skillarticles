package ru.skillbranch.skillarticles.data.repositories

import android.util.Log
import androidx.core.math.MathUtils.clamp
import androidx.lifecycle.LiveData
import androidx.paging.PagingSource
import androidx.paging.PagingState
import ru.skillbranch.skillarticles.data.local.NetworkDataHolder
import ru.skillbranch.skillarticles.data.local.PrefManager
import ru.skillbranch.skillarticles.data.local.dao.ArticleContentsDao
import ru.skillbranch.skillarticles.data.local.dao.ArticleCountsDao
import ru.skillbranch.skillarticles.data.local.dao.ArticlePersonalInfosDao
import ru.skillbranch.skillarticles.data.local.dao.ArticlesDao
import ru.skillbranch.skillarticles.data.local.entities.ArticleFull
import ru.skillbranch.skillarticles.data.models.AppSettings
import ru.skillbranch.skillarticles.data.remote.RestService
import ru.skillbranch.skillarticles.data.remote.err.ApiError
import ru.skillbranch.skillarticles.data.remote.err.NoNetworkError
import ru.skillbranch.skillarticles.data.remote.res.CommentRes
import ru.skillbranch.skillarticles.extensions.data.toArticleContent
import ru.skillbranch.skillarticles.extensions.data.toArticleCounts
import ru.skillbranch.skillarticles.viewmodels.article.ArticleViewModel.Companion.NETWORK_PAGE_SIZE
import javax.inject.Inject

interface IArticleRepository : IRepository {
    fun findArticle(articleId: String): LiveData<ArticleFull>
    fun getAppSettings(): LiveData<AppSettings>
    fun updateSettings(appSettings: AppSettings)
    fun isAuth(): LiveData<Boolean>
    suspend fun toggleLike(articleId: String): Boolean
    suspend fun toggleBookmark(articleId: String): Boolean

    //    fun loadCommentsByRange(slug: String?, size: Int, articleId: String): List<CommentRes>
    suspend fun sendMessage(articleId: String, message: String, answerToMessageId: String?)
    /*fun loadAllComments(
        articleId: String, totalCount: Int,
        errHandler: (Throwable) -> Unit
    ): CommentsDataFactory*/

    suspend fun decrementLike(articleId: String)
    suspend fun incrementLike(articleId: String)
    suspend fun fetchArticleContent(articleId: String)
    fun findArticleCommentCount(articleId: String): LiveData<Int>
    suspend fun refreshCommentsCount(articleId: String)

    fun makeCommentsDataSource(articleId: String, total: Int): CommentsDataSource2
//    suspend fun loadComments(articleId: String, offset: Int?, limit: Int): List<CommentRes>
}

//===============================================================================

// Для тестирования
private var articleIdDH = (0..5).random().toString()
private var totalDH = NetworkDataHolder.commentsCount(articleIdDH)

//===============================================================================

class ArticleRepository @Inject constructor(
    private val prefs: PrefManager,
    private val network: RestService,
    private val articlesDao: ArticlesDao,
    private val articleContentsDao: ArticleContentsDao,
    private val articleCountsDao: ArticleCountsDao,
    private val articlePersonalInfosDao: ArticlePersonalInfosDao
) : IArticleRepository {

    override fun findArticle(articleId: String): LiveData<ArticleFull> =
        articlesDao.findFullArticle(articleId)

    override suspend fun fetchArticleContent(articleId: String) {
        // Загружаем контент статьи из сети
        val content = network.loadArticleContent(articleId)
        // Сохраняем контент локально в БД устройства
        articleContentsDao.insert(content.toArticleContent())
    }

    override fun findArticleCommentCount(articleId: String): LiveData<Int> =
        articleCountsDao.getCommentsCount(articleId)

    override fun getAppSettings(): LiveData<AppSettings> = prefs.appSettingsLive

    override fun updateSettings(appSettings: AppSettings) {
        prefs.isDarkMode = appSettings.isDarkMode
        prefs.isBigText = appSettings.isBigText
    }

    override fun isAuth(): LiveData<Boolean> = prefs.isAuthLive

    /*override fun loadAllComments(
        articleId: String,
        totalCount: Int,
        errHandler: (Throwable) -> Unit
    ) =
        CommentsDataFactory(
            itemProvider = network,
            articleId = articleId,
            totalCount = totalCount,
            errHandler = errHandler
        )*/

    /** Метод возвращает значение isLike сущности ArticlePersonalInfo */
    override suspend fun toggleLike(articleId: String): Boolean =
        articlePersonalInfosDao.toggleLikeOrInsert(articleId)

    /** Метод возвращает значение isBookmark сущности ArticlePersonalInfo */
    override suspend fun toggleBookmark(articleId: String): Boolean =
        articlePersonalInfosDao.toggleBookmarkOrInsert(articleId)

    /** Метод пробует сообщить серверу о необходимости записать в серверную БД,
     * что авторизованный юзер, имеющий токен, добавил статью (articleId) в закладки */
    suspend fun addBookmark(articleId: String) {
        val token = prefs.accessToken
        // Если юзер не авторизован, то выходим
        if (token.isEmpty()) return
        try {
            network.addBookmark(articleId, token)
        } catch (e: Throwable) {
            // Если нет сети, то выходим
            if (e is NoNetworkError) return
            // Прочие ошибки сервера бросим вверх для обработки на уровне ViewModel
            throw e
        }
    }

    /** Метод пробует сообщить серверу о необходимости записать в серверную БД,
     * что авторизованный юзер, имеющий токен, убрал статью (articleId) из закладок */
    suspend fun removeBookmark(articleId: String) {
        val token = prefs.accessToken
        // Если юзер не авторизован, то выходим
        if (token.isEmpty()) return
        try {
            network.removeBookmark(articleId, token)
        } catch (e: Throwable) {
            // Если нет сети, то выходим
            if (e is NoNetworkError) return
            // Прочие ошибки сервера бросим вверх для обработки на уровне ViewModel
            throw e
        }
    }

    // look at video (lecture 11, time code 02:20:33)
    override suspend fun decrementLike(articleId: String) {
        val token = prefs.accessToken
        // Если юзер не авторизован
        if (token.isEmpty()) {
            // Фиксируем в локальной БД уменьшение на 1 всеобщего
            //числа лайков у данной статьи и выходим
            articleCountsDao.decrementLike(articleId)
            return
        }
        try {
            val resp = network.decrementLike(articleId, token)
            articleCountsDao.updateLike(articleId, resp.likeCount)
        } catch (e: Throwable) {
            // Если ошибка ApiError.BadRequest, значит декрементировать
            // не надо, в том числе и локально. В случае остальных ошибок
            // надо записать декремент в локальную БД
            if (e !is ApiError.BadRequest) articleCountsDao.decrementLike(articleId)
            // Если ошибка в отсутствии сети, то выходим
            if (e is NoNetworkError) return
            // Прочие ошибки сервера бросим вверх для обработки на уровне ViewModel
            throw e
        }
    }

    override suspend fun incrementLike(articleId: String) {
        val token = prefs.accessToken
        // Если юзер не авторизован
        if (token.isEmpty()) {
            // Фиксируем в локальной БД увеличение на 1 всеобщего
            // числа лайков у данной статьи и выходим
            articleCountsDao.incrementLike(articleId)
            return
        }
        try {
            val resp = network.incrementLike(articleId, token)
            articleCountsDao.updateLike(articleId, resp.likeCount)
        } catch (e: Throwable) {
            // Если ошибка ApiError.BadRequest, значит инкрементировать
            // не надо, в том числе и локально. В случае остальных ошибок
            // надо записать инкремент в локальную БД
            if (e !is ApiError.BadRequest) articleCountsDao.incrementLike(articleId)
            // Если ошибка в отсутствии сети, то выходим
            if (e is NoNetworkError) return
            // Прочие ошибки сервера бросим вверх для обработки на уровне ViewModel
            throw e
        }
    }

    override suspend fun sendMessage(
        articleId: String, message: String, answerToMessageId: String?
    ) {
        // Для тестирования комментов
        NetworkDataHolder.sendMessage(articleIdDH, message, answerToMessageId)
        totalDH = NetworkDataHolder.commentsCount(articleIdDH)
        //--------------------------------------
        // Отправляем сообщение на сервер
        /*val (_, messageCount) = network.sendMessage(
            articleId,
            MessageReq(message, answerToMessageId),
            prefs.accessToken
        )
        // Обновляем в локальной БД изменившееся всеобщее число
        // комментов данной статьи, полученное с сервера
        articleCountsDao.updateCommentsCount(articleId, messageCount)*/

//        articleCountsDao.incrementCommentsCount(articleId) // <- before lecture 11
    }

    override suspend fun refreshCommentsCount(articleId: String) {
        // Загружаем метрики статьи из сети
        val metrics = network.loadArticleCounts(articleId)
        // Сохраняем свежие метрики локально.
        // Реализация отличается от таковой в видео (lecture 11, 01:44:49)
        articleCountsDao.update(metrics.toArticleCounts())
    }

    override fun makeCommentsDataSource(articleId: String, total: Int): CommentsDataSource2 {
        return CommentsDataSource2(articleId, network, total)
    }

    // Для тестирования комментов
    fun updateCommentsDH() {
        articleIdDH = (0..5).random().toString()
        totalDH = NetworkDataHolder.commentsCount(articleIdDH)
    }

    /*
        override fun loadCommentsByRange(
            slug: String?,
            size: Int,
            articleId: String
        ): List<CommentRes> {
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
    */
}

//============================================================================

/*class CommentsDataFactory(
    private val itemProvider: RestService,
    private val articleId: String,
    private val totalCount: Int,
    private val errHandler: (Throwable) -> Unit
) : DataSource.Factory<String, CommentRes>() {
    override fun create(): DataSource<String, CommentRes> =
        CommentsDataSource(itemProvider, articleId, totalCount, errHandler)
}*/

//============================================================================

/*class CommentsDataSource(
    private val itemProvider: RestService,
    private val articleId: String,
    private val totalCount: Int,
    private val errHandler: (Throwable) -> Unit
) : ItemKeyedDataSource<String, CommentRes>() {

    override fun loadInitial(
        params: LoadInitialParams<String>,
        callback: LoadInitialCallback<CommentRes>
    ) {
        try {
            // synchronous call execute
            val result = itemProvider.loadComments(
                articleId,
                params.requestedInitialKey,
                params.requestedLoadSize
            ).execute()
            callback.onResult(
                if (totalCount > 0) result.body()!! else emptyList(),
                0,
                totalCount
            )
        } catch (e: Throwable) {
            // handle network errors in viewModel
            errHandler(e)
        }
        Log.d(
            "M_S_Paging", "ArticleRepository loadInitial: " +
                    "key: ${params.requestedInitialKey} " +
                    "size: ${params.requestedLoadSize} " +
                    "total: $totalCount"
        )
    }

    override fun loadAfter(
        params: LoadParams<String>,
        callback: LoadCallback<CommentRes>
    ) {
        try {
            // synchronous call execute
            val result = itemProvider.loadComments(
                articleId,
                params.key,
                params.requestedLoadSize
            ).execute()
            callback.onResult(result.body()!!)
        } catch (e: Throwable) {
            // handle network errors in viewModel
            errHandler(e)
        }
        Log.d(
            "M_S_Paging", "ArticleRepository  loadAfter: " +
                    "key: ${params.key} " +
                    "size: ${params.requestedLoadSize}"
        )
    }

    override fun loadBefore(
        params: LoadParams<String>,
        callback: LoadCallback<CommentRes>
    ) {
        try {
            // synchronous call execute
            val result = itemProvider.loadComments(
                articleId,
                params.key,
                -params.requestedLoadSize
            ).execute()
            callback.onResult(result.body()!!)
        } catch (e: Throwable) {
            // handle network errors in viewModel
            errHandler(e)
        }
        Log.d(
            "M_S_Paging", "ArticleRepository loadBefore: " +
                    "key: ${params.key} " +
                    "size: ${params.requestedLoadSize}"
        )
    }
    override fun getKey(item: CommentRes): String = item.id  // item.slug ??
}*/

class CommentsDataSource2(
    val articleId: String,
    val network: RestService,
    private var total: Int
) : PagingSource<Int, CommentRes>() {

    override fun getRefreshKey(state: PagingState<Int, CommentRes>): Int? {
        // Индекс СТРАНИЦЫ для рефрешной загрузки
        val refreshKey = state.anchorPosition?.let { anchorPosition ->
            val anchorPage = state.closestPageToPosition(anchorPosition)
            anchorPage?.prevKey?.plus(1) ?: anchorPage?.nextKey?.minus(1)
        }

        Log.d("M_S_Paging", "ArticleRepository getRefreshKey() " +
            "[anchor: ${state.anchorPosition}] [refreshKey: $refreshKey]")

        return refreshKey
    }

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, CommentRes> {

        val pageKey = params.key ?: 0
        val offset = clamp(pageKey * NETWORK_PAGE_SIZE, 0, totalDH)
        val loadSize = params.loadSize

        return try {
//            val comments = network.loadComments2(articleId, pageKey, pageSize)
            //------------------------------------
            // Для тестирования
            total = totalDH
            val comments = NetworkDataHolder.loadComments(articleIdDH, offset, loadSize)
            //------------------------------------

            val prevKey = if (pageKey > 0) pageKey.minus(1) else null

            val nextKey = if(comments.isNotEmpty()) {
                if (pageKey * NETWORK_PAGE_SIZE + loadSize > total) null
                else  pageKey.plus(loadSize / NETWORK_PAGE_SIZE)
            } else null

            val itemsAfter = nextKey?.let { total - it * NETWORK_PAGE_SIZE } ?: 0

            Log.d("M_S_Paging", "ArticleRepository load [key: $pageKey] " +
                    "[offset: $offset] [limit: $loadSize] [loaded: ${comments.size}] " +
                    "[prev: $prevKey] [next: $nextKey] [before: $offset] " +
                    "[after: $itemsAfter] [total: $total]")

            LoadResult.Page(
                data = comments,
                //Key for previous page if more data can be loaded in that direction
                prevKey = prevKey,
                // Key for next page if more data can be loaded in that direction
                nextKey = nextKey,
                // Optional count of items before the loaded data
                itemsBefore = offset,
                // Optional count of items after the loaded data
                itemsAfter = itemsAfter
            )
            //------------------------------------------
        } catch (t: Throwable) {
            LoadResult.Error(t)
        }
    }
}

