package ru.skillbranch.skillarticles.data.repositories

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.paging.*
import androidx.room.withTransaction
import androidx.sqlite.db.SimpleSQLiteQuery
import retrofit2.HttpException
import ru.skillbranch.skillarticles.data.local.AppDb
import ru.skillbranch.skillarticles.data.local.entities.ArticleItem
import ru.skillbranch.skillarticles.data.local.entities.ArticleTagXRef
import ru.skillbranch.skillarticles.data.local.entities.CategoryData
import ru.skillbranch.skillarticles.data.local.entities.Tag
import ru.skillbranch.skillarticles.data.remote.RestService
import ru.skillbranch.skillarticles.data.remote.res.ArticleRes
import ru.skillbranch.skillarticles.extensions.data.toArticle
import ru.skillbranch.skillarticles.extensions.data.toArticleContent
import ru.skillbranch.skillarticles.extensions.data.toArticleCounts
import ru.skillbranch.skillarticles.extensions.data.toCategory
import ru.skillbranch.skillarticles.extensions.logdu
import java.io.IOException
import java.util.*
import javax.inject.Inject

interface IArticlesRepository : IRepository {
    suspend fun loadArticlesFromNetwork(last: String? = null, size: Int = 10): Int
    suspend fun toggleBookmark(articleId: String): Boolean
    fun findTags(): LiveData<List<String>>
    fun findCategoriesData(): LiveData<List<CategoryData>>
    fun getPagingSource(filter: ArticleFilter): PagingSource<Int, ArticleItem>
    suspend fun incrementTagUseCount(tag: String)
    suspend fun findLastArticleId(): String?
    suspend fun fetchArticleContent(articleId: String)
    suspend fun removeArticleContent(articleId: String)
}

class ArticlesRepository @Inject constructor(
    val network: RestService,
    private val appDb: AppDb
) : IArticlesRepository {

    /** Метод загружает из сети N (== size или менее) статей, сохраняет их
     * в локальной БД и возвращает число загруженных статей */
    override suspend fun loadArticlesFromNetwork(last: String?, size: Int): Int {
        val items = network.articles(last, size)
        Log.d("M_S_Paging", "---------- ArticlesRepository network.articles() " +
                    "=> loaded: ${items.size}")

        if (items.isNotEmpty()) {
            // При загрузке с учебного бэкэнда по пути
            // https://skill-articles.skill-branch.ru/api/v1/articles?last={last}&limit={limit}
            // имеется проблема. При параметрах network.articles(null, <положительное целое>)
            // сервер возвращает List<ArticleRes> с неповторяющимися идентификаторами статей.
            // При параметрах network.articles("last_article_id", <отрицательное целое>) сервер
            // возвращает List<ArticleRes>, в котором все статьи совпадают по идентификаторам
            // и контенту с уже загруженными на вызове network.articles(null, <положительное целое>)
            // Чтобы сэмулировать адекватное поведение сервера мы в нашем учебном проекте будем
            // добавлять к идентификатору статей через тире еще 12 случайных чисел перед сохранением
            // в кэш. А при очередной загрузке с сервера мы от идентификатора last_article_id
            // будем отбрасывать эти 13 символов. Если серверу скормить удлиненный идентификатор,
            // он вернет ошибку InternalServerError: Argument passed in must be a single String
            // of 12 bytes or a string of 24 hex characters. Если дать не удлиненный, но неверный,
            // то сервер вернет в теле ответа пустой список.
            /** По сути это костыль, призванный пофиксить серверный баг */
            val alters = mutableListOf<ArticleRes>()
            items.forEach { articleRes ->
                // UUID имеет такую структуру - 123e4567-e89b-12d3-a456-42665d44f0cb
                val suffixedId = articleRes.data.id + UUID.randomUUID().toString().substring(23)
                alters.add(articleRes.copy(data = articleRes.data.copy(id = suffixedId),
                    counts = articleRes.counts.copy(articleId = suffixedId)))
            }

            insertArticlesToDb(alters)
        }
        return items.size
    }

    private suspend fun insertArticlesToDb(articles: List<ArticleRes>) {
        val list = articles.map { it.data.toArticle() }
        logdu("M_S_Paging", "----------\nArticlesRepository resp.toArticle() => articles: $list")
        appDb.articlesDao().upsert(list)

        appDb.articleCountsDao().upsert(articles.map {
            it.counts.toArticleCounts()
        })

        val categories = articles.map { it.data.category.toCategory() }
        appDb.categoriesDao().insert(categories)

        // Для того, чтобы собрать все теги в две таблицы
        val refs = articles.map { it.data }
            .fold(mutableListOf<Pair<String, String>>()) { acc, res ->
                acc.also { list -> list.addAll(res.tags.map { res.id to it }) }
            }
        val tags = refs.map { it.second }.distinct().map { Tag(it) }

        appDb.tagsDao().insert(tags)
        appDb.tagsDao().insertRefs(refs.map { ArticleTagXRef(it.first, it.second) })
    }

    override suspend fun toggleBookmark(articleId: String): Boolean =
        appDb.articlePersonalInfosDao().toggleBookmarkOrInsert(articleId)

    override fun findTags(): LiveData<List<String>> = appDb.tagsDao().findTags()

    override fun findCategoriesData(): LiveData<List<CategoryData>> =
        appDb.categoriesDao().findAllCategoriesData()

    override suspend fun incrementTagUseCount(tag: String) {
        appDb.tagsDao().incrementTagUseCount(tag)
    }

    override suspend fun findLastArticleId(): String? =
        appDb.articlesDao().findLastArticleId()

    override suspend fun fetchArticleContent(articleId: String) {
        // Обрезаем суффикс перед запросом к серверу
        val content = network.loadArticleContent(articleId.substring(0, 24))
        // Перед вставкой контента в кэш восстанавливаем суффикс
        appDb.articleContentsDao().insert(content.copy(articleId = articleId).toArticleContent())
    }

    override suspend fun removeArticleContent(articleId: String) =
        appDb.articleContentsDao().deleteById(articleId)

    override fun getPagingSource(filter: ArticleFilter): PagingSource<Int, ArticleItem> {
        return appDb.articlesDao().pagingSource(
            SimpleSQLiteQuery(filter.toQuery(0, 0))
        )
    }

    @OptIn(ExperimentalPagingApi::class)
    fun getMediator(): RemoteMediator<Int, ArticleItem> = ArticlesMediator(appDb,this)
}

//=================================================
@OptIn(ExperimentalPagingApi::class)
class ArticlesMediator(
    private val appDb: AppDb,
    private val repo: ArticlesRepository
) : RemoteMediator<Int, ArticleItem>() {

    init {
        Log.d("M_S_Paging", "***************** init Mediator")
    }

    override suspend fun initialize(): InitializeAction {
//        val cacheTimeout = TimeUnit.MILLISECONDS.convert(1, TimeUnit.HOURS)
//        return if (System.currentTimeMillis() - repo.lastCacheEvent >= cacheTimeout) {...

        val cacheCount = appDb.articlesDao().articlesDbCount()
        Log.d("M_S_Paging", "***************** Mediator initialize() [cacheCount: $cacheCount]")

        return if (cacheCount > 0) {
            InitializeAction.SKIP_INITIAL_REFRESH
        } else {
            InitializeAction.LAUNCH_INITIAL_REFRESH
        }
    }

    override suspend fun load(loadType: LoadType, state: PagingState<Int, ArticleItem>): MediatorResult {
        var size = 0
        val lastCachedId = when (loadType) {
            LoadType.REFRESH -> {
                size = state.config.initialLoadSize
                null
            }
            LoadType.PREPEND -> return MediatorResult.Success(endOfPaginationReached = true)
            LoadType.APPEND -> {
                size = -state.config.pageSize
                /* Если произошел вызов load() -> append, значит был предыдущий вызов
                 * load(), в котором была непустая загрузка из сети, и следовательно,
                 * lastCachedId не null */
                val last = appDb.articlesDao().lastArticleId()
                // Откидываем последние 13 символов, оставляя первые 24 символа
                "${last?.substring(0, 24)}"
            }
        }
        Log.d("M_S_Paging", "***************** Mediator load() $loadType <before> [lastCachedId: $lastCachedId]")

        var count = 0
        try {
            appDb.withTransaction {
                // Метод loadArticlesFromNetwork: 1). Получает из сети список статейных элементов.
                // 2). Вставляет загруженный из сети список в локальную БД, раскидывая данные по
                // нескольким таблицам. 3). Возвращает количество загруженных айтемов
                count = repo.loadArticlesFromNetwork(lastCachedId, size)
                // Поскольку при вставке в БД меняется таблица ArticleItem, а у нас метод
                // pagingSource аннотирован как @RawQuery(observedEntities = [ArticleItem::class]),
                // то система автоматически пересоздаст PagingSource с новыми данными
            }

            Log.d("M_S_Paging", "***************** Mediator load() $loadType <after> " +
                    "[requested size: $size] [loaded: $count]")

            // Если айтемов нет
            return if (count == 0) {
                // Сообщаем пейджинг-системе об исчерпании сетевого ресурса
                MediatorResult.Success(endOfPaginationReached = true)
            } else {
                // Из сети была получена порция статейных элементов
                MediatorResult.Success(endOfPaginationReached = false)
            }
        }
        catch (e: IOException) {
            Log.d("M_S_Paging", "***************** Mediator load() => catch IO error: $e")
            // todo: уведомить юзера, что сеть недоступна или др.
            return MediatorResult.Error(e)
        }
        catch (e: HttpException) {
            Log.d("M_S_Paging", "***************** Mediator load() => catch Http error: $e")
            // todo: уведомить юзера, что сервер не отвечает или др.
            return MediatorResult.Error(e)
        }
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
    // Если лимит не нужен, то надо задать limit -> 0
    fun toQuery(offset: Int, limit: Int, ordered: Boolean = false): String {
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
            // Look at video - Lecture 12, 02:09:24
            // До лекции 12 категории брались из датахолдера, там идентификаторы
            // категорий были числоподобными строками ("1" или "5" и т.п) и поэтому
            // условие "category_id IN (${categories.joinToString(", ")})" работало.
            // Начиная с лекции 12 категории берутся из сети и идентификаторы уже
            // не числоподобны ("5f27d6cb83218a001d05964f"). Поэтому меняем условие
            if (categories.isNotEmpty()) appendWhere(
                "category_id IN ('${categories.joinToString("', '")}')"
            )
            if (ordered) orderBy("date")
            setSegment(offset, limit)
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
    private var segment: String? = null

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
        order = "ORDER BY $column ${if (isDesc) "DESC" else "ASC"} "
        return this
    }

    fun setSegment(offset: Int, limit: Int): QueryBuilder {
        if (offset >= 0 && limit > 0) segment = "LIMIT $limit OFFSET $offset"
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
            if (segment != null) append(segment)
        }
        return strBuilder.toString()
    }
}
