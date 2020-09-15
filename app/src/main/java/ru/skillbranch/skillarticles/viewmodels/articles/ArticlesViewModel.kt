package ru.skillbranch.skillarticles.viewmodels.articles

import android.util.Log
import androidx.lifecycle.*
import androidx.paging.DataSource
import androidx.paging.LivePagedListBuilder
import androidx.paging.PagedList
import kotlinx.coroutines.launch
import ru.skillbranch.skillarticles.data.local.entities.ArticleItem
import ru.skillbranch.skillarticles.data.local.entities.CategoryData
import ru.skillbranch.skillarticles.data.remote.err.NoNetworkError
import ru.skillbranch.skillarticles.data.repositories.ArticleFilter
import ru.skillbranch.skillarticles.data.repositories.ArticlesRepository
import ru.skillbranch.skillarticles.viewmodels.base.BaseViewModel
import ru.skillbranch.skillarticles.viewmodels.base.IViewModelState
import ru.skillbranch.skillarticles.viewmodels.base.Notify
import java.util.concurrent.Executors

class ArticlesViewModel(handle: SavedStateHandle) :
    BaseViewModel<ArticlesState>(handle, ArticlesState()) {
    private val repository = ArticlesRepository

    // For paging
    private var isLoadingInitial = false
    private var isLoadingAfter = false

    private val listConfig by lazy {
        PagedList.Config.Builder()
            .setEnablePlaceholders(false)
            .setPageSize(10)
            .setPrefetchDistance(30)
            .setInitialLoadSizeHint(50)
            .build()
    }
    private val listData = Transformations.switchMap(mediatorLiveState) {
        val filter = it.toArticleFilter()
        return@switchMap buildPageList(repository.rawQueryArticles(filter))
    }

    fun observeList(
        owner: LifecycleOwner,
        onlyBookmarkedArticles: Boolean = false,
        onChange: (list: PagedList<ArticleItem>) -> Unit
    ) {
        updateState {
            it.copy(onlyBookmarkedArticles = onlyBookmarkedArticles)
        }
        listData.observe(owner, Observer { onChange(it) })
    }

    fun observeTags(owner: LifecycleOwner, onChange: (List<String>) -> Unit) {
        repository.findTags().observe(owner, Observer(onChange))
    }

    fun observeCategories(owner: LifecycleOwner, onChange: (List<CategoryData>) -> Unit) {
        repository.findCategoriesData().observe(owner, Observer(onChange))
    }

    private fun buildPageList(
        dataFactory: DataSource.Factory<Int, ArticleItem>
    ): LiveData<PagedList<ArticleItem>> {
        val builder = LivePagedListBuilder(dataFactory, listConfig)
        // if all articles
        if (isEmptyFilter()) {
            builder.setBoundaryCallback(
                ArticlesBoundaryCallback(
                    ::zeroLoadingHandle,
                    ::itemAtEndHandle
                )
            )
        }
        return builder.setFetchExecutor(Executors.newSingleThreadExecutor()).build()
    }

    private fun isEmptyFilter() = currentState.searchQuery.isNullOrEmpty()
            && !currentState.onlyBookmarkedArticles
            && currentState.selectedCategories.isEmpty()
            && !currentState.isHashtagSearch

    private fun itemAtEndHandle(lastLoadArticle: ArticleItem) {
        Log.d("M_S_ArticlesViewModel", "itemAtEndHandle: ")
        // Если запрос уже отправлен, то не надо дублировать
        if (isLoadingAfter) return
        // Запрос еще не отправлялся -> поднимаем флаг
        else isLoadingAfter = true

        launchSafety(
            complHandler = { isLoadingAfter = false }
        ) {
            // look at video (lecture 11, time code 01:08:10)
            repository.loadArticlesFromNetwork(
                last = lastLoadArticle.id,
                size = listConfig.pageSize
            )
        }

/*      if (items.isNotEmpty()) {
            repository.insertArticlesToDb(items)
            // invalidate data -> create new LiveData<PagedList>
            listData.value?.dataSource?.invalidate()
        }
        withContext(Dispatchers.Main) {
            notify(
                Notify.TextMessage(
                    "Load network articles from " +
                            "${items.firstOrNull()?.data?.id} to ${items.lastOrNull()?.data?.id}"
                )
            )
        }
        */
    }

    private fun zeroLoadingHandle() {
        Log.d("M_S_ArticlesViewModel", "zeroLoadingHandle: ")
        // Если запрос уже отправлен, то не надо дублировать
        if (isLoadingInitial) return
        // Запрос еще не отправлялся -> поднимаем флаг
        else isLoadingInitial = true

        notify(Notify.TextMessage("Storage is empty"))

        launchSafety(
            complHandler = { isLoadingInitial = false }
        ) {
            repository.loadArticlesFromNetwork(
                last = null, size = listConfig.initialLoadSizeHint
            )
        }
/*      if (items.isNotEmpty()) {
            repository.insertArticlesToDb(items)
            // invalidate data -> create new LiveData<PagedList>
            listData.value?.dataSource?.invalidate()
        }*/
    }

    fun handleIsSearch(isSearch: Boolean) {
        updateState {
            it.copy(isSearch = isSearch)
        }
    }

    fun handleSearchQuery(query: String?) {
        query ?: return
        updateState {
            it.copy(
                searchQuery = query,
                isHashtagSearch = query.startsWith("#", true)
            )
        }
    }

    fun handleToggleBookmark(articleId: String) {
        launchSafety(
            errHandler = {
                when (it) {
                    is NoNetworkError -> notify(
                        Notify.TextMessage(
                            "Network not available, failed to fetch article"
                        )
                    )
                    else -> notify(
                        Notify.ErrorMessage(
                            it.message ?: "Something went wrong"
                        )
                    )
                }
            }, complHandler = null
        ) {
            val isBookmarked = repository.toggleBookmark(articleId)
            // Если юзер добавляет статейный айтем на заметку, то загружаем
            // из сети контент статьи и сохраняем его в БД устройства
            if (isBookmarked) repository.fetchArticleContent(articleId)
            // Если юзер снимает закладку, то удаляем контент статьи из БД
            else repository.removeArticleContent(articleId)
        }
    }

    fun handleSuggestion(tag: String) {
        viewModelScope.launch {
            repository.incrementTagUseCount(tag)
        }
    }

    fun applyCategories(selectedCategories: List<String>) {
        updateState { it.copy(selectedCategories = selectedCategories) }
    }

    /** Метод вызывается, когда юзер на экране списка статей делает свайп
     * вниз при крайнем верхнем показе списка (тянет список вниз
     * при том, что экран и так показывает самый верх списка статей) */
    fun refresh() {
        launchSafety {
            val lastArticleId = repository.findLastArticleId()
            // about minus-sign look at video (lecture 11, time code 01:31:13)
            val count = repository.loadArticlesFromNetwork(
                last = lastArticleId,
                size = if (lastArticleId == null) listConfig.initialLoadSizeHint
                else -listConfig.pageSize
            )
            notify(Notify.TextMessage("Load $count new articles"))
        }
    }
}

private fun ArticlesState.toArticleFilter() =
    ArticleFilter(
        search = searchQuery,
        isBookmark = onlyBookmarkedArticles,
        categories = selectedCategories, // e.g. ["1","5","7"]
        isHashtag = isHashtagSearch
    )


//============================================================================

data class ArticlesState(
    val isSearch: Boolean = false,
    val searchQuery: String? = null,
    /** статьи в процессе загрузки из сети? */
    val isLoading: Boolean = true,
    val onlyBookmarkedArticles: Boolean = false,
    val selectedCategories: List<String> = emptyList(), // e.g. ["1","5","7"]
    val isHashtagSearch: Boolean = false
) : IViewModelState

//============================================================================

class ArticlesBoundaryCallback(
    private val zeroLoadingHandle: () -> Unit,
    private val itemAtEndHandle: (ArticleItem) -> Unit
) : PagedList.BoundaryCallback<ArticleItem>() {

    override fun onZeroItemsLoaded() {
        // Storage is empty
        zeroLoadingHandle()
    }

    override fun onItemAtEndLoaded(itemAtEnd: ArticleItem) {
        // Need load more items when user scroll down
        itemAtEndHandle(itemAtEnd)
    }
}


