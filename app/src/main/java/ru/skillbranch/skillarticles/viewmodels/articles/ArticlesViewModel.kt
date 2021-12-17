package ru.skillbranch.skillarticles.viewmodels.articles

import android.util.Log
import androidx.lifecycle.*
import androidx.paging.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import ru.skillbranch.skillarticles.data.local.entities.ArticleItem
import ru.skillbranch.skillarticles.data.local.entities.CategoryData
import ru.skillbranch.skillarticles.data.remote.err.NoNetworkError
import ru.skillbranch.skillarticles.data.repositories.ArticleFilter
import ru.skillbranch.skillarticles.data.repositories.ArticlesRepository
import ru.skillbranch.skillarticles.viewmodels.base.BaseViewModel
import ru.skillbranch.skillarticles.viewmodels.base.IViewModelState
import ru.skillbranch.skillarticles.viewmodels.base.Notify
import javax.inject.Inject

@HiltViewModel
class ArticlesViewModel @Inject constructor(
    handle: SavedStateHandle,
    private val repo: ArticlesRepository
) : BaseViewModel<ArticlesState>(handle, ArticlesState()) {

    companion object {
        const val ARTICLES_PAGE_SIZE = 10
    }

    private lateinit var dataSource: PagingSource<Int, ArticleItem>

    // Подписка на articlesPager выполняется в ArticlesFragment
    @OptIn(ExperimentalPagingApi::class)
    val articlesPager: LiveData<PagingData<ArticleItem>> =
        Pager(
            config = PagingConfig(
                // Should be several times the number of visible items onscreen
                pageSize = ARTICLES_PAGE_SIZE,
                prefetchDistance = ARTICLES_PAGE_SIZE,
                initialLoadSize = 2 * ARTICLES_PAGE_SIZE
            ),
            pagingSourceFactory = {
                Log.d("M_S_Paging", "ArticlesViewModel new pagingSource into Factory")
                repo.getPagingSource(currentState.toArticleFilter()).also { dataSource = it }
            },
            remoteMediator = repo.getMediator()
        )
            .liveData
            .cachedIn(viewModelScope)

    fun observeArticles(
        owner: LifecycleOwner,
        onlyBookmarkedArticles: Boolean = false,
        onChange: (list: PagingData<ArticleItem>) -> Unit
    ) {
        if (currentState.onlyBookmarkedArticles != onlyBookmarkedArticles) {
            updateState {
                it.copy(onlyBookmarkedArticles = onlyBookmarkedArticles)
            }
            dataSource.invalidate()
        }

        articlesPager.observe(owner, Observer { onChange(it) })
    }

    fun observeTags(owner: LifecycleOwner, onChange: (List<String>) -> Unit) {
        repo.findTags().observe(owner, Observer(onChange))
    }

    fun observeCategories(owner: LifecycleOwner, onChange: (List<CategoryData>) -> Unit) {
        repo.findCategoriesData().observe(owner, Observer(onChange))
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
        dataSource.invalidate()
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
            val isBookmarked = repo.toggleBookmark(articleId)
            // Если юзер добавляет статейный айтем на заметку, то загружаем
            // из сети контент статьи и сохраняем его в БД устройства
            if (isBookmarked) repo.fetchArticleContent(articleId)
            // Если юзер снимает закладку, то удаляем контент статьи из БД
            else repo.removeArticleContent(articleId)
        }
    }

    fun handleSuggestion(tag: String) {
        viewModelScope.launch {
            repo.incrementTagUseCount(tag)
        }
    }

    fun applyCategories(selectedCategories: List<String>) {
        updateState { it.copy(selectedCategories = selectedCategories) }
        dataSource.invalidate()
    }

    /** Метод вызывается, когда юзер на экране списка статей делает свайп
     * вниз при крайнем верхнем показе списка (тянет список вниз
     * при том, что экран и так показывает самый верх списка статей) */
    fun refresh() {
        launchSafety {
            /*todo: load updated list */
            notify(Notify.TextMessage("Swipe to refresh"))
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
    /** Статьи в процессе загрузки из сети ? */
    val isLoading: Boolean = true,
    /** Значение для этого свойства берется из navArgs в ArticlesFragment,
     * передается во вьюмодель (через метод) и апдейтит данное свойство. После этого
     * создается PagingSource с передачей в него правильного фильтра. Когда юзер
     * тапает в боттомбаре по иконке nav_bookmarks, выполняется навигация к
     * нав-"фрагменту" с id -> nav_bookmarks. Он отличается от нав-"фрагмента" с
     * id -> nav_articles только нав-аргументом onlyBookmarkedArticles (true вместо
     * false). Оба нав-"фрагмента" ссылаются на фрагмент ArticlesFragment */
    val onlyBookmarkedArticles: Boolean = false,
    val selectedCategories: List<String> = emptyList(), // e.g. ["1","5","7"]
    val isHashtagSearch: Boolean = false
) : IViewModelState

