package ru.skillbranch.skillarticles.viewmodels

import androidx.hilt.Assisted
import androidx.hilt.lifecycle.ViewModelInject
import androidx.lifecycle.SavedStateHandle
import ru.skillbranch.skillarticles.R
import ru.skillbranch.skillarticles.data.repositories.RootRepository
import ru.skillbranch.skillarticles.viewmodels.base.BaseViewModel
import ru.skillbranch.skillarticles.viewmodels.base.IViewModelState
import ru.skillbranch.skillarticles.viewmodels.base.NavigationCommand

class RootViewModel @ViewModelInject constructor(
    @Assisted handle: SavedStateHandle,
    rootRepo: RootRepository
) : BaseViewModel<RootState>(handle, RootState()) {
    private val repository = rootRepo
    private val privateRoutes = listOf(R.id.nav_profile)

    init {
        subscribeOnDataSource(repository.isAuth()) { isAuth, state ->
            state.copy(isAuth = isAuth)
        }
    }

    override fun navigate(command: NavigationCommand) {
        when (command) {
            is NavigationCommand.To -> {
                // Если запрошено приватное представление (требующее авторизации)
                // и при этом юзер не авторизован
                if (privateRoutes.contains(command.destination) && !currentState.isAuth) {
                    // то направляем его на страницу авторизации
                    super.navigate(NavigationCommand.StartLogin(command.destination))
                } else {
                    super.navigate(command)
                }
            }
            else -> super.navigate(command)
        }
    }
}

data class RootState(
    val isAuth: Boolean = false
) : IViewModelState
