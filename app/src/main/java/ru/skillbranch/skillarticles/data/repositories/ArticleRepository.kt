package ru.skillbranch.skillarticles.data.repositories

import android.util.Log
import androidx.core.math.MathUtils.clamp
import androidx.lifecycle.LiveData
import androidx.paging.PagingSource
import androidx.paging.PagingState
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
import ru.skillbranch.skillarticles.data.remote.req.MessageReq
import ru.skillbranch.skillarticles.data.remote.res.CommentRes
import ru.skillbranch.skillarticles.extensions.data.toArticleContent
import ru.skillbranch.skillarticles.extensions.data.toArticleCounts
import ru.skillbranch.skillarticles.viewmodels.article.ArticleViewModel.Companion.COMMENTS_NW_PAGE_SIZE
import javax.inject.Inject

interface IArticleRepository : IRepository {
    fun findArticle(articleId: String): LiveData<ArticleFull>
    fun getAppSettings(): LiveData<AppSettings>
    fun updateSettings(appSettings: AppSettings)
    fun isAuth(): LiveData<Boolean>
    suspend fun toggleLike(articleId: String): Boolean
    suspend fun toggleBookmark(articleId: String): Boolean
    suspend fun sendMessage(articleId: String, message: String, answerToMessageId: String?)
    suspend fun decrementLike(articleId: String)
    suspend fun incrementLike(articleId: String)
    suspend fun fetchArticleContent(articleId: String)
    fun findArticleCommentCount(articleId: String): LiveData<Int>
    suspend fun refreshCommentsCount(articleId: String)
    fun makeCommentsDataSource(articleId: String, total: Int): CommentsDataSource
}

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
        // Загружаем контент статьи из сети. Обрезаем суффикс перед
        // запросом к серверу
        val content = network.loadArticleContent(articleId.substring(0, 24))
        // Сохраняем контент локально в БД устройства. Перед вставкой контента
        // в кэш восстанавливаем суффикс
        articleContentsDao.insert(content.copy(articleId = articleId).toArticleContent())
    }

    override fun findArticleCommentCount(articleId: String): LiveData<Int> =
        articleCountsDao.getCommentsCount(articleId)

    override fun getAppSettings(): LiveData<AppSettings> = prefs.appSettingsLive

    override fun updateSettings(appSettings: AppSettings) {
        prefs.isDarkMode = appSettings.isDarkMode
        prefs.isBigText = appSettings.isBigText
    }

    override fun isAuth(): LiveData<Boolean> = prefs.isAuthLive

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
            // Обрезаем суффикс перед запросом к серверу
            network.addBookmark(articleId.substring(0, 24), token)
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
            // Обрезаем суффикс перед запросом к серверу
            network.removeBookmark(articleId.substring(0, 24), token)
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
            articleCountsDao.decrementLikes(articleId)
            return
        }
        try {
            // Обрезаем суффикс перед запросом к серверу
            val resp = network.decrementLike(articleId.substring(0, 24), token)
            articleCountsDao.updateLikes(articleId, resp.likeCount)
        } catch (e: Throwable) {
            // Если ошибка ApiError.BadRequest, значит декрементировать
            // не надо, в том числе и локально. В случае остальных ошибок
            // надо записать декремент в локальную БД
            if (e !is ApiError.BadRequest) articleCountsDao.decrementLikes(articleId)
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
            articleCountsDao.incrementLikes(articleId)
            return
        }
        try {
            // Обрезаем суффикс перед запросом к серверу
            val resp = network.incrementLike(articleId.substring(0, 24), token)
            articleCountsDao.updateLikes(articleId, resp.likeCount)
        } catch (e: Throwable) {
            // Если ошибка ApiError.BadRequest, значит инкрементировать
            // не надо, в том числе и локально. В случае остальных ошибок
            // надо записать инкремент в локальную БД
            if (e !is ApiError.BadRequest) articleCountsDao.incrementLikes(articleId)
            // Если ошибка в отсутствии сети, то выходим
            if (e is NoNetworkError) return
            // Прочие ошибки сервера бросим вверх для обработки на уровне ViewModel
            throw e
        }
    }

    override suspend fun sendMessage(
        articleId: String, message: String, answerToMessageId: String?
    ) {
        // Отправляем сообщение на сервер
        val (_, messageCount) = network.sendMessage(
            // Обрезаем суффикс перед запросом к серверу
            articleId.substring(0, 24),
            MessageReq(message, answerToMessageId),
            prefs.accessToken
        )
        // Обновляем в локальной БД изменившееся всеобщее число
        // комментов данной статьи, полученное с сервера
        articleCountsDao.updateCommentsCount(articleId, messageCount)
    }

    override suspend fun refreshCommentsCount(articleId: String) {
        // Загружаем метрики статьи из сети. Обрезаем суффикс перед запросом к серверу
        val metrics = network.loadArticleCounts(articleId.substring(0, 24))
        // Сохраняем свежие метрики локально.
        // Реализация отличается от таковой в видео (lecture 11, 01:44:49)
        // Перед апдейтом кэша восстанавливаем суффикс
        articleCountsDao.update(metrics.copy(articleId = articleId).toArticleCounts())
    }

    override fun makeCommentsDataSource(articleId: String, total: Int): CommentsDataSource {
        return CommentsDataSource(articleId, network, total)
    }
}

//=====================================================================

class CommentsDataSource(
    val articleId: String,
    val network: RestService,
    /** Общее число комментов у статьи */
    private val total: Int
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
        val offset = clamp(pageKey * COMMENTS_NW_PAGE_SIZE, 0, total)
        val loadSize = params.loadSize

        return try {
            // Обрезаем суффикс перед запросом к серверу
            val comments = network.loadComments(articleId.substring(0, 24), pageKey, loadSize)

            val prevKey = if (pageKey > 0) pageKey.minus(1) else null
            // Лишние комменты (сервер продолжает возвращать комменты, зацикливая их
            // и превышая total-значение). Это багфикс серверного поведения
            var extra = 0
            val nextKey = if(comments.isNotEmpty()) {
                if (pageKey * COMMENTS_NW_PAGE_SIZE + comments.size >= total) {
                    extra = pageKey * COMMENTS_NW_PAGE_SIZE + comments.size - total
                    null
                }
                else  pageKey.plus(loadSize / COMMENTS_NW_PAGE_SIZE)
            }
            else null

            val itemsAfter = nextKey?.let { total - it * COMMENTS_NW_PAGE_SIZE } ?: 0

            val type = when(params) {
                is LoadParams.Refresh -> "REF"
                is LoadParams.Prepend -> "PRE"
                is LoadParams.Append -> "APP"
            }
            Log.d("M_S_Paging", "ArticleRepo load() $type [key: $pageKey] " +
                    "[offset: $offset] [limit: $loadSize] [loaded: ${comments.size}] " +
                    "[prev: $prevKey] [next: $nextKey] [before: $offset] " +
                    "[after: $itemsAfter] [total: $total] [extra: $extra]")

            LoadResult.Page(
                data = if (extra > 0) comments.subList(0, comments.size - extra) else comments,
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

