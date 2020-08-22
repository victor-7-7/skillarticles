package ru.skillbranch.skillarticles.data.repositories

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.paging.DataSource
import androidx.paging.PositionalDataSource
import androidx.sqlite.db.SimpleSQLiteQuery
import ru.skillbranch.skillarticles.data.local.DbManager.db
import ru.skillbranch.skillarticles.data.local.dao.*
import ru.skillbranch.skillarticles.data.local.entities.ArticleItem
import ru.skillbranch.skillarticles.data.local.entities.ArticleTagXRef
import ru.skillbranch.skillarticles.data.local.entities.CategoryData
import ru.skillbranch.skillarticles.data.local.entities.Tag
import ru.skillbranch.skillarticles.data.remote.NetworkManager
import ru.skillbranch.skillarticles.data.remote.res.ArticleRes
import ru.skillbranch.skillarticles.extensions.data.toArticle
import ru.skillbranch.skillarticles.extensions.data.toArticleContent
import ru.skillbranch.skillarticles.extensions.data.toArticleCounts
import ru.skillbranch.skillarticles.extensions.data.toCategory

interface IArticlesRepository {
    suspend fun loadArticlesFromNetwork(last: String? = null, size: Int = 10): Int
    suspend fun insertArticlesToDb(articles: List<ArticleRes>)
    suspend fun toggleBookmark(articleId: String): Boolean
    suspend fun toggleLike(articleId: String)
    fun findTags(): LiveData<List<String>>
    fun findCategoriesData(): LiveData<List<CategoryData>>
    fun rawQueryArticles(filter: ArticleFilter): DataSource.Factory<Int, ArticleItem>
    suspend fun incrementTagUseCount(tag: String)
    suspend fun findLastArticleId(): String?
    suspend fun fetchArticleContent(articleId: String)
    suspend fun removeArticleContent(articleId: String)
}

object ArticlesRepository : IArticlesRepository {
    private val network = NetworkManager.api
    private var articlesDao = db.articlesDao()
    private var articleContentsDao = db.articleContentsDao()
    private var articleCountsDao = db.articleCountsDao()
    private var categoriesDao = db.categoriesDao()
    private var tagsDao = db.tagsDao()
    private var articlePersonalInfosDao = db.articlePersonalInfosDao()

    override suspend fun loadArticlesFromNetwork(last: String?, size: Int): Int {
        val items = network.articles(last, size)
        if (items.isNotEmpty()) insertArticlesToDb(items)
        return items.size
    }

    override suspend fun insertArticlesToDb(articles: List<ArticleRes>) {
        articlesDao.upsert(articles.map {
            it.data.toArticle()
        })
        articleCountsDao.upsert(articles.map {
            it.counts.toArticleCounts()
        })
        // Для того, чтобы собрать все теги в две таблицы
        val refs = articles.map { it.data }
            .fold(mutableListOf<Pair<String, String>>()) { acc, res ->
                acc.also { list -> list.addAll(res.tags.map { res.id to it }) }
            }
        val tags = refs.map { it.second }.distinct().map { Tag(it) }

        val categories = articles.map { it.data.category.toCategory() }
        categoriesDao.insert(categories)
        tagsDao.insert(tags)
        tagsDao.insertRefs(refs.map { ArticleTagXRef(it.first, it.second) })
    }

    override suspend fun toggleBookmark(articleId: String): Boolean {
        return articlePersonalInfosDao.toggleBookmarkOrInsert(articleId)
    }

    override suspend fun toggleLike(articleId: String) {
        articlePersonalInfosDao.toggleLikeOrInsert(articleId)
    }

    override fun findTags(): LiveData<List<String>> = tagsDao.findTags()

    override fun findCategoriesData(): LiveData<List<CategoryData>> =
        categoriesDao.findAllCategoriesData()

    // Возвращаем DataSource.Factory, чтобы результат подходил для пейджинга
    override fun rawQueryArticles(filter: ArticleFilter):
            DataSource.Factory<Int, ArticleItem> =
        articlesDao.findArticlesByRaw(SimpleSQLiteQuery(filter.toQuery()))

    override suspend fun incrementTagUseCount(tag: String) {
        tagsDao.incrementTagUseCount(tag)
    }

    override suspend fun findLastArticleId(): String? =
        articlesDao.findLastArticleId()

    override suspend fun fetchArticleContent(articleId: String) {
        val content = network.loadArticleContent(articleId)
        articleContentsDao.insert(content.toArticleContent())
    }

    override suspend fun removeArticleContent(articleId: String) =
        articleContentsDao.deleteById(articleId)


    // Для тестов
    fun setupTestDao(
        articlesDao: ArticlesDao,
        articleCountsDao: ArticleCountsDao,
        categoriesDao: CategoriesDao,
        tagsDao: TagsDao,
        articlePersonalDao: ArticlePersonalInfosDao,
        articlesContentDao: ArticleContentsDao
    ) {
        this.articlesDao = articlesDao
        this.articleCountsDao = articleCountsDao
        this.categoriesDao = categoriesDao
        this.tagsDao = tagsDao
        this.articlePersonalInfosDao = articlePersonalDao
        this.articleContentsDao = articlesContentDao
    }
}

//============================================================================

class ArticleFilter(
    // Только статьи, содержащие в названии поисковый запрос (если это не хэштэг)
    val search: String? = null,
    // Только статьи с пометкой: в закладках
    val isBookmark: Boolean = false,
    // Только статьи, относящиеся хотя бы к одной из выбранных юзером категорий.
    // Поле содержит список идентификаторов категорий, например - ["1", "4", "9"]
    val categories: List<String> = listOf(),
    // Только статьи, связанные с хэштэгом, набранном в поисковом запросе
    // (запрос начинается с символа #)
    val isHashtag: Boolean = false
) {
    fun toQuery(): String {
        val qb = QueryBuilder()
        with(qb) {
            table("ArticleItem")
            if (search != null && !isHashtag) appendWhere("title LIKE '%$search%' ")
            if (search != null && isHashtag) {
                innerJoin("article_tag_x_ref AS refs", "refs.a_id = id")
                appendWhere("refs.t_id = '$search'")
            }
            if (isBookmark) appendWhere("is_bookmark = 1")
            // Элементы списка, если они не числа, должны обрамляться апострофами,
            // чтобы запрос выглядел так - WHERE first_name IN ('Sarah', 'Jane', 'Heather')
            // Если элементы - числа, то апострофы не нужны, например
            // WHERE employee_id IN (1, 2, 3, 4)
            if (categories.isNotEmpty()) appendWhere(
                "category_id IN (${categories.joinToString(", ")})"
            )
            orderBy("date")
        }
        return qb.build()
    }
}

class QueryBuilder {
    private var table: String? = null
    private var selectColumns: String = "*"
    private var joinTables: String? = null
    private var whereCondition: String? = null
    private var order: String? = null

    fun table(table: String): QueryBuilder {
        this.table = table
        return this
    }

    fun appendWhere(condition: String, logic: String = "AND"): QueryBuilder {
        if (whereCondition.isNullOrEmpty()) whereCondition = "WHERE $condition "
        else whereCondition += "$logic $condition "
        return this
    }

    fun innerJoin(table: String, on: String): QueryBuilder {
        if (joinTables.isNullOrEmpty()) joinTables = "INNER JOIN $table ON $on "
        else joinTables += "INNER JOIN $table ON $on "
        return this
    }

    fun orderBy(column: String, isDesc: Boolean = true): QueryBuilder {
        order = "ORDER BY $column ${if (isDesc) "DESC" else "ASC"}"
        return this
    }

    fun build(): String {
        check(table != null) { "table must be not null" }
        val strBuilder = StringBuilder("SELECT ")
        with(strBuilder) {
            append("$selectColumns ")
            append("FROM $table ")
            if (joinTables != null) append(joinTables)
            if (whereCondition != null) append(whereCondition)
            if (order != null) append(order)
        }
        return strBuilder.toString()
    }
}


//============================================================================

class ArticleDataSource(private val strategy: ArticleStrategy) :
    PositionalDataSource<ArticleItem>() {

    override fun loadInitial(
        params: LoadInitialParams,
        callback: LoadInitialCallback<ArticleItem>
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

    override fun loadRange(params: LoadRangeParams, callback: LoadRangeCallback<ArticleItem>) {
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


sealed class ArticleStrategy() {
    abstract fun getItems(start: Int, size: Int): List<ArticleItem>

    class AllArticles(
        private val itemProvider: (Int, Int) -> List<ArticleItem>
    ) : ArticleStrategy() {
        override fun getItems(start: Int, size: Int) =
            itemProvider(start, size)
    }

    class SearchArticles(
        private val itemProvider: (Int, Int, String) -> List<ArticleItem>,
        private val query: String
    ) : ArticleStrategy() {
        override fun getItems(start: Int, size: Int) =
            itemProvider(start, size, query)
    }

    class BookmarkArticles(
        private val itemProvider: (Int, Int) -> List<ArticleItem>
    ) : ArticleStrategy() {
        override fun getItems(start: Int, size: Int) =
            itemProvider(start, size)
    }

    class SearchBookmark(
        private val itemProvider: (Int, Int, String) -> List<ArticleItem>,
        private val query: String
    ) : ArticleStrategy() {
        override fun getItems(start: Int, size: Int) =
            itemProvider(start, size, query)
    }
}


