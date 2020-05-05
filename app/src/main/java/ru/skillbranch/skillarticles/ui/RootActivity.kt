package ru.skillbranch.skillarticles.ui

import android.os.Bundle
import android.view.Menu
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.widget.SearchView
import androidx.lifecycle.ViewModelProviders
import com.google.android.material.snackbar.Snackbar
import kotlinx.android.synthetic.main.activity_root.*
import kotlinx.android.synthetic.main.layout_bottombar.*
import kotlinx.android.synthetic.main.layout_submenu.*
import ru.skillbranch.skillarticles.R
import ru.skillbranch.skillarticles.extensions.dpToIntPx
import ru.skillbranch.skillarticles.viewmodels.ArticleState
import ru.skillbranch.skillarticles.viewmodels.ArticleViewModel
import ru.skillbranch.skillarticles.viewmodels.Notify
import ru.skillbranch.skillarticles.viewmodels.ViewModelFactory

class RootActivity : AppCompatActivity() {

    private lateinit var viewModel: ArticleViewModel
    private var query: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_root)
        setupToolbar()
        setupBottombar()
        setupSubmenu()

        val vmFactory = ViewModelFactory("0")
        viewModel = ViewModelProviders.of(this, vmFactory)
            .get(ArticleViewModel::class.java)
        viewModel.observeState(this) {
            query = it.searchQuery
            renderUI(it)
        }
        viewModel.observeNotifications(this) {
            renderNotification(it)
        }
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
        tv_text_content.text = if (data.isLoadingContent) "loading"
        else data.content.first() as String

        // bind toolbar
        toolbar.title = data.title ?: "Skill Articles"
        toolbar.subtitle = data.category ?: "loading..."
        if (data.categoryIcon != null) {
            toolbar.logo = getDrawable(data.categoryIcon as Int)
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

    private fun setupToolbar() {
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        val logo = if (toolbar.childCount > 2) toolbar.getChildAt(2)
                as ImageView else null
        logo?.scaleType = ImageView.ScaleType.CENTER_CROP
//        Log.d("M_RootActivity", "logo => $logo")

        /** null !!! for Toolbar.LayoutParams */
        val lp = logo?.layoutParams // as? Toolbar.LayoutParams
//        Log.d("M_RootActivity", "lp => $lp")

        lp?.let {
            it.width = this.dpToIntPx(40)
            it.height = this.dpToIntPx(40)
//            it.marginEnd = this.dpToIntPx(16) // for Toolbar lp
            logo.layoutParams = it
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
    }

    private fun setupSubmenu() {
        btn_text_up.setOnClickListener {
            viewModel.handleUpText()
        }
        btn_text_down.setOnClickListener {
            viewModel.handleDownText()
        }
        switch_mode.setOnClickListener {
            viewModel.handleNightMode()
        }
    }

    // https://stackoverflow.com/questions/22498344/is-there-a-better-way-to-restore-searchview-state
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_search, menu)
        val searchItem = menu?.findItem(R.id.action_search)
        val searchView = searchItem?.actionView as SearchView
        if (!query.isNullOrEmpty()) {
            searchItem.expandActionView()
            searchView.setQuery(query, true)
            searchView.clearFocus()
        }
        with(searchView) {
            queryHint = "Введите поисковый запрос"
            setOnQueryTextListener(object : SearchView.OnQueryTextListener {
                override fun onQueryTextSubmit(query: String?): Boolean {
                    return true
                }

                override fun onQueryTextChange(newText: String?): Boolean {
                    viewModel.handleSearchQuery(newText)
                    return true
                }
            })
            setOnCloseListener {
                viewModel.handleSearchQuery(null)
                false
            }
        }
        return super.onCreateOptionsMenu(menu)
    }
}
