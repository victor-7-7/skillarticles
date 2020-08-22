package ru.skillbranch.skillarticles.ui.article

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.text.method.LinkMovementMethod
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.WindowManager
import android.widget.TextView
import androidx.annotation.VisibleForTesting
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.widget.SearchView
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.request.RequestOptions.circleCropTransform
import com.google.android.material.appbar.AppBarLayout
import kotlinx.android.synthetic.main.activity_root.*
import kotlinx.android.synthetic.main.fragment_article.*
import kotlinx.android.synthetic.main.layout_bottombar.view.*
import kotlinx.android.synthetic.main.layout_submenu.view.*
import kotlinx.android.synthetic.main.search_view_layout.*
import ru.skillbranch.skillarticles.R
import ru.skillbranch.skillarticles.data.repositories.Element
import ru.skillbranch.skillarticles.data.repositories.MarkdownElement
import ru.skillbranch.skillarticles.extensions.*
import ru.skillbranch.skillarticles.ui.base.*
import ru.skillbranch.skillarticles.ui.custom.ArticleSubmenu
import ru.skillbranch.skillarticles.ui.custom.Bottombar
import ru.skillbranch.skillarticles.ui.custom.markdown.MarkdownBuilder
import ru.skillbranch.skillarticles.ui.delegates.RenderProp
import ru.skillbranch.skillarticles.viewmodels.article.ArticleState
import ru.skillbranch.skillarticles.viewmodels.article.ArticleViewModel
import ru.skillbranch.skillarticles.viewmodels.base.IViewModelState
import ru.skillbranch.skillarticles.viewmodels.base.Loading
import ru.skillbranch.skillarticles.viewmodels.base.ViewModelFactory

class ArticleFragment : BaseFragment<ArticleViewModel>(), IArticleView {
    private val args: ArticleFragmentArgs by navArgs()

    override val viewModel: ArticleViewModel by viewModels {
        ViewModelFactory(owner = this, params = args.articleId)
    }
    override val layout = R.layout.fragment_article

    @VisibleForTesting(otherwise = VisibleForTesting.PROTECTED)
    override val binding: ArticleBinding by lazy { ArticleBinding() }

    private val commentsAdapter by lazy {
        CommentsAdapter {
            Log.d(
                "M_ArticleFragment", "click on comment: " +
                        "id: ${it.id} slug: ${it.slug}"
            )
            viewModel.handleReplyTo(it.id, it.user.name)
            et_comment.requestFocus()
            scroll.smoothScrollTo(0, wrap_comments.top)
            et_comment.context.showKeyboard(et_comment)
        }
    }
    override val prepareToolbar: (ToolbarBuilder.() -> Unit)? = {
        this.setSubtitle(args.category)
            .setLogo(args.categoryIcon)
            .addMenuItem(
                MenuItemHolder(
                    "Search",
                    R.id.action_search,
                    R.drawable.ic_search_black_24dp,
                    R.layout.search_view_layout
                )
            )
    }
    override val prepareBottombar: (BottombarBuilder.() -> Unit)? = {
        this.addView(R.layout.layout_submenu)
            .addView(R.layout.layout_bottombar)
            .setVisibility(false)
    }
    private val bottombar
        get() = root.findViewById<Bottombar>(R.id.bottombar)
    private val submenu
        get() = root.findViewById<ArticleSubmenu>(R.id.submenu)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onDestroyView() {
        // window resize options
        root.window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING)
        super.onDestroyView()
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        super.onPrepareOptionsMenu(menu)
        val searchMenuItem = menu.findItem(R.id.action_search)
        val searchView = searchMenuItem.actionView as SearchView
        searchView.queryHint = getString(R.string.article_search_placeholder)

        // restore search
        if (binding.isSearch) {
            searchMenuItem.expandActionView()
            searchView.setQuery(binding.searchQuery, false)
            if (binding.isFocusedSearch) searchView.requestFocus()
            else searchView.clearFocus()
        }

        searchMenuItem.setOnActionExpandListener(
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
//                    viewModel.handleSearchQuery(query)
                    return true
                }

                override fun onQueryTextChange(newText: String): Boolean {
                    viewModel.handleSearchQuery(newText)
                    return true
                }
            })
    }

    override fun showSearchBar() {
        bottombar.setSearchState(true)
        // Чтобы контент был виден до самого конца (не перекрывался боттомбаром)
        scroll.setMarginOptionally(bottom = root.dpToIntPx(56))
    }

    override fun hideSearchBar() {
        bottombar.setSearchState(false)
        scroll.setMarginOptionally(bottom = 0)
    }

    override fun setupViews() {
        // window resize options
        root.window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)

        setupSubmenu()
        setupBottombar()
        // init views
        val avatarSize = root.dpToIntPx(40)
        val cornerRadius = root.dpToIntPx(8)

        Glide.with(root).load(args.authorAvatar)
            .apply(circleCropTransform()).override(avatarSize)
            .into(iv_author_avatar)

        Glide.with(root).load(args.poster)
            .transform(CenterCrop(), RoundedCorners(cornerRadius))
            .into(iv_poster)

        tv_title.text = args.title
        tv_author.text = args.author
        tv_date.text = args.date.format()

        tv_source.movementMethod = LinkMovementMethod.getInstance()
        tv_hashtags.setLineSpacing(tv_hashtags.textSize * .5f, 1f)

        et_comment.setOnEditorActionListener { view, _, _ ->
            root.hideKeyboard(view)
            viewModel.handleSendComment(view.text.toString())
            if (viewModel.currentState.isAuth) {
                view.text = null
                view.clearFocus()
            }
            true
        }

        et_comment.setOnFocusChangeListener { _, hasFocus ->
            viewModel.handleCommentFocus(hasFocus)
        }

        wrap_comments.setEndIconOnClickListener { view ->
            view.context.hideKeyboard(view)
            viewModel.handleClearComment()
//            et_comment.text = null
//            et_comment.clearFocus()
        }

        with(rv_comments) {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = commentsAdapter
        }
        viewModel.observeCommentList(viewLifecycleOwner) {
            commentsAdapter.submitList(it)
        }
        // Корневая для фрагмента вьюгруппа - SwipeRefreshLayout
        refresh.setOnRefreshListener {
            viewModel.refresh()
        }
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

    private fun setupSubmenu() {
        submenu.btn_text_up.setOnClickListener {
            viewModel.handleUpTextSize()
        }
        submenu.btn_text_down.setOnClickListener {
            viewModel.handleDownTextSize()
        }
        submenu.switch_mode.setOnClickListener {
            viewModel.handleNightMode()
        }
    }

    private fun setupBottombar() {
        bottombar.btn_like.setOnClickListener {
            viewModel.handleLike()
        }
        bottombar.btn_bookmark.setOnClickListener {
            viewModel.handleBookmark()
        }
        bottombar.btn_share.setOnClickListener {
            viewModel.handleShare()
        }
        bottombar.btn_settings.setOnClickListener {
            viewModel.handleToggleMenu()
        }
        bottombar.btn_result_up.setOnClickListener {
            if (!tv_text_content.hasFocus()) tv_text_content.requestFocus()
            root.hideKeyboard(bottombar.btn_result_up)
            viewModel.handleUpResult()
        }
        bottombar.btn_result_down.setOnClickListener {
            if (!tv_text_content.hasFocus()) tv_text_content.requestFocus()
            root.hideKeyboard(bottombar.btn_result_down)
            viewModel.handleDownResult()
        }
        bottombar.btn_search_close.setOnClickListener {
            viewModel.handleIsSearch(false)
            root.invalidateOptionsMenu()
        }
    }

    private fun setupCopyListener() {
        tv_text_content.setCopyListener { copy ->
            val clipboard = requireContext().getSystemService(
                Context.CLIPBOARD_SERVICE
            ) as ClipboardManager
            val clip = ClipData.newPlainText("Copied code", copy)
            clipboard.setPrimaryClip(clip)
            viewModel.handleCopyCode()
        }
    }

    override fun onPause() {
        super.onPause()
        if (et_comment.text.isNotBlank())
            viewModel.saveComment(et_comment.text.toString())
    }


    //===================================================================

    inner class ArticleBinding : Binding() {
        var isFocusedSearch: Boolean = false
        var searchQuery: String? = null

        /** Подсказка, появляющаяся в редактируемом поле комментария. Если
         * юзер отвечает на комментарий юзера [name], то подсказка будет вида -
         * Reply to [name]. Если юзер комментирует статью, то подсказка будет
         * вида - Comment */
        private var answerTo: String by RenderProp("Comment") {
            wrap_comments.hint = it
        }
        private var comment: String by RenderProp("") {
            et_comment.setText(it)
        }
        private var isShowBottombar: Boolean by RenderProp(true) {
            if (it) bottombar.show() else bottombar.hide()
            if (submenu.isOpen) submenu.isVisible = it
        }
        private var isLike: Boolean by RenderProp(false) {
            bottombar.btn_like.isChecked = it
        }
        private var isBookmark: Boolean by RenderProp(false) {
            bottombar.btn_bookmark.isChecked = it
        }
        private var isShowMenu: Boolean by RenderProp(false) {
            bottombar.btn_settings.isChecked = it
            if (it) submenu.open() else submenu.close()
        }

        // bind submenu views
        private var isBigText: Boolean by RenderProp(false) { bigText ->
            if (bigText) {
                tv_text_content.textSize = 18f
                tv_source.textSize = 18f
                submenu.btn_text_up.isChecked = true
                submenu.btn_text_down.isChecked = false
            } else {
                tv_text_content.textSize = 14f
                tv_source.textSize = 14f
                submenu.btn_text_up.isChecked = false
                submenu.btn_text_down.isChecked = true
            }
        }
        private var isDarkMode: Boolean by RenderProp(
            value = false,
            needInit = false
        ) { dark ->
            submenu.switch_mode.isChecked = dark
            val mode = if (dark) AppCompatDelegate.MODE_NIGHT_YES
            else AppCompatDelegate.MODE_NIGHT_NO
            root.delegate.localNightMode = mode
        }
        private var isLoadingContent by RenderProp(true)

        var isSearch: Boolean by RenderProp(false) {
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
                by RenderProp(emptyList<Pair<Int, Int>>())
        private var searchPosition: Int by RenderProp(0)

        private var content: List<MarkdownElement>
                by RenderProp(emptyList<MarkdownElement>()) {
                    tv_text_content.isLoading = it.isEmpty()
                    tv_text_content.setContent(it)
                    if (it.isNotEmpty()) setupCopyListener()
                }

        private var source by RenderProp("") { source ->
            if (source.isEmpty()) tv_source.visibility = View.GONE
            else {
                val element = Element.Link(source, "Article source")
                val markdownElem = MarkdownElement.Text(mutableListOf(element))
                val spannedString = MarkdownBuilder(context!!).markdownToSpan(markdownElem)
                tv_source.visibility = View.VISIBLE
                tv_source.setText(spannedString, TextView.BufferType.SPANNABLE)
            }
        }
        private var tags by RenderProp(emptyList<String>()) { tags ->
            if (tags.isEmpty()) tv_hashtags.visibility = View.GONE
            else {
                val elements = mutableListOf<Element>()
                tags.forEachIndexed { index, tag ->
                    // Пробел в конце тега - для симметрии бэкграунда
                    elements.add(Element.InlineCode("$tag "))
                    if (index < tags.size - 1)
                    // Между тегами добавляем пробел, чтобы их
                    // бэкграунды не слипались
                        elements.add(Element.Text(" "))
                }
                val markdownElem = MarkdownElement.Text(elements)
                val spannedString = MarkdownBuilder(context!!).markdownToSpan(markdownElem)
                tv_hashtags.visibility = View.VISIBLE
                tv_hashtags.setText(spannedString, TextView.BufferType.SPANNABLE)
            }
        }

        override val afterInflated: (() -> Unit)? = {
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

            content = data.content
            isLoadingContent = data.isLoadingContent

            isSearch = data.isSearch
            searchQuery = data.searchQuery
            searchResults = data.searchResults
            searchPosition = data.searchPosition

            answerTo = data.answerTo ?: "Comment"
            isShowBottombar = data.showBottombar
            comment = data.commentText ?: ""

            source = data.source ?: ""
            tags = data.tags
        }

        override fun saveUi(outState: Bundle) {
            outState.putBoolean(::isFocusedSearch.name, search_view?.hasFocus() ?: false)
        }

        override fun restoreUi(savedState: Bundle?) {
            isFocusedSearch = savedState?.getBoolean(::isFocusedSearch.name) ?: false
        }
    }
}