package ru.skillbranch.skillarticles.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.ImageView
import androidx.annotation.VisibleForTesting
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.widget.SearchView
import androidx.appcompat.widget.Toolbar
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.snackbar.Snackbar
import kotlinx.android.synthetic.main.activity_root.*
import kotlinx.android.synthetic.main.layout_bottombar.*
import kotlinx.android.synthetic.main.layout_submenu.*
import kotlinx.android.synthetic.main.search_view_layout.*
import ru.skillbranch.skillarticles.R
import ru.skillbranch.skillarticles.data.repositories.MarkdownElement
import ru.skillbranch.skillarticles.extensions.dpToIntPx
import ru.skillbranch.skillarticles.extensions.hideKeyboard
import ru.skillbranch.skillarticles.extensions.setMarginOptionally
import ru.skillbranch.skillarticles.ui.base.BaseActivity
import ru.skillbranch.skillarticles.ui.base.Binding
import ru.skillbranch.skillarticles.ui.delegates.ObserveProp
import ru.skillbranch.skillarticles.ui.delegates.RenderProp
import ru.skillbranch.skillarticles.viewmodels.ArticleState
import ru.skillbranch.skillarticles.viewmodels.ArticleViewModel
import ru.skillbranch.skillarticles.viewmodels.base.IViewModelState
import ru.skillbranch.skillarticles.viewmodels.base.Notify

class RootActivity : BaseActivity<ArticleViewModel>(), IArticleView {

    override val layout = R.layout.activity_root
    override val viewModel: ArticleViewModel by provideViewModel("0")

    @VisibleForTesting(otherwise = VisibleForTesting.PROTECTED)
    public override val binding: ArticleBinding by lazy { ArticleBinding() }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_search, menu)
        val searchMenuItem = menu?.findItem(R.id.action_search)
        val searchView = searchMenuItem?.actionView as? SearchView
        searchView?.queryHint = getString(R.string.article_search_placeholder)

        // restore search
        if (binding.isSearch) {
            searchMenuItem?.expandActionView()
            searchView?.setQuery(binding.searchQuery, false)
            if (binding.isFocusedSearch) searchView?.requestFocus()
            else searchView?.clearFocus()
        }

        searchMenuItem?.setOnActionExpandListener(
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
        searchView?.setOnQueryTextListener(
            object : SearchView.OnQueryTextListener {
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

    override fun renderNotification(notify: Notify) {
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
            if (!tv_text_content.hasFocus()) tv_text_content.requestFocus()
            hideKeyboard(btn_result_up)
            viewModel.handleUpResult()
        }
        btn_result_down.setOnClickListener {
            if (!tv_text_content.hasFocus()) tv_text_content.requestFocus()
            hideKeyboard(btn_result_down)
            viewModel.handleDownResult()
        }
        btn_search_close.setOnClickListener {
            viewModel.handleIsSearch(false)
            invalidateOptionsMenu()
        }
    }

    private fun setupCopyListener() {
        tv_text_content.setCopyListener { copy ->
            val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("Copied code", copy)
            clipboard.setPrimaryClip(clip)
            viewModel.handleCopyCode()
        }
    }

    override fun showSearchBar() {
        // Чтобы контент был виден до самого конца (не перекрывался боттомбаром)
        scroll.setMarginOptionally(bottom = dpToIntPx(56))
        bottombar.setSearchState(true)
    }

    override fun hideSearchBar() {
        scroll.setMarginOptionally(bottom = dpToIntPx(0))
        bottombar.setSearchState(false)
    }

    //===================================================================

    inner class ArticleBinding : Binding() {
        var isFocusedSearch: Boolean = false
        var searchQuery: String? = null

        private var isLike: Boolean by RenderProp(false) {
            btn_like.isChecked = it
        }
        private var isBookmark: Boolean by RenderProp(false) {
            btn_bookmark.isChecked = it
        }
        private var isShowMenu: Boolean by RenderProp(false) {
            btn_settings.isChecked = it
            if (it) submenu.open() else submenu.close()
        }
        private var title: String by RenderProp("loading...") {
            toolbar.title = it
        }
        private var category: String by RenderProp("loading...") {
            toolbar.subtitle = it
        }
        private var categoryIcon: Int by RenderProp(R.drawable.logo_placeholder) {
            toolbar.logo = getDrawable(it)
        }
        private var isBigText: Boolean by RenderProp(false) {
            if (it) {
                tv_text_content.textSize = 18f
                btn_text_up.isChecked = true
                btn_text_down.isChecked = false
            } else {
                tv_text_content.textSize = 14f
                btn_text_up.isChecked = false
                btn_text_down.isChecked = true
            }
        }
        private var isDarkMode: Boolean by RenderProp(
            value = false, needInit = false
        ) {
            // bind submenu views
            switch_mode.isChecked = it
            delegate.localNightMode =
                if (it) AppCompatDelegate.MODE_NIGHT_YES
                else AppCompatDelegate.MODE_NIGHT_NO
        }
        private var isLoadingContent by ObserveProp(true)

        var isSearch: Boolean by ObserveProp(false) {
            if (it) {
                showSearchBar()
                with(toolbar) {
                    (layoutParams as AppBarLayout.LayoutParams)
                        .scrollFlags = AppBarLayout.LayoutParams.SCROLL_FLAG_NO_SCROLL
                }
            } else {
                hideSearchBar()
                with(toolbar) {
                    (layoutParams as AppBarLayout.LayoutParams)
                        .scrollFlags = AppBarLayout.LayoutParams.SCROLL_FLAG_SCROLL or
                            AppBarLayout.LayoutParams.SCROLL_FLAG_ENTER_ALWAYS_COLLAPSED
                }
            }
        }

        private var searchResults: List<Pair<Int, Int>>
                by ObserveProp(emptyList<Pair<Int, Int>>())
        private var searchPosition: Int by ObserveProp(0)

        private var content: List<MarkdownElement>
                by ObserveProp(emptyList<MarkdownElement>()) {
                    tv_text_content.isLoading = it.isEmpty()
                    tv_text_content.setContent(it)
                    if (it.isNotEmpty()) setupCopyListener()
                }

        override fun onFinishInflate() {
            dependsOn<Boolean, Boolean, List<Pair<Int, Int>>, Int>(
                ::isLoadingContent,
                ::isSearch,
                ::searchResults,
                ::searchPosition
            ) { ilc, ise, sr, sp ->
                // Если контент уже загружен и мы в поиске
                if (!ilc && ise) {
                    tv_text_content.renderSearchResult(sr)
                    tv_text_content.renderSearchPosition(sr.getOrNull(sp))
                }
                // Если контент уже загружен, но мы не в поиске
                if (!ilc && !ise) {
                    tv_text_content.clearSearchResult()
                }
                bottombar.bindSearchInfo(sr.size, sp)
            }
        }

        override fun bind(data: IViewModelState) {
            data as ArticleState
            isLike = data.isLike
            isBookmark = data.isBookmark
            isShowMenu = data.isShowMenu
            isBigText = data.isBigText
            isDarkMode = data.isDarkMode

            if (data.title != null) title = data.title
            if (data.category != null) category = data.category
            if (data.categoryIcon != null) categoryIcon = data.categoryIcon as Int
            content = data.content

            isLoadingContent = data.isLoadingContent
            isSearch = data.isSearch
            searchQuery = data.searchQuery
            searchResults = data.searchResults
            searchPosition = data.searchPosition
        }

        override fun saveUi(outState: Bundle) {
            outState.putBoolean(::isFocusedSearch.name, search_view?.hasFocus() ?: false)
        }

        override fun restoreUi(savedState: Bundle) {
            isFocusedSearch = savedState.getBoolean(::isFocusedSearch.name)
        }
    }
}
