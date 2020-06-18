package ru.skillbranch.skillarticles.viewmodels.bookmarks

import androidx.lifecycle.*
import androidx.paging.LivePagedListBuilder
import androidx.paging.PagedList
import ru.skillbranch.skillarticles.data.models.ArticleItemData
import ru.skillbranch.skillarticles.data.repositories.ArticlesDataFactory
import ru.skillbranch.skillarticles.data.repositories.ArticlesRepository
import ru.skillbranch.skillarticles.viewmodels.articles.ArticlesState
import ru.skillbranch.skillarticles.viewmodels.base.BaseViewModel
import java.util.concurrent.Executors

class BookmarksViewModel(handle: SavedStateHandle) :
    BaseViewModel<ArticlesState>(handle, ArticlesState()) {

    private val repository = ArticlesRepository
    private val listConfig by lazy {
        PagedList.Config.Builder()
            .setEnablePlaceholders(false)
            .setPageSize(10)
            .setPrefetchDistance(15)
            .setInitialLoadSizeHint(30)
            .build()
    }
    private val listData = Transformations.switchMap(state) {
        when {
            it.isSearch && !it.searchQuery.isNullOrBlank() ->
                buildPageList(repository.searchBookmark(it.searchQuery))
            else -> buildPageList(repository.bookmarkArticles())
        }
    }

    fun observeList(
        owner: LifecycleOwner,
        onChange: (list: PagedList<ArticleItemData>) -> Unit
    ) {
        listData.observe(owner, Observer { onChange(it) })
    }

    private fun buildPageList(
        dataFactory: ArticlesDataFactory
    ): LiveData<PagedList<ArticleItemData>> {
        val builder = LivePagedListBuilder(dataFactory, listConfig)
        return builder.setFetchExecutor(Executors.newSingleThreadExecutor()).build()
    }

    fun handleIsSearch(isSearch: Boolean) {
        updateState {
            it.copy(isSearch = isSearch)
        }
    }

    fun handleSearchQuery(query: String?) {
        query ?: return
        updateState {
            it.copy(searchQuery = query)
        }
    }

    fun handleToggleBookmark(articleId: String, isChecked: Boolean) {
        repository.updateBookmark(articleId, isChecked)
        listData.value?.dataSource?.invalidate()
    }
}
