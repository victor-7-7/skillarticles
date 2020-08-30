package ru.skillbranch.skillarticles.viewmodels.profile

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Parcel
import android.os.Parcelable
import android.provider.Settings
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import kotlinx.android.parcel.Parcelize
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import ru.skillbranch.skillarticles.data.repositories.ProfileRepository
import ru.skillbranch.skillarticles.viewmodels.base.*
import java.io.InputStream

class ProfileViewModel(handle: SavedStateHandle) :
    BaseViewModel<ProfileState>(handle, ProfileState()) {

    private val repository = ProfileRepository
    private val storagePermissions = listOf(
        Manifest.permission.WRITE_EXTERNAL_STORAGE,
        Manifest.permission.READ_EXTERNAL_STORAGE
    )
    private val activityResults = MutableLiveData<Event<PendingAction>>()

    init {
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

    private fun startForResult(action: PendingAction) {
        activityResults.value = Event(action)
    }

    // lecture 12, t.c. 01:12:00
    fun handlePermission(permissionsResult: Map<String, Pair<Boolean, Boolean>>) {
        val isAllGranted = !permissionsResult.values.map { it.first }
            .contains(false)
        val isAllMayBeShown = !permissionsResult.values.map { it.second }
            .contains(false)
        when {
            isAllGranted -> executePendingAction()
            // if request permission not may be shown (don't ask again check)
            // show app settings for manual permission
            !isAllMayBeShown -> executeOpenSettings()
            // else retry request permission
            else -> {
                val msg = Notify.ErrorMessage(
                    "Need permissions for storage",
                    "Retry"
                ) { requestPermissions(storagePermissions) }
                notify(msg)
            }
        }
    }

    private fun executePendingAction() {
        val pendingAction = currentState.pendingAction ?: return
        startForResult(pendingAction)
    }

    private fun executeOpenSettings() {
        val errHandler = {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
            intent.data = Uri.parse("package:ru.skillbranch.skillarticles")
            startForResult(PendingAction.SettingsAction(intent))
        }
        notify(
            Notify.ErrorMessage(
                "Need permissions for storage",
                "Open settings",
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

    // Для тестов
    fun handleTestAction(source: Uri, destination: Uri) {
/*
        // lecture 12, time code 01:09:38
        // Если бы нам надо было выбрать из галереи изображения
        // с расширением jpeg
        val pendingAction = PendingAction.GalleryAction("image/jpeg")
*/ /*
        // lecture 12, time code 01:33:27
        // Выбираем файл из хранилища по указанному uri
         val pendingAction = PendingAction.CameraAction(uri)
*/
        // lecture 12, time code 01:37:50
        val pendingAction = PendingAction.EditAction(source to destination)

        updateState { it.copy(pendingAction = pendingAction) }
        requestPermissions(storagePermissions)
    }

    fun observeActivityResults(
        owner: LifecycleOwner,
        handle: (action: PendingAction) -> Unit
    ) {
        activityResults.observe(owner, EventObserver { handle(it) })
    }

    fun handleCameraAction(destination: Uri) {
        updateState {
            it.copy(pendingAction = PendingAction.CameraAction(destination))
        }
        requestPermissions(storagePermissions)
    }

    fun handleGalleryAction() {
        updateState {
            it.copy(pendingAction = PendingAction.GalleryAction("image/jpeg"))
        }
        requestPermissions(storagePermissions)
    }

    fun handleDeleteAction() {
        // todo: implement (Lecture 12, 02:05:20)
        // Вызвать метод репозитория, который удалит аватар на сервере
    }

    fun handleEditAction(source: Uri, destination: Uri) {
        val pendingAction = PendingAction.EditAction(source to destination)
        updateState { it.copy(pendingAction = pendingAction) }
        requestPermissions(storagePermissions)
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
    data class EditAction(override val payload: Pair<Uri, Uri>) : PendingAction(), Parcelable {
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
