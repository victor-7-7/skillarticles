package ru.skillbranch.skillarticles.ui.articles

import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.android.synthetic.main.fragment_articles.*
import ru.skillbranch.skillarticles.R
import ru.skillbranch.skillarticles.ui.base.BaseFragment
import ru.skillbranch.skillarticles.ui.base.Binding
import ru.skillbranch.skillarticles.ui.base.MenuItemHolder
import ru.skillbranch.skillarticles.ui.base.ToolbarBuilder
import ru.skillbranch.skillarticles.ui.delegates.RenderProp
import ru.skillbranch.skillarticles.viewmodels.articles.ArticlesState
import ru.skillbranch.skillarticles.viewmodels.articles.ArticlesViewModel
import ru.skillbranch.skillarticles.viewmodels.base.IViewModelState
import ru.skillbranch.skillarticles.viewmodels.base.NavigationCommand

class ArticlesFragment : BaseFragment<ArticlesViewModel>() {

    override val viewModel: ArticlesViewModel by viewModels()
    override val layout = R.layout.fragment_articles
    override val binding: ArticlesBinding by lazy { ArticlesBinding() }
    override val prepareToolbar: (ToolbarBuilder.() -> Unit)? = {
        addMenuItem(
            MenuItemHolder(
                "Search",
                R.id.action_search,
                R.drawable.ic_search_black_24dp,
                R.layout.search_view_layout
            )
        )
    }

    private val articlesAdapter = ArticlesAdapter({ item ->
        Log.d(
            "M_ArticlesFragment", "click on bookmark: ${item.id} " +
                    "isBookmark(before toggle)=${item.isBookmark}"
        )
        viewModel.handleToggleBookmark(item.id, !item.isBookmark)
        Log.d(
            "M_ArticlesFragment", "click on bookmark: ${item.id} " +
                    "isBookmark(after toggle)=${item.isBookmark}"
        )
    }) { item ->
        Log.d("M_ArticlesFragment", "click on article: ${item.id}")
        val action = ArticlesFragmentDirections.actionNavArticlesToPageArticle(
            item.id, item.author, item.authorAvatar, item.category,
            item.categoryIcon, item.date, item.poster, item.title
        )
        viewModel.navigate(NavigationCommand.To(action.actionId, action.arguments))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        super.onPrepareOptionsMenu(menu)
        val menuItem = menu.findItem(R.id.action_search)
        val searchView = menuItem.actionView as SearchView
        if (binding.isSearch) {
            menuItem.expandActionView()
            searchView.setQuery(binding.searchQuery, false)
            if (binding.isFocusedSearch) searchView.requestFocus()
            else searchView.clearFocus()
        }
        menuItem.setOnActionExpandListener(
            object : MenuItem.OnActionExpandListener {
                override fun onMenuItemActionExpand(item: MenuItem?): Boolean {
                    viewModel.handleIsSearch(true)
                    return true
                }

                override fun onMenuItemActionCollapse(item: MenuItem?): Boolean {
                    viewModel.handleIsSearch(false)
                    return true
                }
            })
        searchView.setOnQueryTextListener(
            object : SearchView.OnQueryTextListener {
                override fun onQueryTextSubmit(query: String): Boolean {
                    viewModel.handleSearchQuery(query)
                    return true
                }

                override fun onQueryTextChange(newText: String): Boolean {
                    viewModel.handleSearchQuery(newText)
                    return true
                }
            })
        searchView.setOnCloseListener {
            viewModel.handleIsSearch(false)
            true
        }
    }

    override fun setupViews() {
        with(rv_articles) {
            layoutManager = LinearLayoutManager(context)
            adapter = articlesAdapter
            addItemDecoration(DividerItemDecoration(context, LinearLayoutManager.VERTICAL))
        }
        viewModel.observeList(viewLifecycleOwner) {
            articlesAdapter.submitList(it)
        }
    }

    inner class ArticlesBinding : Binding() {
//        private var articles: List<ArticleItemData>
//                by RenderProp(emptyList<ArticleItemData>()) {
//                    articlesAdapter.submitList(it)
//                }

        var isFocusedSearch: Boolean = false
        var searchQuery: String? = null
        var isSearch: Boolean = false
        var isLoading: Boolean by RenderProp(true) {
            // https://blog.mindorks.com/using-shimmer-effect-placeholder-in-android
            // TODO show shimmer on rv_list
        }

        override fun bind(data: IViewModelState) {
            data as ArticlesState
//            articles = data.articles
            searchQuery = data.searchQuery
            isSearch = data.isSearch
            isLoading = data.isLoading
        }
        // коммент на 47:20. Сохранение UI-элементов реализовать самим.
        // Пишем значения в бандл. Для этого надо переопределить всего
        // два метода - saveUi и restoreUi. И то же самое сделать с вьюмоделью.
        // TODO save UI
    }
}