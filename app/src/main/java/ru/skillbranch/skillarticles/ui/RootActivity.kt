package ru.skillbranch.skillarticles.ui

import android.graphics.Color
import android.os.Bundle
import android.text.Selection
import android.text.Spannable
import android.text.SpannableString
import android.text.method.ScrollingMovementMethod
import android.view.Menu
import android.view.MenuItem
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.widget.SearchView
import androidx.appcompat.widget.Toolbar
import androidx.lifecycle.ViewModelProviders
import com.google.android.material.snackbar.Snackbar
import kotlinx.android.synthetic.main.activity_root.*
import kotlinx.android.synthetic.main.layout_bottombar.*
import kotlinx.android.synthetic.main.layout_submenu.*
import kotlinx.android.synthetic.main.search_view_layout.*
import ru.skillbranch.skillarticles.R
import ru.skillbranch.skillarticles.extensions.dpToIntPx
import ru.skillbranch.skillarticles.extensions.setMarginOptionally
import ru.skillbranch.skillarticles.ui.base.BaseActivity
import ru.skillbranch.skillarticles.ui.custom.SearchFocusSpan
import ru.skillbranch.skillarticles.ui.custom.SearchSpan
import ru.skillbranch.skillarticles.viewmodels.ArticleState
import ru.skillbranch.skillarticles.viewmodels.ArticleViewModel
import ru.skillbranch.skillarticles.viewmodels.base.Notify
import ru.skillbranch.skillarticles.viewmodels.base.ViewModelFactory

class RootActivity : BaseActivity<ArticleViewModel>(), IArticleView {

    override val layout = R.layout.activity_root
    override lateinit var viewModel: ArticleViewModel

//    private var searchQuery: String? = null
//    private var isSearching = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val vmFactory = ViewModelFactory("0")
        viewModel = ViewModelProviders.of(this, vmFactory)
            .get(ArticleViewModel::class.java)

        viewModel.observeState(this) {
            // save search mode for creating options menu
//            isSearching = it.isSearch
//            searchQuery = it.searchQuery
            //-------------------------------------------
            renderUI(it)
        }
        viewModel.observeNotifications(this) {
            renderNotification(it)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_search, menu)
        val searchMenuItem = menu?.findItem(R.id.action_search)
        val searchView = searchMenuItem?.actionView as? SearchView
        searchView?.queryHint = getString(R.string.article_search_placeholder)

        // restore search
        if (viewModel.getIsSearch()) {
            searchMenuItem?.expandActionView()
            searchView?.setQuery(viewModel.getSearchQuery(), false)
            renderSearchResult(viewModel.getSearchResults())
            renderSearchPosition(viewModel.getSearchPosition())
            searchView?.clearFocus()
        }

        searchMenuItem?.setOnActionExpandListener(object : MenuItem.OnActionExpandListener {
            override fun onMenuItemActionExpand(item: MenuItem?): Boolean {
                viewModel.handleIsSearch(true)
                return true
            }
            override fun onMenuItemActionCollapse(item: MenuItem?): Boolean {
                viewModel.handleIsSearch(false)
                return true
            }
        })
        searchView?.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String): Boolean {
//                viewModel.handleSearchQuery(query)
                return true
            }
            override fun onQueryTextChange(newText: String): Boolean {
                viewModel.handleSearchQuery(newText)
                return true
            }
        })
        return super.onCreateOptionsMenu(menu)
    }

    private fun renderUI(data: ArticleState) {
        // bind submenu state
        btn_settings.isChecked = data.isShowMenu
        if (data.isShowMenu) submenu.open() else submenu.close()

        // bind article person data
        btn_like.isChecked = data.isLike
        btn_bookmark.isChecked = data.isBookmark

        // bind submenu views
        switch_mode.isChecked = data.isDarkMode
        delegate.localNightMode =
            if (data.isDarkMode) AppCompatDelegate.MODE_NIGHT_YES
            else AppCompatDelegate.MODE_NIGHT_NO

        // bind toolbar
        toolbar.title = data.title ?: "Skill Articles"
        toolbar.subtitle = data.category ?: "loading..."
        if (data.categoryIcon != null) {
            toolbar.logo = getDrawable(data.categoryIcon as Int)
        }

        if (data.isBigText) {
            tv_text_content.textSize = 18f
            btn_text_up.isChecked = true
            btn_text_down.isChecked = false
        } else {
            tv_text_content.textSize = 14f
            btn_text_up.isChecked = false
            btn_text_down.isChecked = true
        }

        // bind content
        if (data.isLoadingContent) tv_text_content.text = "loading..."
        // Присваиваем полученный из репозитория контент только один раз
        else if (tv_text_content.text == "loading...") {
            val content = data.content.first() as String
            tv_text_content.setText(content, TextView.BufferType.SPANNABLE)
            // Чтобы перевод фокуса к следующему поисковому результату
            // мог проскроллить контент до этого результата
            tv_text_content.movementMethod = ScrollingMovementMethod()
        }

        if (data.isSearch) {
            showSearchBar()
            if (search_view != null && search_view.hasFocus()) {
                renderSearchResult(data.searchResults)
            }
            if (search_view != null && !search_view.hasFocus()) {
                renderSearchPosition(data.searchPosition)
            }
            bottombar.bindSearchInfo(data.searchResults.size, data.searchPosition)
        } else {
            hideSearchBar()
            clearSearchResult()
            search_view?.isIconified = true
        }
    }

    override fun renderSearchResult(searchResult: List<Pair<Int, Int>>) {
        clearSearchResult() // results before current render
        if (searchResult.isEmpty()) return

        val content = tv_text_content.text as Spannable
        val bgColor = Color.RED
        val fgColor = Color.WHITE

        searchResult.forEach { (start, end) ->
            content.setSpan(
                SearchSpan(bgColor, fgColor), start, end,
                SpannableString.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }
        // for new search result set focus to 0
        renderSearchPosition(0)
    }

    override fun clearSearchResult() {
        val content = (tv_text_content.text as? Spannable) ?: return
        val spans = content.getSpans(
            0, tv_text_content.text.lastIndex,
            SearchSpan::class.java
        )
        if (spans.isEmpty()) return
        for (span in spans) {
            content.removeSpan(span)
        }
    }

    override fun renderSearchPosition(searchPosition: Int) {
        val content = (tv_text_content.text as? Spannable) ?: return
        val spans = content.getSpans(
            0, tv_text_content.text.lastIndex,
            SearchSpan::class.java
        )
        if (spans.isNotEmpty()) {
            val bgColor = Color.RED
            val fgColor = Color.WHITE
            // clear last search focus position
            content.getSpans(
                0, tv_text_content.text.lastIndex,
                SearchFocusSpan::class.java
            ).forEach { content.removeSpan(it) }

            // find span at position
            val nextSpan = spans[searchPosition]
            // move cursor to nextSpan
            Selection.setSelection(content, content.getSpanStart(nextSpan))
            // set search focus span over nextSpan
            content.setSpan(
                SearchFocusSpan(bgColor, fgColor),
                content.getSpanStart(nextSpan),
                content.getSpanEnd(nextSpan),
                SpannableString.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }
    }

    private fun renderNotification(notify: Notify) {
        val snackbar = Snackbar.make(
            coordinator_container,
            notify.message,
            Snackbar.LENGTH_LONG
        ).setAnchorView(bottombar)

        when (notify) {
            is Notify.TextMessage -> {
//                if(notify.message.contains("запрос")) snackbar.anchorView = null
            }
            is Notify.ActionMessage -> {
                snackbar.setActionTextColor(getColor(R.color.color_accent_dark))
                snackbar.setAction(notify.actionLabel) {
                    notify.actionHandler?.invoke()
                }
            }
            is Notify.ErrorMessage -> {
                with(snackbar) {
                    setBackgroundTint(getColor(R.color.design_default_color_error))
                    setTextColor(getColor(android.R.color.white))
                    setActionTextColor(getColor(android.R.color.white))
                    setAction(notify.errLabel) {
                        notify.errHandler?.invoke()
                    }
                }
            }
        }
        snackbar.show()
    }

    override fun setupViews() {
        setupToolbar()
        setupBottombar()
        setupSubmenu()
    }

    private fun setupToolbar() {
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        val logo = if (toolbar.childCount > 2) toolbar.getChildAt(2)
                as ImageView else null
        logo?.scaleType = ImageView.ScaleType.CENTER_CROP
//        Log.d("M_RootActivity", "logo => $logo")

        /** Важно !!! Toolbar взять из androidx.appcompat.widget.Toolbar,
         * иначе будет null */
        val lp = logo?.layoutParams as? Toolbar.LayoutParams
//        Log.d("M_RootActivity", "lp => $lp")

        lp?.let {
            it.width = this.dpToIntPx(40)
            it.height = this.dpToIntPx(40)
            it.marginEnd = this.dpToIntPx(16) // for Toolbar lp
            logo.layoutParams = it
        }
    }

    private fun setupSubmenu() {
        btn_text_up.setOnClickListener {
            viewModel.handleUpTextSize()
        }
        btn_text_down.setOnClickListener {
            viewModel.handleDownTextSize()
        }
        switch_mode.setOnClickListener {
            viewModel.handleNightMode()
        }
    }

    private fun setupBottombar() {
        btn_like.setOnClickListener {
            viewModel.handleLike()
        }
        btn_bookmark.setOnClickListener {
            viewModel.handleBookmark()
        }
        btn_share.setOnClickListener {
            viewModel.handleShare()
        }
        btn_settings.setOnClickListener {
            viewModel.handleToggleMenu()
        }
        btn_result_up.setOnClickListener {
            if (search_view.hasFocus()) search_view.clearFocus()
            viewModel.handleUpResult()
        }
        btn_result_down.setOnClickListener {
            if (search_view.hasFocus()) search_view.clearFocus()
            viewModel.handleDownResult()
        }
        btn_search_close.setOnClickListener {
            viewModel.handleIsSearch(false)
            invalidateOptionsMenu()
        }
    }

    override fun showSearchBar() {
        // Чтобы контент был виден до самого конца (не перекрывался боттомбаром)
        scroll.setMarginOptionally(bottomPx = dpToIntPx(56))
        bottombar.setSearchState(true)
    }

    override fun hideSearchBar() {
        scroll.setMarginOptionally(bottomPx = dpToIntPx(0))
        bottombar.setSearchState(false)
    }
}
