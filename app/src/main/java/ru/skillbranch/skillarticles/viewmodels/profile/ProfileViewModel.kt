package ru.skillbranch.skillarticles.viewmodels.profile

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Parcel
import android.os.Parcelable
import android.provider.Settings
import androidx.annotation.VisibleForTesting
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.android.parcel.Parcelize
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import ru.skillbranch.skillarticles.data.repositories.ProfileRepository
import ru.skillbranch.skillarticles.viewmodels.base.*
import java.io.InputStream
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    handle: SavedStateHandle,
    private val repository: ProfileRepository
) : BaseViewModel<ProfileState>(handle, ProfileState()) {

    private val storagePermissions = listOf(
        Manifest.permission.READ_EXTERNAL_STORAGE,
        Manifest.permission.WRITE_EXTERNAL_STORAGE
    )
    private val pendingActionLive = MutableLiveData<Event<PendingAction>>()

    init {
        // Здесь мы подписываемся на прослушивание изменений только
        // в объекте LiveData<User?> (profile). При изменении поля
        // pendingAction нашего стейта указанная здесь лямбда не сработает
        subscribeOnDataSource(repository.getProfile()) { profile, state ->
            profile ?: return@subscribeOnDataSource null
            state.copy(
                avatar = profile.avatar,
                name = profile.name,
                about = profile.about,
                rating = profile.rating,
                respect = profile.respect
            )
        }
    }

    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    fun startForResult(action: PendingAction) {
        pendingActionLive.value = Event(action)
    }

    // lecture 12, t.c. 01:12:00
    fun handlePermission(
        permissionsResult: Map<String, Pair<Boolean, Boolean>>
    ) {
        // Если в очередном элементе мапы разрешение предоставлено, то в
        // вальюмной паре первый булеан будет true, иначе - false. Поэтому,
        // если хотя бы один элемент мапы в своей паре содержит первый
        // булеан false, то значит переменная 'все_разрешено' - тоже false
        val isAllGranted = !permissionsResult.values.map { it.first }
            .contains(false)
        // Если в очередном элементе мапы для отказного разрешения есть
        // запрет напоминания о разрешении (юзер чекнул - don't ask me again),
        // то вальюмная пара будет (false,FALSE), иначе - (false, TRUE).
        // Поэтому, если хотя бы один элемент мапы в своей паре содержит второй
        // булеан false, то значит переменная 'любое_отказное_разрешение
        // _может_быть_показано_юзеру_как_предложение_его_изменить' - тоже false
        val isAllMayBeShown = !permissionsResult.values.map { it.second }
            .contains(false)
        when {
            // Если все разрешения предоставлены (чтение/запись файла), то
            // выполняем действие
            isAllGranted -> executePendingAction()
            // if request permission can not be shown ("don't ask again" checked)
            // show app settings for manual permission
            !isAllMayBeShown -> executeOpenSettings()
            // Юзер не дал разрешение(й), но не запретил их запрашивать снова
            // retry request permission
            else -> {
                val msg = Notify.ErrorMessage(
                    "Need permissions for storage",
                    "Retry",
                    5000
                ) { requestPermissions(storagePermissions) }
                notify(msg)
            }
        }
    }

    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    fun executePendingAction() {
        // Если отложенное действие во вьюмодел стейте null, то выходим
        val pendingAction = currentState.pendingAction ?: return
        // Запускаем отложенное действие
        startForResult(pendingAction)
    }

    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    fun executeOpenSettings() {
        // Готовим хэндлер
        val errHandler = {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
            intent.data = Uri.parse("package:ru.skillbranch.skillarticles")
            // Интент откроет системную страницу настроек данного приложения
            startForResult(PendingAction.SettingsAction(intent))
        }
        // Предлагаем юзеру открыть настройки, чтобы задать разрешения
        notify(
            Notify.ErrorMessage(
                "Need permissions for storage",
                "Open settings",
                5000,
                errHandler
            )
        )
    }

    fun handleUploadPhoto(inputStream: InputStream?) {
        inputStream ?: return // or show error notification
        launchSafety(null, {
            updateState { it.copy(pendingAction = null) }
        }) {
            // Read file stream on background thread IO
            val byteArray = withContext(Dispatchers.IO) {
                inputStream.use { input -> input.readBytes() }
            }
            val reqFile = byteArray.toRequestBody("image/jpeg".toMediaType())
            val reqPart: MultipartBody.Part = MultipartBody.Part.createFormData(
                name = "avatar",
                filename = "avatar.ipg",
                body = reqFile
            )
            repository.uploadAvatar(reqPart)
        }
    }

    // ТЕСТОВЫЙ МЕТОД
    fun handleTestAction(source: Uri, destination: Uri) {
/*
        // lecture 12, time code 01:09:38
        // Если бы нам надо было выбрать из галереи изображения
        // с расширением jpeg
        val pendingAction = PendingAction.GalleryAction("image/jpeg")
*//*
        // lecture 12, time code 01:33:27
        // Выбираем файл из хранилища по указанному uri
         val pendingAction = PendingAction.CameraAction(uri)
*//*
        // lecture 12, time code 01:37:50
        val pendingAction = PendingAction.EditAction(source to destination)

        updateState { it.copy(pendingAction = pendingAction) }
        requestPermissions(storagePermissions)
        */
    }

    // Здесь мы подписываемся на прослушивание изменений только
    // объекта Event<PendingAction>, инкапсулированного в переменной
    // pendingActionLive (MutableLiveData<Event<PendingAction>>).
    // Указанная здесь лямбда сработает лишь при вызове метода
    // startForResult (там задается новое значение для pendingActionLive.value)
    fun observePendingActions(
        owner: LifecycleOwner,
        handlePendingAction: (action: PendingAction) -> Unit
    ) {
        pendingActionLive.observe(owner, EventObserver { handlePendingAction(it) })
    }

    fun handleCameraAction(destination: Uri) {
        // Меняем поле pendingAction в стейте вьюмодел (ProfileState).
        // При этом никакие обработчики не будут вызваны
        updateState {
            it.copy(pendingAction = PendingAction.CameraAction(destination))
        }
        requestPermissions(storagePermissions)
    }

    fun handleGalleryAction() {
        // Меняем поле pendingAction в стейте вьюмодел (ProfileState).
        // При этом никакие обработчики не будут вызваны
        updateState {
            it.copy(pendingAction = PendingAction.GalleryAction("image/jpeg"))
        }
        requestPermissions(storagePermissions)
    }

    fun handleDeleteAction() {
        launchSafety { repository.removeAvatar() }
    }

    fun handleEditAction(source: Uri, destination: Uri) {
        val pendingAction = PendingAction.EditAction(source to destination)
        // Меняем поле pendingAction в стейте вьюмодел (ProfileState).
        // При этом никакие обработчики не будут вызваны
        updateState { it.copy(pendingAction = pendingAction) }
        requestPermissions(storagePermissions)
    }

    fun handleEditProfile(name: String, about: String) {
        launchSafety { repository.editProfile(name, about) }
    }
}

data class ProfileState(
    val avatar: String? = null,
    val name: String? = null,
    val about: String? = null,
    val rating: Int = 0,
    val respect: Int = 0,
    val pendingAction: PendingAction? = null
) : IViewModelState {
    override fun save(outState: SavedStateHandle) {
        outState.set("pendingAction", pendingAction)
    }

    override fun restore(savedState: SavedStateHandle): IViewModelState {
        return copy(pendingAction = savedState["pendingAction"])
    }
}

sealed class PendingAction : Parcelable {
    abstract val payload: Any?

    @Parcelize
    data class GalleryAction(override val payload: String) : PendingAction()

    @Parcelize
    data class SettingsAction(override val payload: Intent) : PendingAction()

    @Parcelize
    data class CameraAction(override val payload: Uri) : PendingAction()

    // @Parcelize - не справляется корректно с объектом типа Pair
    data class EditAction(
        override val payload: Pair<Uri, Uri>
    ) : PendingAction(), Parcelable {

        constructor(parcel: Parcel) : this(
            Uri.parse(parcel.readString())
                    to Uri.parse(parcel.readString())
        )

        override fun writeToParcel(parcel: Parcel, flags: Int) {
            parcel.writeString(payload.first.toString())
            parcel.writeString(payload.second.toString())
        }

        override fun describeContents() = 0

        companion object CREATOR : Parcelable.Creator<EditAction> {
            override fun createFromParcel(parcel: Parcel) = EditAction(parcel)
            override fun newArray(size: Int): Array<EditAction?> = arrayOfNulls(size)
        }
    }
}
