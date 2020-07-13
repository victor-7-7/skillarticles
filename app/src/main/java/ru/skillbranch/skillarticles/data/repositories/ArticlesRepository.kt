package ru.skillbranch.skillarticles.data.repositories

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.paging.DataSource
import androidx.paging.PositionalDataSource
import androidx.sqlite.db.SimpleSQLiteQuery
import ru.skillbranch.skillarticles.data.NetworkDataHolder
import ru.skillbranch.skillarticles.data.local.DbManager.db
import ru.skillbranch.skillarticles.data.local.dao.*
import ru.skillbranch.skillarticles.data.local.entities.ArticleItem
import ru.skillbranch.skillarticles.data.local.entities.ArticleTagXRef
import ru.skillbranch.skillarticles.data.local.entities.CategoryData
import ru.skillbranch.skillarticles.data.local.entities.Tag
import ru.skillbranch.skillarticles.data.remote.res.ArticleRes
import ru.skillbranch.skillarticles.extensions.data.toArticle
import ru.skillbranch.skillarticles.extensions.data.toArticleCounts

interface IArticlesRepository {
    fun loadArticlesFromNetwork(start: Int = 0, size: Int): List<ArticleRes>
    fun insertArticlesToDb(articles: List<ArticleRes>)
    fun toggleBookmark(articleId: String)
    fun toggleLike(articleId: String)
    fun findTags(): LiveData<List<String>>
    fun findCategoriesData(): LiveData<List<CategoryData>>
    fun rawQueryArticles(filter: ArticleFilter): DataSource.Factory<Int, ArticleItem>
    fun incrementTagUseCount(tag: String)
}

object ArticlesRepository : IArticlesRepository {
    private val network = NetworkDataHolder
    private var articlesDao = db.articlesDao()
    private var articleCountsDao = db.articleCountsDao()
    private var categoriesDao = db.categoriesDao()
    private var tagsDao = db.tagsDao()
    private var articlePersonalInfosDao = db.articlePersonalInfosDao()

    override fun loadArticlesFromNetwork(start: Int, size: Int): List<ArticleRes> =
        network.findArticlesItem(start, size)

    override fun insertArticlesToDb(articles: List<ArticleRes>) {
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

        val categories = articles.map { it.data.category }
        categoriesDao.insert(categories)
        tagsDao.insert(tags)
        tagsDao.insertRefs(refs.map { ArticleTagXRef(it.first, it.second) })
    }

    override fun toggleBookmark(articleId: String) {
        articlePersonalInfosDao.toggleBookmarkOrInsert(articleId)
    }

    override fun toggleLike(articleId: String) {
        articlePersonalInfosDao.toggleLikeOrInsert(articleId)
    }

    override fun findTags(): LiveData<List<String>> = tagsDao.findTags()

    override fun findCategoriesData(): LiveData<List<CategoryData>> =
        categoriesDao.findAllCategoriesData()

    // Возвращаем DataSource.Factory, чтобы результат подходил для пейджинга
    override fun rawQueryArticles(filter: ArticleFilter):
            DataSource.Factory<Int, ArticleItem> =
        articlesDao.findArticlesByRaw(SimpleSQLiteQuery(filter.toQuery()))

    override fun incrementTagUseCount(tag: String) {
        tagsDao.incrementTagUseCount(tag)
    }

    // Для тестов
    fun setupTestDao(
        articlesDao: ArticlesDao,
        articleCountsDao: ArticleCountsDao,
        categoriesDao: CategoriesDao,
        tagsDao: TagsDao,
        articlePersonalDao: ArticlePersonalInfosDao
    ) {
        this.articlesDao = articlesDao
        this.articleCountsDao = articleCountsDao
        this.categoriesDao = categoriesDao
        this.tagsDao = tagsDao
        this.articlePersonalInfosDao = articlePersonalDao
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

//============================================================================

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




