package ru.skillbranch.skillarticles.ui

import android.os.Bundle
import android.text.Selection
import android.text.Spannable
import android.text.SpannableString
import android.text.method.ScrollingMovementMethod
import android.view.Menu
import android.view.MenuItem
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.VisibleForTesting
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.widget.SearchView
import androidx.appcompat.widget.Toolbar
import com.google.android.material.snackbar.Snackbar
import kotlinx.android.synthetic.main.activity_root.*
import kotlinx.android.synthetic.main.layout_bottombar.*
import kotlinx.android.synthetic.main.layout_submenu.*
import kotlinx.android.synthetic.main.search_view_layout.*
import ru.skillbranch.skillarticles.R
import ru.skillbranch.skillarticles.extensions.dpToIntPx
import ru.skillbranch.skillarticles.extensions.getSpans
import ru.skillbranch.skillarticles.extensions.setMarginOptionally
import ru.skillbranch.skillarticles.ui.base.BaseActivity
import ru.skillbranch.skillarticles.ui.base.Binding
import ru.skillbranch.skillarticles.ui.custom.SearchFocusSpan
import ru.skillbranch.skillarticles.ui.custom.SearchSpan
import ru.skillbranch.skillarticles.ui.delegates.AttrValue
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

    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    val bgColor by AttrValue(R.attr.colorSecondary)

    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    val fgColor by AttrValue(R.attr.colorOnSecondary)

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

    override fun renderSearchResult(searchResult: List<Pair<Int, Int>>) {
        val content = tv_text_content.text as Spannable
//        tv_text_content.isVisible // ??????????????????????????????????????????????????
        clearSearchResult() // results before current render

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
//        val spans = content.getSpans(
//            0, tv_text_content.text.lastIndex,
//            SearchSpan::class.java
//        )
//        if (spans.isEmpty()) return
//        for (span in spans) {
//            content.removeSpan(span)
//        }
        content.getSpans<SearchSpan>().forEach { content.removeSpan(it) }
    }

    override fun renderSearchPosition(searchPosition: Int) {
        val content = (tv_text_content.text as? Spannable) ?: return

//        val spans = content.getSpans(
//            0, tv_text_content.text.lastIndex,
//            SearchSpan::class.java
//        )
        val spans = content.getSpans<SearchSpan>()

        // clear last search focus position
//        content.getSpans(
//            0, tv_text_content.text.lastIndex,
//            SearchFocusSpan::class.java
//        ).forEach { content.removeSpan(it) }
        content.getSpans<SearchFocusSpan>().forEach { content.removeSpan(it) }

        if (spans.isNotEmpty()) {
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
            if (it) showSearchBar() else hideSearchBar()
        }

        private var searchResults: List<Pair<Int, Int>> by ObserveProp(emptyList())
        private var searchPosition: Int by ObserveProp(0)

        private var content: String by ObserveProp("loading...") {
            tv_text_content.setText(it, TextView.BufferType.SPANNABLE)
            // Чтобы перевод фокуса к следующему поисковому результату
            // мог проскроллить контент до этого результата
            tv_text_content.movementMethod = ScrollingMovementMethod()
        }

        override fun onFinishInflate() {
            dependsOn<Boolean, Boolean, List<Pair<Int, Int>>, Int>(
                ::isLoadingContent,
                ::isSearch,
                ::searchResults,
                ::searchPosition
            ) { ilc, ise, sr, sp ->
                if (!ilc && ise) {
                    renderSearchResult(sr)
                    /** метод renderSearchPosition(0) вызывается и в теле
                     * метода renderSearchResult(). Все ли здесь чисто? */
                    renderSearchPosition(sp)
                }
                if (!ilc && !ise) {
                    clearSearchResult()
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
            if (data.content.isNotEmpty()) content = data.content.first() as String

            isLoadingContent = data.isLoadingContent
            isSearch = data.isSearch
            searchQuery = data.searchQuery
            searchResults = data.searchResults
            searchPosition = data.searchPosition
        }

        override fun saveUI(outState: Bundle) {
            outState.putBoolean(::isFocusedSearch.name, search_view?.hasFocus() ?: false)
        }

        override fun restoreUI(savedState: Bundle) {
            isFocusedSearch = savedState.getBoolean(::isFocusedSearch.name)
        }
    }
}
