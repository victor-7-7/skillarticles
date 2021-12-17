package ru.skillbranch.skillarticles.ui

import android.os.Bundle
import androidx.activity.viewModels
import androidx.navigation.NavOptions
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.android.synthetic.main.activity_root.*
import kotlinx.android.synthetic.main.layout_bottombar.*
import ru.skillbranch.skillarticles.R
import ru.skillbranch.skillarticles.extensions.selectMenuItem
import ru.skillbranch.skillarticles.ui.base.BaseActivity
import ru.skillbranch.skillarticles.viewmodels.RootViewModel
import ru.skillbranch.skillarticles.viewmodels.base.IViewModelState
import ru.skillbranch.skillarticles.viewmodels.base.NavigationCommand
import ru.skillbranch.skillarticles.viewmodels.base.Notify

@AndroidEntryPoint
class RootActivity : BaseActivity<RootViewModel>() {

    override val layout = R.layout.activity_root
    public override val viewModel: RootViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // https://developer.android.com/topic/libraries/view-binding?
//        bindRoot = ActivityRootBinding.inflate(layoutInflater)
//        bindBottom = LayoutBottombarBinding.inflate(layoutInflater)

        // top level destination
        val appbarConfiguration = AppBarConfiguration(
            setOf(
                R.id.nav_articles,
                R.id.nav_bookmarks,
                R.id.nav_transcriptions,
                R.id.nav_profile
            )
        )
        setupActionBarWithNavController(navController, appbarConfiguration)

//        nav_view.setupWithNavController(navController) // дефолтная реализация навигации

        // if click on item -> navigate to destination by item id
        nav_view.setOnNavigationItemSelectedListener {
            // Выполняем навигацию по-нашенски. Важно (!), чтобы элементы меню
            // в bottom_nav_menu.xml соответствовали элементам навигации
            // в mobile_navigation.xml (названия их id должны совпадать)
            viewModel.navigate(NavigationCommand.To(it.itemId))
            true
        }

        navController.addOnDestinationChangedListener { _, dest, args ->
            // if destination has changed check whether it is necessary
            // to show nav_auth
            if (viewModel.currentState.isAuth && dest.id == R.id.nav_auth) {
                // Мы уже авторизованы, поэтому вместо экрана авторизации
                // следует показать экран, с которого нас перекинуло на авторизацию
                // и при этом выкинуть из бэкстека контроллера экран авторизации
                val navOptions = NavOptions.Builder()
                    .setPopUpTo(dest.id, true).build()
                // Выполняем навигацию
                viewModel.navigate(
                    NavigationCommand.To(R.id.nav_profile, args, navOptions)
                )
            }
            // set bottom navigation item selected
            nav_view.selectMenuItem(dest)
        }
    }

    override fun renderNotification(notify: Notify) {
        val snackbar = Snackbar.make(
            container,
            notify.message,
            notify.duration
        )
        if (bottombar != null) snackbar.anchorView = bottombar
        else snackbar.anchorView = nav_view


        when (notify) {
            is Notify.TextMessage -> {
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
        // После навигаций по разным фрагментам и возврата на экран статьи
        // нажатие на кнопку Like не ведет к появлению снэкбара, хотя
        // метод snackbar.show() вызывается
        //todo - fix bug
        snackbar.show()
    }

    override fun subscribeOnState(state: IViewModelState) {
        // TODO implement
    }
}
