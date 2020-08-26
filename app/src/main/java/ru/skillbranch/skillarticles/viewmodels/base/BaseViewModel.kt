package ru.skillbranch.skillarticles.viewmodels.base

import android.os.Bundle
import androidx.annotation.UiThread
import androidx.annotation.VisibleForTesting
import androidx.lifecycle.*
import androidx.navigation.NavOptions
import androidx.navigation.Navigator
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.plus
import ru.skillbranch.skillarticles.data.remote.err.ApiError
import ru.skillbranch.skillarticles.data.remote.err.NoNetworkError
import java.net.SocketTimeoutException

abstract class BaseViewModel<T : IViewModelState>(
    private val handleState: SavedStateHandle,
    initState: T
) : ViewModel() {
    @VisibleForTesting(otherwise = VisibleForTesting.PROTECTED)
    val notification = MutableLiveData<Event<Notify>>()

    @VisibleForTesting(otherwise = VisibleForTesting.PROTECTED)
    val navigation = MutableLiveData<Event<NavigationCommand>>()

    private val loading = MutableLiveData<Loading>(Loading.HIDE_LOADING)

    /***
     * Инициализация начального состояния аргументом конструктора, и объявление
     * состояния как MediatorLiveData - медиатор исспользуется для того, чтобы
     * учитывать изменяемые данные модели и обновлять состояние ViewModel,
     * исходя из полученных данных
     */
    @VisibleForTesting(otherwise = VisibleForTesting.PROTECTED)
    val state: MediatorLiveData<T> = MediatorLiveData<T>().apply {
        value = initState
    }

    /***
     * Геттер для получения not null значения текущего состояния ViewModel
     */
    @VisibleForTesting(otherwise = VisibleForTesting.PROTECTED)
    val currentState
        get() = state.value!!


    /***
     * Лямбда-выражение, принимающее в качестве аргумента текущее состояние и
     * возвращающее модифицированное состояние, которое присваивается
     * текущему состоянию
     */
    @UiThread
    protected inline fun updateState(update: (currentState: T) -> T) {
        val updatedState: T = update(currentState)
        state.value = updatedState
    }

    /***
     * Функция для создания уведомления пользователя о событии (событие
     * обрабатывается только один раз), соответсвенно при изменении конфигурации
     * и пересоздании Activity уведомление не будет вызвано повторно
     */
    @UiThread
    protected fun notify(content: Notify) {
        notification.value = Event(content)
    }

    @UiThread
    open fun navigate(command: NavigationCommand) {
        navigation.value = Event(command)
    }

    /**
     * Отображение индикатора загрузки. По умолчанию - не блокирующая загрузка
     * */
    protected fun setLoading(loadingType: Loading = Loading.SHOW_LOADING) {
        loading.value = loadingType
    }

    /**
     * Скрытие индикатора загрузки
     * */
    protected fun hideLoading() {
        loading.value = Loading.HIDE_LOADING
    }

    /**
     * Более компактная форма записи observe() метода LiveData. Функция принимает
     * последним аргументом лямбда-выражение, обрабатывающее изменившееся состояние
     * индикатора загрузки
     */
    fun observeLoading(owner: LifecycleOwner, onChanged: (newState: Loading) -> Unit) {
        loading.observe(owner, Observer { onChanged(it!!) })
    }

    /**
     * Более компактная форма записи observe() метода LiveData. Функция принимает
     * последним аргументом лямбда-выражение, обрабатывающее изменившееся состояние
     */
    fun observeState(owner: LifecycleOwner, onChanged: (newState: T) -> Unit) {
        state.observe(owner, Observer { onChanged(it!!) })
    }

    /***
     * Более компактная форма записи observe() метода LiveData, вызывает лямбду
     * обработчик только в том случае, если уведомление не было уже обработанно
     * ранее, реализует данное поведение с помощью EventObserver
     */
    fun observeNotification(
        owner: LifecycleOwner,
        onNotify: (notification: Notify) -> Unit
    ) {
        notification.observe(owner, EventObserver {
            onNotify(it)
        })
    }

    fun observeNavigation(
        owner: LifecycleOwner,
        onNavigate: (command: NavigationCommand) -> Unit
    ) {
        navigation.observe(owner, EventObserver {
            onNavigate(it)
        })
    }

    /***
     * Функция принимает источник данных и лямбду, обрабатывающую изменяющиеся
     * данные источника. В лямбду передаются изменившиеся данные и текущее
     * состояние ViewModel в качестве аргументов. Лямбда изменяет состояние
     * ViewModel и возвращает это состояние, установив его как текущее.
     */
    protected fun <S> subscribeOnDataSource(
        source: LiveData<S>,
        onChanged: (newValue: S, currentState: T) -> T?
    ) {
        state.addSource(source) {
            state.value = onChanged(it, currentState) ?: return@addSource
        }
    }

    fun saveState() {
        currentState.save(handleState)
    }

    @Suppress("UNCHECKED_CAST")
    fun restoreState() {
        val restoredState = currentState.restore(handleState) as T
        if (currentState == restoredState) return
        state.value = restoredState
    }

    // look at video (lecture 11, time codes 01:17:50 & 02:03:17)
    protected fun launchSafety(
        // on error
        errHandler: ((Throwable) -> Unit)? = null,
        // on complete
        complHandler: ((Throwable?) -> Unit)? = null,
        // payload coroutine
        payloadBlock: suspend CoroutineScope.() -> Unit
    ) {
        val errHand = CoroutineExceptionHandler { _, err ->
            errHandler?.invoke(err) ?: when (err) {
                is NoNetworkError -> notify(
                    Notify.TextMessage(
                        "Network not available, check internet connection"
                    )
                )
                is SocketTimeoutException -> notify(Notify.ActionMessage(
                    "Network timeout exception - please try again",
                    "Retry"
                ) {
                    launchSafety(errHandler, complHandler, payloadBlock)
                })
                is ApiError.InternalServerError -> notify(Notify.ErrorMessage(
                    err.message, "Retry"
                ) {
                    launchSafety(errHandler, complHandler, payloadBlock)
                })
                is ApiError -> notify(Notify.ErrorMessage(err.message))
                else -> notify(
                    Notify.ErrorMessage(
                        err.message ?: "Something went wrong"
                    )
                )
            }
        }
        (viewModelScope + errHand).launch {
            // отобразить неблокирующий прогресс-бар
            setLoading()
            // выполнить в фоне полезную работу
            payloadBlock()
            // после завершения фоновой задачи
        }.invokeOnCompletion {
            // скрыть прогресс-бар
            hideLoading()
            // выполнить (если имеется) завершающую работу
            complHandler?.invoke(it)
        }
    }
}

class Event<out E>(private val content: E) {
    private var hasBeenHandled = false

    /***
     * Возвращает контент, который еще не был обработан, иначе null
     */
    fun getContentIfNotHandled(): E? {
        return if (hasBeenHandled) null
        else {
            hasBeenHandled = true
            content
        }
    }
    fun peekContent(): E = content
}

/***
 * В качестве аргумента конструктора класс-обзервер события принимает лямбду,
 * которая будет вызвана только если это событие произошло в первый раз
 */
class EventObserver<E>(
    private val onEventUnhandledContent: (E) -> Unit
) : Observer<Event<E>> {

    override fun onChanged(event: Event<E>?) {
        event?.getContentIfNotHandled()?.let {
            onEventUnhandledContent(it)
        }
    }
}

sealed class Notify {
    abstract val message: String

    data class TextMessage(override val message: String) : Notify()

    data class ActionMessage(
        override val message: String,
        val actionLabel: String,
        val actionHandler: (() -> Unit)?
    ) : Notify()

    data class ErrorMessage(
        override val message: String,
        val errLabel: String? = null,
        val errHandler: (() -> Unit)? = null
    ) : Notify()
}

sealed class NavigationCommand {

    data class To(
        val destination: Int,
        val args: Bundle? = null,
        val options: NavOptions? = null,
        val extras: Navigator.Extras? = null
    ) : NavigationCommand()

    data class StartLogin(
        val privateDestination: Int? = null
    ) : NavigationCommand()

    data class FinishLogin(
        val privateDestination: Int? = null
    ) : NavigationCommand()
}

enum class Loading {
    SHOW_LOADING, SHOW_BLOCKING_LOADING, HIDE_LOADING
}




