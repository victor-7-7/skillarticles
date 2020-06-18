package ru.skillbranch.skillarticles.data.repositories

import android.util.Log
import androidx.paging.DataSource
import androidx.paging.PositionalDataSource
import ru.skillbranch.skillarticles.data.LocalDataHolder
import ru.skillbranch.skillarticles.data.NetworkDataHolder
import ru.skillbranch.skillarticles.data.models.ArticleItemData
import java.lang.Thread.sleep

object ArticlesRepository {
    private val local = LocalDataHolder
    private val network = NetworkDataHolder

//    fun loadArticles(): LiveData<List<ArticleItemData>?> = local.findArticles()

    fun allArticles(): ArticlesDataFactory = ArticlesDataFactory(
        ArticleStrategy.AllArticles(::findArticlesByRange)
    )

    fun searchArticles(query: String) = ArticlesDataFactory(
        ArticleStrategy.SearchArticles(::searchArticlesByTitle, query)
    )

    fun bookmarkArticles(): ArticlesDataFactory = ArticlesDataFactory(
        ArticleStrategy.BookmarkArticles(::findArticlesByBookmark)
    )

    fun searchBookmark(query: String) = ArticlesDataFactory(
        ArticleStrategy.SearchBookmark(::searchBookmarkArticlesByTitle, query)
    )

    private fun findArticlesByRange(start: Int, size: Int) =
        local.localArticleItems.drop(start).take(size)

    private fun findArticlesByBookmark(start: Int, size: Int) =
        local.localArticleItems.filter { it.isBookmark }.drop(start).take(size)

    private fun searchArticlesByTitle(start: Int, size: Int, query: String) =
        local.localArticleItems.asSequence()
            .filter { it.title.contains(query, true) }
            .drop(start).take(size).toList()

    private fun searchBookmarkArticlesByTitle(start: Int, size: Int, query: String) =
        local.localArticleItems.asSequence()
            .filter { it.isBookmark && it.title.contains(query, true) }
            .drop(start).take(size).toList()

    fun loadArticlesFromNetwork(start: Int, size: Int) =
        network.networkArticleItems.drop(start).take(size).apply { sleep(500) }

    fun insertArticlesToDb(articles: List<ArticleItemData>) {
        local.localArticleItems.addAll(articles).apply { sleep(100) }
    }

    fun updateBookmark(articleId: String, isChecked: Boolean) {
        val index = local.localArticleItems.indexOfFirst { it.id == articleId }
        if (index == -1) return
        local.localArticleItems[index] =
            local.localArticleItems[index].copy(isBookmark = isChecked)
    }
}

//============================================================================

class ArticlesDataFactory(val strategy: ArticleStrategy) :
    DataSource.Factory<Int, ArticleItemData>() {
    override fun create(): DataSource<Int, ArticleItemData> = ArticleDataSource(strategy)
}

//============================================================================

class ArticleDataSource(private val strategy: ArticleStrategy) :
    PositionalDataSource<ArticleItemData>() {

    override fun loadInitial(
        params: LoadInitialParams,
        callback: LoadInitialCallback<ArticleItemData>
    ) {
        val result = strategy.getItems(
            params.requestedStartPosition,
            params.requestedLoadSize
        )
        Log.d(
            "M_ArticlesRepository", "loadInitial: " +
                    "start - ${params.requestedStartPosition} " +
                    "size - ${params.requestedLoadSize} " +
                    "resultSize - ${result.size}"
        )
        callback.onResult(result, params.requestedStartPosition)
    }

    override fun loadRange(params: LoadRangeParams, callback: LoadRangeCallback<ArticleItemData>) {
        val result = strategy.getItems(params.startPosition, params.loadSize)
        Log.d(
            "M_ArticlesRepository", "loadRange: " +
                    "start - ${params.startPosition} " +
                    "size - ${params.loadSize} " +
                    "resultSize - ${result.size}"
        )
        callback.onResult(result)
    }
}

//============================================================================

sealed class ArticleStrategy() {
    abstract fun getItems(start: Int, size: Int): List<ArticleItemData>

    class AllArticles(
        private val itemProvider: (Int, Int) -> List<ArticleItemData>
    ) : ArticleStrategy() {
        override fun getItems(start: Int, size: Int) =
            itemProvider(start, size)
    }

    class SearchArticles(
        private val itemProvider: (Int, Int, String) -> List<ArticleItemData>,
        private val query: String
    ) : ArticleStrategy() {
        override fun getItems(start: Int, size: Int) =
            itemProvider(start, size, query)
    }

    class BookmarkArticles(
        private val itemProvider: (Int, Int) -> List<ArticleItemData>
    ) : ArticleStrategy() {
        override fun getItems(start: Int, size: Int) =
            itemProvider(start, size)
    }

    class SearchBookmark(
        private val itemProvider: (Int, Int, String) -> List<ArticleItemData>,
        private val query: String
    ) : ArticleStrategy() {
        override fun getItems(start: Int, size: Int) =
            itemProvider(start, size, query)
    }
}