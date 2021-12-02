package ru.skillbranch.skillarticles.viewmodels.article

import android.util.Log
import androidx.lifecycle.*
import androidx.paging.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import ru.skillbranch.skillarticles.data.local.NetworkDataHolder
import ru.skillbranch.skillarticles.data.remote.err.ApiError
import ru.skillbranch.skillarticles.data.remote.res.CommentRes
import ru.skillbranch.skillarticles.data.repositories.*
import ru.skillbranch.skillarticles.extensions.data.toAppSettings
import ru.skillbranch.skillarticles.extensions.indexesOf
import ru.skillbranch.skillarticles.extensions.shortFormat
import ru.skillbranch.skillarticles.viewmodels.base.BaseViewModel
import ru.skillbranch.skillarticles.viewmodels.base.IViewModelState
import ru.skillbranch.skillarticles.viewmodels.base.NavigationCommand
import ru.skillbranch.skillarticles.viewmodels.base.Notify
import java.util.concurrent.Executors
import javax.inject.Inject

@HiltViewModel
class ArticleViewModel @Inject constructor(
    handle: SavedStateHandle,
    private val repository: ArticleRepository
) : BaseViewModel<ArticleState>(handle, ArticleState()), IArticleViewModel {
    /**
     * Недостаток хилта (Hilt) - он не позволяет передать в конструктор
     * ViewModel параметр articleId: String и т.п. Мы можем передать
     * только SavedStateHandle через @Assisted и репозиторий,
     * который можно заинжектить при помощи даггера. Все остальное
     * приходится извлекать из handle, а это фееричный антипаттерн
     * (lecture 14, t.c. 03:01:55)
     * */
    // bundle key default args (safe args from navigation)
    private val articleId: String = handle["article_id"]!!
    private var clearContent: String? = null

    /*private val listConfig by lazy {
        PagedList.Config.Builder()
            .setEnablePlaceholders(true) // мы знаем общее число комментов к статье
            .setPageSize(7)
            .build()
    }

    private val commentsListData = Transformations.switchMap(
        repository.findArticleCommentCount(articleId)
    ) {
        // Текущее количество комментариев у статьи известно заранее
        buildPageList(repository.loadAllComments(articleId, it, ::commentLoadErrorHandler))
    }*/

    private lateinit var dataSource: CommentsDataSource2
    private var commCount = 0
    private var firstTrigger = true

    companion object {
        const val NETWORK_PAGE_SIZE = 10
    }

    // Подписка на commentsPager выполняется в ArticleFragment (в методе setupViews)
    val commentsPager: LiveData<PagingData<CommentRes>> =
        Pager(
            config = PagingConfig(
                // Should be several times the number of visible items onscreen
                pageSize = NETWORK_PAGE_SIZE,
                prefetchDistance = 2 * NETWORK_PAGE_SIZE,
                enablePlaceholders = true, // default -> true
                initialLoadSize = 3 * NETWORK_PAGE_SIZE, // default (3 * pageSize)
                /*maxSize = 55,*/
                // Граничное число непрогруженных айтемов. Скролл в пределах
                // этого порога грузит страницы инкрементно. Скролл, убежавший за
                // этот порог, заставляет движок прыгнуть к прогрузке страницы,
                // которая отвечает обновленной позиции скролла. Чтобы это заработало
                // надо дополнительно выставить в true свойство PagingSource.jumpingSupported
                /*jumpThreshold = 2 * 12 // default (2 * pageSize)*/
                // Минус прыжков в том, что ранее загруженные и закешированные айтемы
                // инвалидируются (отбрасываются) и надо будет грузить их заново
            ),
            // С какого айтема следует начинать постраничную загрузку списка
            initialKey = 0,
            pagingSourceFactory = {
                Log.d("M_S_Paging", "ArticleViewModel pagingSourceFactory " +
                        "[count: $commCount]")
                repository.makeCommentsDataSource(articleId, commCount)
                    .also { dataSource = it }
            }
        )
            .liveData
            .cachedIn(viewModelScope)

    // subscribe on mutable data
    init {
        subscribeOnDataSource(repository.findArticle(articleId)) { article, state ->
            if (article.content == null) fetchContent()

            state.copy(
                shareLink = article.shareLink,
                title = article.title,
                category = article.category.title,
                categoryIcon = article.category.icon,
                date = article.date.shortFormat(),
                author = article.author,
                isBookmark = article.isBookmark,
                isLike = article.isLike,
                content = article.content ?: emptyList(),
                isLoadingContent = article.content == null,
                source = article.source,
                tags = article.tags
            )
        }
        subscribeOnDataSource(repository.getAppSettings()) { settings, state ->
            state.copy(
                isDarkMode = settings.isDarkMode,
                isBigText = settings.isBigText
            )
        }
        subscribeOnDataSource(repository.isAuth()) { isAuth, state ->
            state.copy(isAuth = isAuth)
        }
        subscribeOnDataSource(repository.findArticleCommentCount(articleId)) { count, state ->
            Log.d("M_S_Paging", "ArticleViewModel subscribeOnDataSource " +
                    "[commCount: $count]")
            commCount = count
            if (firstTrigger) {
                // При инициализации модели статьи нам ни к чему инвалидация
                // источника комментов. Просто сбрасываем флаг
                firstTrigger = false
            } else {
                // В результате инвалидации будет вызван PagingSource-метод getRefreshKey
                dataSource.invalidate()
            }
            state
        }

        // Для тестирования комментов
        repository.updateCommentsDH()
    }


    /** Метод вызывается, когда юзер на экране статьи делает свайп
     * вниз при крайнем верхнем показе контента (тянет контент вниз
     * при том, что статья и так показывает самый верх контента) */
    fun swipeRefresh() {
        // Параллельно запускаем фоновые задачи
        launchSafety {
            // Загружаем контент статьи с сервера
            launch { repository.fetchArticleContent(articleId) }
            // Синхронизируем локальные метрики статьи с серверными
            launch { repository.refreshCommentsCount(articleId) }
        }
    }

    private fun commentLoadErrorHandler(throwable: Throwable) {
        // todo: handle errors
    }

    /** Метод вызывается, если контент статьи равен null (не загружен) */
    private fun fetchContent() {
        launchSafety { repository.fetchArticleContent(articleId) }
    }

    // personal article info
    override fun handleLike() {
        val msg = if (!currentState.isLike)
            Notify.TextMessage("Mark is liked")
        else Notify.ActionMessage(
            "Don`t like it anymore", // snackbar message
            "No, still like it" // action btn on snackbar
        ) {
            handleLike()
        } // handler, if action btn will be pressed

        launchSafety(null, { notify(msg) }) {
            // Фиксируем в локальной БД (в таблице article_personal_infos)
            // лайк/дизлайк (юзером данного девайса) этой статьи
            val isLiked = repository.toggleLike(articleId)
            try {
                // Фиксируем на сервере (если юзер авторизован и сеть/сервер
                // доступны), а также в локальной БД (в таблице article_counts)
                // увеличение/уменьшение на 1 всеобщего числа лайков у статьи
                if (isLiked) repository.incrementLike(articleId)
                else repository.decrementLike(articleId)
            } catch (e: ApiError.BadRequest) {
                // Если сервер говорит, что действий не требуется, то и ладно
                return@launchSafety
                // Остальные ошибки, не перехваченные этим кэтчером (не являющиеся
                // ApiError.BadRequest) будут обработаны дефолтным хендлером
            }
        }
    }

    // personal article info
    override fun handleBookmark() {
        val msg = if (!currentState.isBookmark) "Add to bookmarks"
        else "Remove from bookmarks"
        launchSafety(null, {
            notify(Notify.TextMessage(msg))
        }) {
            // Сохраняем клик юзера по закладке в локальной БД
            val bookmarked = repository.toggleBookmark(articleId)
            try {
                // Фиксируем на сервере (если юзер авторизован и сеть/сервер
                // доступны), что данный конкретный юзер добавил в закладки
                // или убрал из закладок конкретную статью
                if (bookmarked) repository.addBookmark(articleId)
                else repository.removeBookmark(articleId)
            } catch (e: ApiError.BadRequest) {
                // Если сервер говорит, что действий не требуется, то и ладно
                return@launchSafety
                // Остальные ошибки, не перехваченные этим кэтчером (не являющиеся
                // ApiError.BadRequest) будут обработаны дефолтным хендлером
            }
        }
    }

    override fun handleShare() {
        val msg = "Share is not implemented"
        notify(Notify.ErrorMessage(msg, "OK", errHandler = null))
    }

    // session state
    override fun handleToggleMenu() {
        updateState { it.copy(isShowMenu = !it.isShowMenu) }
    }

    override fun handleUpTextSize() {
        repository.updateSettings(currentState.toAppSettings().copy(isBigText = true))
        updateState { it.copy(isBigText = true) }
    }

    override fun handleDownTextSize() {
        repository.updateSettings(currentState.toAppSettings().copy(isBigText = false))
        updateState { it.copy(isBigText = false) }
    }

    // app settings
    override fun handleNightMode() {
        val settings = currentState.toAppSettings()
        val newSettings = settings.copy(isDarkMode = !settings.isDarkMode)
        repository.updateSettings(newSettings)
        updateState { it.copy(isDarkMode = newSettings.isDarkMode) }
    }

    override fun handleIsSearch(isSearch: Boolean) {
        updateState { it.copy(isSearch = isSearch, isShowMenu = false, searchPosition = 0) }
    }

    override fun handleSearchQuery(query: String?) {
        query ?: return
        if (clearContent == null && currentState.content.isNotEmpty())
            clearContent = currentState.content.clearContent()

        val results = clearContent
            .indexesOf(query)
            .map {
                it to it + query.length
            }
        updateState {
            it.copy(searchQuery = query, searchResults = results, searchPosition = 0)
        }
    }

    fun handleUpResult() {
        updateState {
            val newPos = when {
                it.searchPosition.dec() < 0 -> it.searchResults.lastIndex
                else -> it.searchPosition.dec()
            }
            it.copy(searchPosition = newPos)

//            it.copy(searchPosition = it.searchPosition.dec())
        }
    }

    fun handleDownResult() {
        updateState {
            val newPos = when {
                it.searchPosition.inc() > it.searchResults.lastIndex -> 0
                else -> it.searchPosition.inc()
            }
            it.copy(searchPosition = newPos)

//            it.copy(searchPosition = it.searchPosition.inc())
        }
    }

    fun handleCopyCode() {
        notify(Notify.TextMessage("Code copy to clipboard"))
    }

    fun handleSendComment(comment: String?) {
        if (comment.isNullOrBlank()) {
            notify(Notify.TextMessage("Comment must not be empty"))
            return
        }
        updateState {
            it.copy(commentText = comment)
        }
        if (!currentState.isAuth) {
            navigate(NavigationCommand.StartLogin())
        } else launchSafety(null, {
            updateState {
                it.copy(
                    answerTo = null,
                    answerToMessageId = null,
                    commentText = null
                )
            }
        }) {
            // Для сетевого запроса инвалидация dataSource будет сделана
            // в коллбэке обзервера за commentsCount
            repository.sendMessage(
                articleId,
                currentState.commentText!!,
                currentState.answerToMessageId
            )
            //----------------------------------
            // Для тестового запроса к NetworkDataHolder, инвалидируем dataSource
            dataSource.invalidate()
        }
    }

    /*fun observeCommentList(
        owner: LifecycleOwner,
        onChange: (list: PagedList<CommentRes>) -> Unit
    ) {
        commentsListData.observe(owner, Observer { onChange(it) })
    }

    private fun buildPageList(
        dataFactory: CommentsDataFactory
    ): LiveData<PagedList<CommentRes>> {
        return LivePagedListBuilder(
            dataFactory,
            listConfig
        ).setFetchExecutor(Executors.newSingleThreadExecutor()).build()
    }*/

    fun handleCommentFocus(hasFocus: Boolean) {
        updateState { it.copy(showBottombar = !hasFocus) }
    }

    fun handleClearComment() {
        updateState {
            it.copy(
                answerTo = null,
                answerToMessageId = null
            )
        }
    }

    fun handleReplyTo(messageId: String, name: String) {
        updateState { it.copy(answerTo = "Reply to $name", answerToMessageId = messageId) }
    }

    fun saveComment(comment: String) {
        updateState { it.copy(commentText = comment) }
    }

    fun initCommentCount(count: Int) {
        Log.d("M_S_Paging", "ArticleViewModel initCommentCount [count: $count]")
        commCount = count
//        updateState { it.copy(commentsCount = count) }
    }
}


//============================================================================

data class ArticleState(
    val isAuth: Boolean = false, // пользователь авторизован
    val isLoadingContent: Boolean = true, // контент загружается
    val isLoadingReviews: Boolean = true, // отзывы загружаются
    val isLike: Boolean = false, // отмечено как Like
    val isBookmark: Boolean = false, // в закладках
    val isShowMenu: Boolean = false, // отображается меню
    val isBigText: Boolean = false, // шрифт увеличен
    val isDarkMode: Boolean = false, // темный режим
    val isSearch: Boolean = false, // режим поиска
    val searchQuery: String? = null, // поисковый запрос
    // результаты поиска (список стартовых/конечных позиций фрагментов)
    val searchResults: List<Pair<Int, Int>> = emptyList(),
    // текущая индексная (zero-based) позиция найденного результата
    val searchPosition: Int = 0,
    val shareLink: String? = null, // ссылка Share
    val title: String? = null, // заголовок статьи
    val category: String? = null, // категория
    val categoryIcon: Any? = null, // иконка категории ::signature is verified
    val date: String? = null, // дата публикации
    val author: Any? = null, // автор статьи
    val poster: String? = null, // обложка статьи
    val content: List<MarkdownElement> = emptyList(), // контент
    val commentsCount: Int = 0, // число комментариев к статье
    /** Подсказка, появляющаяся в редактируемом поле комментария. Если
     * юзер отвечает на комментарий юзера John, то подсказка будет вида -
     * Reply to John. Если юзер комментирует саму статью, то подсказка
     * будет вида - Comment */
    val answerTo: String? = null,
    /** Текстовый ключ (slug) коммента, в ответ на который в данный момент
     * (ArticleState в момент редактирования комментария) юзер набирает
     * свой комментарий. Если юзер комментирует саму статью, то данное
     * значение - null */
    val answerToMessageId: String? = null,
    val showBottombar: Boolean = true, // при написании коммента боттомбар д/б скрыт
    val commentText: String? = null,
    val source: String? = null,
    val tags: List<String> = emptyList()
) : IViewModelState {
    override fun save(outState: SavedStateHandle) {
        outState.set("isSearch", isSearch)
        outState.set("searchQuery", searchQuery)
        outState.set("searchResults", searchResults)
        outState.set("searchPosition", searchPosition)
    }

    @Suppress("UNCHECKED_CAST")
    override fun restore(savedState: SavedStateHandle): ArticleState {
        return copy(
            isSearch = savedState["isSearch"] ?: false,
            searchQuery = savedState["searchQuery"],
            searchResults = savedState["searchResults"] ?: emptyList(),
            searchPosition = savedState["searchPosition"] ?: 0
        )
    }
}

