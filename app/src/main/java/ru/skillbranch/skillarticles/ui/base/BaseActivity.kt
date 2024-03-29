package ru.skillbranch.skillarticles.ui.base

import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.content.ContextCompat.getDrawable
import androidx.core.os.bundleOf
import androidx.core.view.children
import androidx.core.view.isVisible
import androidx.navigation.NavController
import androidx.navigation.findNavController
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions.circleCropTransform
import com.google.android.material.behavior.HideBottomViewOnScrollBehavior
import kotlinx.android.synthetic.main.activity_root.*
import ru.skillbranch.skillarticles.R
import ru.skillbranch.skillarticles.extensions.dpToIntPx
import ru.skillbranch.skillarticles.ui.RootActivity
import ru.skillbranch.skillarticles.viewmodels.base.*

abstract class BaseActivity<T : BaseViewModel<out IViewModelState>> : AppCompatActivity() {

    protected abstract val viewModel: T
    protected abstract val layout: Int
    lateinit var navController: NavController

    val toolbarBuilder = ToolbarBuilder()
    val bottombarBuilder = BottombarBuilder()

    abstract fun subscribeOnState(state: IViewModelState)

    abstract fun renderNotification(notify: Notify)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(layout)
        setSupportActionBar(toolbar)
        viewModel.observeState(this) {
            subscribeOnState(it)
        }
        viewModel.observeNotification(this) {
            renderNotification(it)
        }
        viewModel.observeNavigation(this) { subscribeOnNavigation(it) }
        viewModel.observeLoading(this) { renderLoading(it) }

        navController = findNavController(R.id.nav_host_fragment)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        viewModel.saveState()
        super.onSaveInstanceState(outState)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        viewModel.restoreState()
    }

    override fun onSupportNavigateUp(): Boolean {
        return navController.navigateUp() || super.onSupportNavigateUp()
    }

    private fun subscribeOnNavigation(command: NavigationCommand) {
        when (command) {
            is NavigationCommand.To -> {
                navController.navigate(
                    command.destination,
                    command.args,
                    command.options,
                    command.extras
                )
            }
            is NavigationCommand.FinishLogin -> {
                navController.navigate(R.id.finish_login)
                // Поскольку мы успешно авторизовались, то переходим к
                // представлению, с которого нас перекинуло на авторизацию
                if (command.privateDestination != null)
                    navController.navigate(command.privateDestination)
            }
            is NavigationCommand.StartLogin -> {
                navController.navigate(
                    R.id.start_login,
                    bundleOf(
                        "private_destination" to
                                (command.privateDestination ?: -1)
                    )
                )
            }
        }
    }

    // open для того, чтобы мы могли переопределить эту функцию
    // в любой другой дочерней активити
    open fun renderLoading(loadingState: Loading) {
        when (loadingState) {
            Loading.SHOW_LOADING -> progress.isVisible = true
            Loading.SHOW_BLOCKING_LOADING -> {
                progress.isVisible = true
                // todo: block interact with UI
            }
            Loading.HIDE_LOADING -> progress.isVisible = false
        }
    }
}

class ToolbarBuilder {
    private var subtitle: String? = null
    private var logo: String? = null
    private var visibility: Boolean = true
    val items: MutableList<MenuItemHolder> = mutableListOf()

    fun setSubtitle(subtitle: String): ToolbarBuilder {
        this.subtitle = subtitle
        return this
    }

    fun setLogo(logo: String): ToolbarBuilder {
        this.logo = logo
        return this
    }

    fun setVisibility(isVisible: Boolean): ToolbarBuilder {
        this.visibility = isVisible
        return this
    }

    fun addMenuItem(item: MenuItemHolder): ToolbarBuilder {
        this.items.add(item)
        return this
    }

    fun prepare(prepareFn: (ToolbarBuilder.() -> Unit)?): ToolbarBuilder {
        invalidate()
        prepareFn?.invoke(this)
        return this
    }

    fun build(root: RootActivity) {
        //show appbar if hidden due to scroll behavior
        root.appbar.setExpanded(true, true)
        with(root.toolbar) {
            subtitle = this@ToolbarBuilder.subtitle
            if (this@ToolbarBuilder.logo != null) {
                val logoSize = root.dpToIntPx(40)
                val logoMargin = root.dpToIntPx(16)
                val logoPlaceholder = getDrawable(root, R.drawable.logo_placeholder)

                logo = logoPlaceholder

                val logo = children.last() as? ImageView
                if (logo != null) {
                    logo.scaleType = ImageView.ScaleType.CENTER_CROP
                    (logo.layoutParams as? Toolbar.LayoutParams)?.let {
                        it.width = logoSize
                        it.height = logoSize
                        it.marginEnd = logoMargin
                        logo.layoutParams = it
                    }

                    Glide.with(root)
                        .load(this@ToolbarBuilder.logo)
                        .apply(circleCropTransform())
                        .override(logoSize)
                        .into(logo)
                }
            } else {
                logo = null
            }
        }
    }

    private fun invalidate(): ToolbarBuilder {
        this.subtitle = null
        this.logo = null
        this.visibility = true
        this.items.clear()
        return this
    }
}

data class MenuItemHolder(
    val title: String,
    val menuId: Int,
    val icon: Int,
    val actionViewLayout: Int?,
    val clickListener: ((MenuItem) -> Unit)? = null
)

class BottombarBuilder {
    private var visible: Boolean = true
    private val views = mutableListOf<Int>()
    private val viewsIds = mutableListOf<Int>()

    /** Метод добавляет ресурсный идентификатор лейаута
     * в интовую лист-переменную боттомбар билдера */
    fun addView(layoutId: Int): BottombarBuilder {
        views.add(layoutId)
        return this
    }

    fun setVisibility(isVisible: Boolean): BottombarBuilder {
        visible = isVisible
        return this
    }

    fun prepare(prepareFn: (BottombarBuilder.() -> Unit)?): BottombarBuilder {
        invalidate()
        prepareFn?.invoke(this)
        return this
    }

    private fun invalidate(): BottombarBuilder {
        visible = true
        views.clear()
        return this
    }

    fun build(root: RootActivity) {
        //remove old views
        if (viewsIds.isNotEmpty()) {
            viewsIds.forEach {
                val view = root.container.findViewById<View>(it)
                root.container.removeView(view)
            }
            viewsIds.clear()
        }
        //add new bottom bar views
        if (views.isNotEmpty()) {
            val inflater = LayoutInflater.from(root)
            views.forEach {
                val view = inflater.inflate(it, root.container, false)
                root.container.addView(view)
                viewsIds.add(view.id)
            }
        }
        with(root.nav_view) {
            isVisible = visible
            // show bottombar (if hidden due to scroll behavior)
            ((layoutParams as CoordinatorLayout.LayoutParams)
                .behavior as HideBottomViewOnScrollBehavior)
                .slideUp(this)
        }
    }
}

