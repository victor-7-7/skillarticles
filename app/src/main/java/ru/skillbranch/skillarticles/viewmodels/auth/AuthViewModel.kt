package ru.skillbranch.skillarticles.viewmodels.auth

import androidx.hilt.Assisted
import androidx.hilt.lifecycle.ViewModelInject
import androidx.lifecycle.SavedStateHandle
import ru.skillbranch.skillarticles.data.repositories.RootRepository
import ru.skillbranch.skillarticles.extensions.isValidEmail
import ru.skillbranch.skillarticles.extensions.isValidPassword
import ru.skillbranch.skillarticles.viewmodels.base.BaseViewModel
import ru.skillbranch.skillarticles.viewmodels.base.IViewModelState
import ru.skillbranch.skillarticles.viewmodels.base.NavigationCommand
import ru.skillbranch.skillarticles.viewmodels.base.Notify

class AuthViewModel @ViewModelInject constructor(
    @Assisted handle: SavedStateHandle,
    private val repository: RootRepository
) : BaseViewModel<AuthState>(handle, AuthState()), IAuthViewModel {

    init {
        subscribeOnDataSource(repository.isAuth()) { isAuth, state ->
            state.copy(isAuth = isAuth)
        }
    }

    override fun handleLogin(login: String, pass: String, dest: Int?) {
        launchSafety {
            repository.login(login, pass)
            navigate(NavigationCommand.FinishLogin(dest))
        }
    }

    override fun handleRegister(name: String, email: String, pass: String, dest: Int?) {

        if (name.isBlank() || name.length < 4) {
            handleAlert(
                "Имя пользователя должно состоять из трех " +
                        "или более непробельных символов"
            )
            return
        }
        if (!email.isValidEmail()) {
            handleAlert(
                "Email адрес задан неверно"
            )
            return
        }
        if (!pass.isValidPassword()) {
            handleAlert(
                "Пароль должен состоять из 8 или более букв и цифр"
            )
            return
        }

        launchSafety {
            repository.register(name, email, pass)
            navigate(NavigationCommand.FinishLogin(dest))
        }
    }

    private fun handleAlert(alert: String) {
        notify(Notify.ErrorMessage(alert))
    }
}

data class AuthState(
    val isAuth: Boolean = false
) : IViewModelState