package ru.skillbranch.skillarticles.viewmodels.article

import androidx.lifecycle.*
import androidx.paging.LivePagedListBuilder
import androidx.paging.PagedList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ru.skillbranch.skillarticles.data.models.ArticleData
import ru.skillbranch.skillarticles.data.models.ArticlePersonalInfo
import ru.skillbranch.skillarticles.data.models.CommentItemData
import ru.skillbranch.skillarticles.data.repositories.ArticleRepository
import ru.skillbranch.skillarticles.data.repositories.CommentsDataFactory
import ru.skillbranch.skillarticles.data.repositories.MarkdownElement
import ru.skillbranch.skillarticles.data.repositories.clearContent
import ru.skillbranch.skillarticles.extensions.data.toAppSettings
import ru.skillbranch.skillarticles.extensions.data.toArticlePersonalInfo
import ru.skillbranch.skillarticles.extensions.format
import ru.skillbranch.skillarticles.extensions.indexesOf
import ru.skillbranch.skillarticles.viewmodels.base.BaseViewModel
import ru.skillbranch.skillarticles.viewmodels.base.IViewModelState
import ru.skillbranch.skillarticles.viewmodels.base.NavigationCommand
import ru.skillbranch.skillarticles.viewmodels.base.Notify
import java.util.concurrent.Executors

class ArticleViewModel(
    handle: SavedStateHandle,
    private val articleId: String
) : BaseViewModel<ArticleState>(handle, ArticleState()), IArticleViewModel {

    private val repository = ArticleRepository
    private var clearContent: String? = null

    private val listConfig by lazy {
        PagedList.Config.Builder()
            .setEnablePlaceholders(true) // мы знаем общее число комментов к статье
            .setPageSize(5)
            .build()
    }
    private val listData = Transformations.switchMap(getArticleData()) {
        buildPageList(repository.allComments(articleId, it?.commentCount ?: 0))
    }

    // subscribe on mutable data
    init {
        subscribeOnDataSource(getArticleData()) { article, state ->
            article ?: return@subscribeOnDataSource null
            state.copy(
                shareLink = article.shareLink,
                title = article.title,
                category = article.category,
                categoryIcon = article.categoryIcon,
                date = article.date.format(),
                author = article.author
            )
        }
        subscribeOnDataSource(getArticleContent()) { content, state ->
            content ?: return@subscribeOnDataSource null
            state.copy(
                isLoadingContent = false,
                content = content
            )
        }
        subscribeOnDataSource(getArticlePersonalInfo()) { info, state ->
            info ?: return@subscribeOnDataSource null
            state.copy(
                isBookmark = info.isBookmark,
                isLike = info.isLike
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
    }

    // load text from network
    override fun getArticleContent(): LiveData<List<MarkdownElement>?> {
        return repository.loadArticleContent(articleId)
    }

    // load data from db
    override fun getArticleData(): LiveData<ArticleData?> {
        return repository.getArticle(articleId)
    }

    // load data from db
    override fun getArticlePersonalInfo(): LiveData<ArticlePersonalInfo?> {
        return repository.loadArticlePersonalInfo(articleId)
    }

    // personal article info
    override fun handleLike() { // override ?
        val toggleLike = {
            val info = currentState.toArticlePersonalInfo()
            repository.updateArticlePersonalInfo(info.copy(isLike = !info.isLike))
        }
        toggleLike()
        val msg = if (currentState.isLike) Notify.TextMessage("Mark is liked")
        else Notify.ActionMessage(
            "Don`t like it anymore", // snackbar message
            "No, still like it", // action btn on snackbar
            toggleLike // handler, if action btn will be pressed
        )
        notify(msg)
    }

    // personal article info
    override fun handleBookmark() {   // override ?
        val info = currentState.toArticlePersonalInfo()
        repository.updateArticlePersonalInfo(info.copy(isBookmark = !info.isBookmark))
        val msg = if (currentState.isBookmark) "Add to bookmarks"
        else "Remove from bookmarks"
        notify(Notify.TextMessage(msg))
    }

    override fun handleShare() { // override ?
        val msg = "Share is not implemented"
        notify(Notify.ErrorMessage(msg, "OK", null))
    }

    // session state
    override fun handleToggleMenu() {
        updateState { it.copy(isShowMenu = !it.isShowMenu) }
    }

    override fun handleUpTextSize() {
        repository.updateSettings(currentState.toAppSettings().copy(isBigText = true))
    }

    override fun handleDownTextSize() {
        repository.updateSettings(currentState.toAppSettings().copy(isBigText = false))
    }

    // app settings
    override fun handleNightMode() {
        val settings = currentState.toAppSettings()
        val newSettings = settings.copy(isDarkMode = !settings.isDarkMode)
        repository.updateSettings(newSettings)
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
            //            val newPos = when {
//                it.searchPosition.dec() < 0 -> it.searchResults.lastIndex
//                else -> it.searchPosition.dec()
//            }
//            it.copy(searchPosition = newPos)
            it.copy(searchPosition = it.searchPosition.dec())
        }
    }

    fun handleDownResult() {
        updateState {
            //            val newPos = when {
//                it.searchPosition.inc() > it.searchResults.lastIndex -> 0
//                else -> it.searchPosition.inc()
//            }
//            it.copy(searchPosition = newPos)
            it.copy(searchPosition = it.searchPosition.inc())
        }
    }

    fun handleCopyCode() {
        notify(Notify.TextMessage("Code copy to clipboard"))
    }

    fun handleSendComment(comment: String) {
        if (!currentState.isAuth) navigate(NavigationCommand.StartLogin())
        viewModelScope.launch {
            repository.sendComment(articleId, comment, currentState.answerToSlug)
            withContext(Dispatchers.Main) {
                updateState { it.copy(answerTo = null, answerToSlug = null) }
            }
        }
    }

    fun observeList(
        owner: LifecycleOwner,
        onChange: (list: PagedList<CommentItemData>) -> Unit
    ) {
        listData.observe(owner, Observer { onChange(it) })
    }

    private fun buildPageList(
        dataFactory: CommentsDataFactory
    ): LiveData<PagedList<CommentItemData>> {
        return LivePagedListBuilder<String, CommentItemData>(
            dataFactory,
            listConfig
        ).setFetchExecutor(Executors.newSingleThreadExecutor()).build()
    }

    fun handleCommentFocus(hasFocus: Boolean) {
        updateState { it.copy(showBottombar = !hasFocus) }
    }

    fun handleClearComment() {
        updateState { it.copy(answerTo = null, answerToSlug = null) }
    }

    fun handleReplyTo(slug: String, name: String) {
        updateState { it.copy(answerTo = "Reply to $name", answerToSlug = slug) }
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
    // результаты поиска (стартовая и конечная позиции фрагментов)
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
    val answerTo: String? = null, // ответный коммент на коммент
    val answerToSlug: String? = null, // коммент
    val showBottombar: Boolean = true // при написании коммента боттомбар д/б скрыт
) : IViewModelState {
    override fun save(outState: SavedStateHandle) {
        // TODO save comments state
        outState.set("isSearch", isSearch)
        outState.set("searchQuery", searchQuery)
        outState.set("searchResults", searchResults)
        outState.set("searchPosition", searchPosition)
    }

    @Suppress("UNCHECKED_CAST")
    override fun restore(savedState: SavedStateHandle): ArticleState {
        // TODO restore comments state
        return copy(
            isSearch = savedState["isSearch"] ?: false,
            searchQuery = savedState["searchQuery"],
            searchResults = savedState["searchResults"] ?: emptyList(),
            searchPosition = savedState["searchPosition"] ?: 0
        )
    }
}

