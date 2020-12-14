package ru.skillbranch.skillarticles.ui.articles

import android.database.Cursor
import android.database.MatrixCursor
import android.os.Bundle
import android.provider.BaseColumns
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.AutoCompleteTextView
import android.widget.CursorAdapter
import androidx.appcompat.widget.SearchView
import androidx.core.view.isVisible
import androidx.cursoradapter.widget.SimpleCursorAdapter
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.android.synthetic.main.activity_root.*
import kotlinx.android.synthetic.main.fragment_articles.*
import kotlinx.android.synthetic.main.search_view_layout.view.*
import ru.skillbranch.skillarticles.R
import ru.skillbranch.skillarticles.data.local.entities.CategoryData
import ru.skillbranch.skillarticles.ui.base.BaseFragment
import ru.skillbranch.skillarticles.ui.base.Binding
import ru.skillbranch.skillarticles.ui.base.MenuItemHolder
import ru.skillbranch.skillarticles.ui.base.ToolbarBuilder
import ru.skillbranch.skillarticles.ui.custom.delegates.RenderProp
import ru.skillbranch.skillarticles.viewmodels.articles.ArticlesState
import ru.skillbranch.skillarticles.viewmodels.articles.ArticlesViewModel
import ru.skillbranch.skillarticles.viewmodels.base.IViewModelState
import ru.skillbranch.skillarticles.viewmodels.base.Loading
import ru.skillbranch.skillarticles.viewmodels.base.NavigationCommand

class ArticlesFragment : BaseFragment<ArticlesViewModel>() {

    override val viewModel: ArticlesViewModel by activityViewModels()
    override val layout = R.layout.fragment_articles
    override val binding: ArticlesBinding by lazy { ArticlesBinding() }
    private val args: ArticlesFragmentArgs by navArgs()
    private lateinit var suggestionsAdapter: SimpleCursorAdapter

    override val prepareToolbar: ToolbarBuilder.() -> Unit = {
        addMenuItem(
            MenuItemHolder(
                "Search",
                R.id.action_search,
                R.drawable.ic_search_black_24dp,
                R.layout.search_view_layout
            )
        )
        addMenuItem(
            MenuItemHolder(
                "Filter",
                R.id.action_filter,
                R.drawable.ic_round_filter_list_24,
                actionViewLayout = null
            ) {
                val action = ArticlesFragmentDirections.actionChooseCategory(
                    binding.selectedCategories.toTypedArray(), // e.g. ["1","5","7"]
                    binding.categories.toTypedArray()
                )
                viewModel.navigate(
                    NavigationCommand
                        .To(action.actionId, action.arguments)
                )
            }
        )
    }

    private val articlesAdapter = ArticlesAdapter { item, isToggleBookmark ->
        if (isToggleBookmark) viewModel.handleToggleBookmark(item.id)
        else {
            Log.d("M_S_ArticlesFragment", "click on article: ${item.id}")
            val action = ArticlesFragmentDirections.actionToPageArticle(
                item.id, item.author, item.authorAvatar ?: "", item.category,
                item.categoryIcon, item.date, item.poster, item.title
            )
            viewModel.navigate(NavigationCommand.To(action.actionId, action.arguments))
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        suggestionsAdapter = SimpleCursorAdapter(
            context,
            R.layout.item_suggestion,
            null, // cursor
            // FROM: names of cursor columns for bind on view
            arrayOf("tag"),
            // TO: text view id for bind data from cursor
            intArrayOf(android.R.id.text1),
            CursorAdapter.FLAG_REGISTER_CONTENT_OBSERVER
        )
        suggestionsAdapter.setFilterQueryProvider { constraint ->
            populateAdapter(constraint)
        }
        setHasOptionsMenu(true)
    }

    private fun populateAdapter(constraint: CharSequence?): Cursor {
        // create cursor for table of 2 columns: _id, tag
        val cursor = MatrixCursor(arrayOf(BaseColumns._ID, "tag"))
        constraint ?: return cursor
        val currentCursor = suggestionsAdapter.cursor
        currentCursor.moveToFirst()
        // populate the cursor's table
        for (i in 0 until currentCursor.count) {
            val tagValue = currentCursor.getString(1) // column "tag"
            if (tagValue.contains(constraint, true))
                cursor.addRow(arrayOf<Any>(i, tagValue))
            currentCursor.moveToNext()
        }
        return cursor
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
        val autoComplTv = searchView
            .findViewById<AutoCompleteTextView>(R.id.search_src_text)
        autoComplTv.threshold = 1

        searchView.suggestionsAdapter = suggestionsAdapter
        searchView.setOnSuggestionListener(object : SearchView.OnSuggestionListener {
            override fun onSuggestionSelect(position: Int) = false

            override fun onSuggestionClick(position: Int): Boolean {
                suggestionsAdapter.cursor.moveToPosition(position)
                val tag = suggestionsAdapter.cursor.getString(1)
                searchView.setQuery(tag, true)
                // Для увеличения счетчика использования тега (use count)
                viewModel.handleSuggestion(tag)
                return false
            }
        })

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
        viewModel.observeList(viewLifecycleOwner, args.onlyBookmarkedArticles) {
            articlesAdapter.submitList(it)
        }
        viewModel.observeTags(viewLifecycleOwner) {
            binding.tags = it
        }
        viewModel.observeCategories(viewLifecycleOwner) {
            binding.categories = it
        }
        // Корневая для фрагмента вьюгруппа - SwipeRefreshLayout
        refresh.setOnRefreshListener {
            viewModel.refresh()
        }
    }

    override fun onDestroyView() {
        toolbar.search_view?.setOnQueryTextListener(null)
        super.onDestroyView()
    }

    override fun renderLoading(loadingState: Loading) {
        val progressBar = root.progress
        when (loadingState) {
            Loading.SHOW_LOADING -> if (!refresh.isRefreshing) progressBar.isVisible = true
            Loading.SHOW_BLOCKING_LOADING -> progressBar.isVisible = false
            Loading.HIDE_LOADING -> {
                progressBar.isVisible = false
                if (refresh.isRefreshing) refresh.isRefreshing = false
            }
        }
    }

    inner class ArticlesBinding : Binding() {
        var isFocusedSearch: Boolean = false
        var searchQuery: String? = null
        var isSearch: Boolean = false
        private var isLoading: Boolean by RenderProp(true) {
            // https://blog.mindorks.com/using-shimmer-effect-placeholder-in-android
            // TODO show shimmer on rv_list
        }
        private var isHashtagSearch: Boolean by RenderProp(false)
        var tags: List<String> by RenderProp(emptyList())

        var categories: List<CategoryData> = emptyList()
        var selectedCategories: List<String> by RenderProp(emptyList()) {
            // todo selected color on icon
        }

        override fun bind(data: IViewModelState) {
            data as ArticlesState
            searchQuery = data.searchQuery
            isSearch = data.isSearch
            isLoading = data.isLoading
            isHashtagSearch = data.isHashtagSearch
            selectedCategories = data.selectedCategories
        }

        override val afterFragmentInflatedHandler: () -> Unit = {
            Log.d(
                "M_S_ArticlesBinding", "trigger " +
                        "afterFragmentInflatedHandler: invoke dependsOn method"
            )
            dependsOn<Boolean, List<String>>(::isHashtagSearch, ::tags) { ihs, tags ->
                val cursor = MatrixCursor(arrayOf(BaseColumns._ID, "tag"))
                if (ihs && tags.isNotEmpty())
                    for ((counter, tag) in tags.withIndex()) {
                        cursor.addRow(arrayOf(counter, tag))
                    }
                suggestionsAdapter.changeCursor(cursor)
                Log.d(
                    "M_S_ArticlesBinding", "trigger dependsOn handler: " +
                            "for isHashtagSearch/tags: isHashtagSearch = $isHashtagSearch " +
                            "tags is not empty = ${tags.isNotEmpty()} new cursor " +
                            "size - ${cursor.count}"
                )
            }
        }

        // коммент на 47:20 (лекц 8). Сохранение UI-элементов реализовать самим.
        // Пишем значения в бандл. Для этого надо переопределить всего
        // два метода - saveUi и restoreUi. И то же самое сделать с вьюмоделью.
        // TODO save UI
    }
}