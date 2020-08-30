package ru.skillbranch.skillarticles.ui.profile

import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.util.Log
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import androidx.fragment.app.setFragmentResultListener
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import kotlinx.android.synthetic.main.fragment_profile.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ru.skillbranch.skillarticles.R
import ru.skillbranch.skillarticles.extensions.dpToIntPx
import ru.skillbranch.skillarticles.ui.base.BaseFragment
import ru.skillbranch.skillarticles.ui.base.Binding
import ru.skillbranch.skillarticles.ui.delegates.RenderProp
import ru.skillbranch.skillarticles.ui.dialogs.AvatarActionsDialog
import ru.skillbranch.skillarticles.viewmodels.base.IViewModelState
import ru.skillbranch.skillarticles.viewmodels.base.NavigationCommand
import ru.skillbranch.skillarticles.viewmodels.profile.PendingAction
import ru.skillbranch.skillarticles.viewmodels.profile.ProfileState
import ru.skillbranch.skillarticles.viewmodels.profile.ProfileViewModel
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class ProfileFragment : BaseFragment<ProfileViewModel>() {

    override val viewModel: ProfileViewModel by viewModels()
    override val layout = R.layout.fragment_profile
    override val binding: ProfileBinding by lazy { ProfileBinding() }

    private val permissionsResultCallback = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        // look at video (lecture 12, time code 00:58:11)
        val permissionsResult = result.mapValues { (perm, isGranted) ->
            // Если в очередной маповской паре (key=perm, value=isGranted)
            // разрешение предоставлено, то меняем в ней значение с единичного
            // на парное (key=perm, value=true)=>(key=perm, value=(true, true))
            if (isGranted) true to true
            // Если разрешение не было предоставлено (key=perm, value=false),
            // то запоминаем - была ли установлена галочка о том, чтобы не
            // спрашивать разрешение у юзера в будущем. Например, если галочка
            // для данного отказного разрешения была установлена, то маповская
            // пара будет такой (key=perm, value=(false, false)). А если галочка
            // не была установлена, то - (key=perm, value=(false, true))
            else false to ActivityCompat.shouldShowRequestPermissionRationale(
                requireActivity(), perm
            )
        }
        viewModel.handlePermission(permissionsResult)
        /*       Log.d("M_ProfileFragment",
                   "Request runtime permissions result: $result")*/
    }

    private val galleryResultCallback = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { result ->
        if (result != null) {
            val inputStream = requireContext().contentResolver.openInputStream(result)
            // Выгружаем фото из галереи на сервер
            viewModel.handleUploadPhoto(inputStream)
        }
    }

    private val settingResultCallback = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        // todo: do something with result after closing settings activity
    }

    // lecture 12, t.c. 01:26:26
    private val cameraResultCallback = registerForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { isSaved ->
        val (payload) = binding.pendingAction as PendingAction.CameraAction
        // Если получили фото от камеры (оно было сохранено по указанному Uri)
        if (isSaved) {
            val inputStream = requireContext().contentResolver.openInputStream(payload)
            // выгружаем это фото на сервер
            viewModel.handleUploadPhoto(inputStream)
        }
        // Фото не было сохранено в надлежащее место, поэтому удаляем Uri
        else removeTempUri(payload)
    }

    // lecture 12, t.c. 01:42:10
    private val editPhotoResultCallback = registerForActivityResult(
        EditImageContract()
    ) { uri ->
        // Если получили отредактированное изображение (по указанному Uri)
        if (uri != null) {
            val inputStream = requireContext().contentResolver.openInputStream(uri)
            // выгружаем это изображение на сервер
            viewModel.handleUploadPhoto(inputStream)
        }
        // Иначе удаляем Uri
        else {
            val (payload) = binding.pendingAction as PendingAction.EditAction
            removeTempUri(payload.second)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // listen dialog fragment result
        setFragmentResultListener(AvatarActionsDialog.AVATAR_ACTIONS_KEY) { _, bundle ->
            when (bundle[AvatarActionsDialog.SELECT_ACTION_KEY] as String) {

                AvatarActionsDialog.CAMERA_KEY ->
                    viewModel.handleCameraAction(prepareTempUri())

                AvatarActionsDialog.GALLERY_KEY -> viewModel.handleGalleryAction()

                AvatarActionsDialog.DELETE_KEY -> viewModel.handleDeleteAction()

                AvatarActionsDialog.EDIT_KEY -> {
                    // lecture 12, t.c. 02:03:30
                    lifecycleScope.launch(Dispatchers.IO) {
                        // Возьмем необработанный файл из кэша глайда
                        val sourceFile = Glide.with(requireActivity()).asFile()
                            .load(binding.avatar).submit().get()
                        // Получим его content Uri
                        val sourceUri = FileProvider.getUriForFile(
                            requireContext(),
                            // Полномочия идентичны указанным в провайдере манифеста
                            "${requireContext().packageName}.provider",
                            sourceFile
                        )
                        withContext(Dispatchers.Main) {
                            // Метод берет файл из sourceUri, редактирует его и
                            // сохраняет в файл с uri
                            viewModel.handleEditAction(sourceUri, prepareTempUri())
                        }
                    }
                }
            }
        }
    }

    override fun setupViews() {
        iv_avatar.setOnClickListener {
            // lecture 12, t.c. 02:04:41
            val action = ProfileFragmentDirections
                .actionNavProfileToDialogAvatarActions(binding.avatar.isNotBlank())
            viewModel.navigate(NavigationCommand.To(action.actionId, action.arguments))
/*
            // ТЕСТОВЫЙ БЛОК
            // lecture 12, t.c. 01:35:35. Don't call on UI thread
            lifecycleScope.launch(Dispatchers.IO) {
                // Возьмем необработанный файл из кэша глайда
                val sourceFile = Glide.with(requireActivity()).asFile()
                    .load(binding.avatar).submit().get()
                // Получим его content Uri
                val sourceUri = FileProvider.getUriForFile(
                    requireContext(),
                    // Полномочия идентичны указанным в провайдере манифеста
                    "${requireContext().packageName}.provider",
                    sourceFile
                )
                Log.d("M_ProfileFragment", "setupViews: " +
                        "edit image - glide cache uri: ${sourceFile.toUri()} " +
                        "content source uri: $sourceUri")
                // Подготовим ссылку, по которой будем сохранять обработанный файл
                val uri = prepareTempUri()

                withContext(Dispatchers.Main) {
                    // Метод берет файл из sourceUri, редактирует его и
                    // сохраняет в файл с uri
                    viewModel.handleTestAction(sourceUri, uri)
                }
            }
*/
        }
        viewModel.observePermissions(viewLifecycleOwner) {
            // launch callback for request permissions
            permissionsResultCallback.launch(it.toTypedArray())
        }
        viewModel.observeActivityResults(viewLifecycleOwner) {
            when (it) {
                is PendingAction.GalleryAction -> galleryResultCallback.launch(it.payload)
                is PendingAction.SettingsAction -> settingResultCallback.launch(it.payload)
                is PendingAction.CameraAction -> cameraResultCallback.launch(it.payload)
                is PendingAction.EditAction -> editPhotoResultCallback.launch(it.payload)
            }
        }
    }

    private fun updateAvatar(avatarUrl: String) {
        val avatarSize = root.dpToIntPx(168)
        if (avatarUrl.isBlank())
            Glide.with(root).load(R.drawable.ic_avatar)
                .apply(RequestOptions.circleCropTransform()).override(avatarSize)
                .into(iv_avatar)
        else
            Glide.with(root).load(avatarUrl).placeholder(R.drawable.ic_avatar)
                .apply(RequestOptions.circleCropTransform()).override(avatarSize)
                .into(iv_avatar)
    }

    // lecture 12, t.c. 01:28:30
    private fun prepareTempUri(): Uri {
        // Штамп - для уникальности имени файла
        val timestamp = SimpleDateFormat("HHmmss", Locale.ROOT).format(Date())
        val storageDir = requireContext().getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        // Создаем пустой временный файл с файловым дескриптором и уникальным
        // именем, в который (файл) можем записывать и получать его Uri
        val tempFile = File.createTempFile(
            "JPEG_$timestamp",
            "jpg",
            storageDir
        )
        // Return content: Uri (not file: Uri)
        val contentUri = FileProvider.getUriForFile(
            requireContext(),
            // Полномочия идентичны указанным в провайдере манифеста
            "${requireContext().packageName}.provider",
            tempFile
        )
        Log.d(
            "M_ProfileFragment", "prepareTempUri: " +
                    "file uri: ${tempFile.toUri()} content uri: $contentUri"
        )
        // Контентный uri можно передавать во внешние приложения,
        // чтобы те могли использовать файл. Файловый uri в такой
        // ситуации привел бы к runtime error. Разница продемонстрирована
        // в lecture 12, time code 01:34:00
        return contentUri
    }

    private fun removeTempUri(uri: Uri) {
        requireContext().contentResolver.delete(uri, null, null)
    }

    inner class ProfileBinding : Binding() {
        var pendingAction: PendingAction? = null

        var avatar: String by RenderProp("") {
            updateAvatar(it)
        }
        var name: String by RenderProp("") {
            tv_name.text = it
        }
        var about: String by RenderProp("") {
            tv_about.text = it
        }
        var rating: Int by RenderProp(0) {
            val rat = "Rating: $it"
            tv_rating.text = rat
        }
        var respect: Int by RenderProp(0) {
            val res = "Respect: $it"
            tv_respect.text = res
        }

        override fun bind(data: IViewModelState) {
            data as ProfileState
            avatar = data.avatar ?: ""
            name = data.name ?: ""
            about = data.about ?: ""
            rating = data.rating
            respect = data.respect
            pendingAction = data.pendingAction
        }
    }
}
