package ru.skillbranch.skillarticles.ui.profile

import android.content.Context
import android.content.Intent
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.WindowManager
import android.widget.TextView
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts.*
import androidx.annotation.VisibleForTesting
import androidx.core.app.ActivityCompat
import androidx.core.content.FileProvider
import androidx.core.content.res.ResourcesCompat
import androidx.core.net.toUri
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.setFragmentResultListener
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import dagger.hilt.android.AndroidEntryPoint
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

@AndroidEntryPoint
class ProfileFragment : BaseFragment<ProfileViewModel>() {
    /*//------------------------------------------------------------
    */
    /** Only for testing *//*
    private var _mockFactory:
            ((SavedStateRegistryOwner) -> ViewModelProvider.Factory)? = null

    @VisibleForTesting(otherwise = VisibleForTesting.NONE)
    constructor(
        mockRoot: RootActivity,
        testRegistry: ActivityResultRegistry? = null,
        mockFactory: ((SavedStateRegistryOwner) -> ViewModelProvider.Factory)? = null
    ) : this() {
        _mockRoot = mockRoot
        _mockFactory = mockFactory
        if (testRegistry != null) resultRegistry = testRegistry
    }
    //------------------------------------------------------------*/
    companion object {
        const val EDIT_MODE = "EDIT_MODE"
    }

    private var editMode = false

//    private lateinit var resultRegistry: ActivityResultRegistry

    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    lateinit var permissionsLauncher: ActivityResultLauncher<Array<out String>>

    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    lateinit var cameraLauncher: ActivityResultLauncher<Uri>

    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    lateinit var galleryLauncher: ActivityResultLauncher<String>

    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    lateinit var editPhotoLauncher: ActivityResultLauncher<Pair<Uri, Uri>>

    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    lateinit var settingsLauncher: ActivityResultLauncher<Intent>

    /*@VisibleForTesting(otherwise = VisibleForTesting.PROTECTED)
    override val viewModel: ProfileViewModel by viewModels {
        _mockFactory?.invoke(this) ?: defaultViewModelProviderFactory
    }*/
    override val viewModel: ProfileViewModel by activityViewModels()
    override val layout = R.layout.fragment_profile
    override val binding: ProfileBinding by lazy { ProfileBinding() }

    /*
       Логика работы класса следующая. При открытии фрагмента его UI получает
       данные профиля юзера через связывание (binding). Если юзер кликает по
       аватару, то внизу экрана открывается шторка AvatarActionsDialog с
       меню на выбор: взять картинку с камеры (если на девайсе она есть),
       взять с галереи, редактировать текущий аватар (если он есть), удалить
       аватар (если он есть). Когда юзер выбирает пункт меню, шторка закрывается
       и срабатывает слушатель, заданный в методе setFragmentResultListener.
       ------
       Слушатель вызывает соответствующий метод вьюмодели:
       Удалить аватар -> viewModel.handleDeleteAction()
       Взять с галереи -> viewModel.handleGalleryAction()
       Взять с камеры -> viewModel.handleCameraAction(prepareTempUri())
       Редактировать -> viewModel.handleEditAction(sourceUri, prepareTempUri())
       ------
       Методы делают следующее:
       Удалить - адрес картинки удаляется с сервера и из настроек девайса.
       Из галереи / с камеры / редактировать - устанавливается соответствующее
       значение в pendingAction и вызывается requestPermissions()
       ------
       Вызов метода requestPermissions() триггерит (через механизм LiveData)
       запуск permissionsLauncher. Лончер спрашивает у системы - дал ли
       юзер приложению разрешение на чтение/запись во внешнем хранилище?
       Когда система возвращает ответ, срабатывает callbackPermissions(),
       в котором результат модифицируется и вызывается handlePermission()
       ------
       Логика handlePermission(). Если юзер предоставил разрешения, то
       вызывается executePendingAction(). Если юзер не дал разрешения(й)
       и запретил его(их) запрашивать, то вызывается executeOpenSettings().
       Если юзер не дал разрешения(й), но не запрещал его(их) запрашивать
       снова, то ему показывается снэкбар с предложением сделать
       повторный запрос разрешений. Если он соглашается, то делается
       повторный вызов метода requestPermissions().
       Оба метода - executePendingAction() и executeOpenSettings()
       устанавливают pendingAction (второй устанавливает интент
       для открытия системной страницы настроек приложения) и вызывают
       startForResult().
       ------
       Метод startForResult() триггерит (через механизм LiveData) вызов
       слушателя, подписанного на pendingActionLive через метод
       observePendingActions. Слушатель запускает соответсвующий лончер,
       который переключает юзера на взаимодействие со внешним приложением.
       ------
       После возврата из внешнего приложения срабатывает соответствующий
       колбэк - callbackCamera / callbackGallery / callbackEditPhoto или
       callbackSettings. Первые три выгружают полученное новое изображение
       на сервер.
       ------
       Когда с сервера вернется url нового изображения, будет вызван
       метод replaceAvatarUrl() и в настройки приложения запишется profile
       с обновленным строковым полем avatar. Сработает хэндлер
       OnSharedPreferenceChangeListener. Он обновит value для profileLive.
       А на profileLive подписана вьюмодель (через subscribeOnDataSource).
       Следовательно обновится ProfileState вьюмодели. Тогда сработает
       метод bind() объекта binding, который изменит поле avatar, а это
       приведет к вызову метода updateAvatar(), который загрузит в виджет
       iv_avatar фрагмента новую картинку с сервера
    */

    override fun onAttach(context: Context) {
        super.onAttach(context)
        /*if (!::resultRegistry.isInitialized)
            resultRegistry = requireActivity().activityResultRegistry*/

        val resultRegistry = requireActivity().activityResultRegistry

        permissionsLauncher = registerForActivityResult(
            RequestMultiplePermissions(), resultRegistry, ::callbackPermissions
        )
        cameraLauncher = registerForActivityResult(
            TakePicture(), resultRegistry, ::callbackCamera
        )
        galleryLauncher = registerForActivityResult(
            GetContent(), resultRegistry, ::callbackGallery
        )
        editPhotoLauncher = registerForActivityResult(
            EditImageContract(), resultRegistry, ::callbackEditPhoto
        )
        settingsLauncher = registerForActivityResult(
            StartActivityForResult(), resultRegistry, ::callbackSettings
        )
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
        editMode = savedInstanceState?.getBoolean(EDIT_MODE, false) ?: false
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean(EDIT_MODE, editMode)
    }

    override fun setupViews() {
        // Чтобы поле формы, имеющее фокус, не перекрывалось софт-клавиатурой
        root.window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN)

        iv_avatar.setOnClickListener {
            // lecture 12, t.c. 02:04:41
            val action = ProfileFragmentDirections
                .actionNavProfileToDialogAvatarActions(binding.avatar.isNotBlank())
            viewModel.navigate(NavigationCommand.To(action.actionId, action.arguments))
        }

        viewModel.observeRequestedPermissions(viewLifecycleOwner) {
            // Запрашиваем у системы - каково состояние перечисленных в
            // параметре it разрешений для нашего приложения
            permissionsLauncher.launch(it.toTypedArray())
        }

        // Указанный в теле блока Handler будет запущен в результате вызова
        // функции startForResult(). Последняя в свою очередь вызывается из двух
        // мест - из методов executePendingAction() или executeOpenSettings().
        // А они, в свою очередь, (либо тот, либо другой) вызываются из метода
        // вьюмодели - handlePermission(). А этот метод вызывается из тела
        // колбэка callbackPermissions (permissionsResultCallback). А колбэк
        // будет вызван после вызова метода вьюмодели requestPermissions().
        // А запрос разрешений делается из трех мест - из методов вьюмодели
        // handleCameraAction() / handleGalleryAction() / handleEditAction().
        // Итак:
        // handleCameraAction() / handleGalleryAction() / handleEditAction()
        // ==> requestPermissions() ==> launch permissionsLauncher
        // ==> callbackPermissions ==> handlePermission()
        // ==> executePendingAction() или executeOpenSettings()
        // ==> startForResult() триггерит (через механизм LiveData) вызов Handler.
        // Хэндлер запускает соответсвующий лончер, который переключает юзера
        // на взаимодействие со внешним приложением
        viewModel.observePendingActions(viewLifecycleOwner) {
            when (it) {
                // Открываем приложение - галерея
                is PendingAction.GalleryAction -> galleryLauncher.launch(it.payload)
                // Открываем системную страницу настроек приложения
                is PendingAction.SettingsAction -> settingsLauncher.launch(it.payload)
                // Открываем приложение - камера
                is PendingAction.CameraAction -> cameraLauncher.launch(it.payload)
                // Открываем приложение для редактирования изображений
                is PendingAction.EditAction -> editPhotoLauncher.launch(it.payload)
            }
        }

        setupWidgets()

        btn_edit.setOnClickListener {
            editMode = if (editMode) {
                // Профиль находился в режиме редактирования, поэтому
                // сохраняем результат, если он содержателен
                if (et_name.text.toString().isNotBlank()
                    && et_about.text.toString().isNotBlank()
                )
                    viewModel.handleEditProfile(
                        et_name.text.toString(), et_about.text.toString()
                    )
                // Переключаемся в нередактируемый режим
                false
            }
            // Переключаемся в режим редактирования
            else true

            setupWidgets()
        }
    }

    private fun getIcon(): Drawable? =
        if (editMode) {
            ResourcesCompat.getDrawable(
                resources, R.drawable.ic_save_black_24dp, null
            )
        } else {
            ResourcesCompat.getDrawable(
                resources, R.drawable.ic_edit_black_24dp, null
            )
        }

    private fun setupWidgets() {
        et_name.isEnabled = editMode
        et_name.isFocusableInTouchMode = editMode
        et_about.isEnabled = editMode
        et_about.isFocusableInTouchMode = editMode
        if (editMode) {
            et_name.background = ResourcesCompat.getDrawable(
                resources, R.drawable.bg_edit, null
            )
            et_about.background = ResourcesCompat.getDrawable(
                resources, R.drawable.bg_edit, null
            )
        } else {
            et_name.setBackgroundColor(
                resources.getColor(R.color.color_transparent, null)
            )
            et_about.setBackgroundColor(
                resources.getColor(R.color.color_transparent, null)
            )
        }
        btn_edit.setImageDrawable(getIcon())
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
/*
        // FOR TESTING
        if (avatarUrl.isBlank())
            Glide.with(this).load(R.drawable.ic_avatar).into(iv_avatar)
        else
            Glide.with(this).load(avatarUrl)
                .placeholder(R.drawable.ic_avatar)
                .apply(RequestOptions.circleCropTransform()).into(iv_avatar)
*/
    }

    // lecture 12, t.c. 01:28:30
    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    fun prepareTempUri(): Uri {
        // Штамп - для уникальности имени файла
        val timestamp = SimpleDateFormat("HHmmss", Locale.ROOT).format(Date())
        // https://developer.android.com/training/data-storage/use-cases
        // todo: For SDK 29, 30 (android 10, 11) it won't be working
        val storageDir = requireContext().getExternalFilesDir(
            Environment.DIRECTORY_PICTURES
        )
        // Создаем пустой временный файл с файловым дескриптором и уникальным
        // именем, в который (файл) можем записывать и получать его Uri
        val tempFile = File.createTempFile(
            "JPEG_$timestamp",
            ".jpg",
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
            "M_S_ProfileFragment", "prepareTempUri: " +
                    "file uri: ${tempFile.toUri()} content uri: $contentUri"
        )
        // Контентный uri можно передавать во внешние приложения,
        // чтобы те могли использовать файл. Файловый uri в такой
        // ситуации привел бы к runtime error. Разница продемонстрирована
        // в lecture 12, time code 01:34:00
        return contentUri
    }

    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    fun removeTempUri(uri: Uri?) {
        uri ?: return
        requireContext().contentResolver.delete(uri, null, null)
    }


    private fun callbackPermissions(result: MutableMap<String, Boolean>) {
        val permissionsResult = result.mapValues { (perm, isGranted) ->
            // Если в очередной маповской паре (key=perm, value=isGranted)
            // разрешение предоставлено, то меняем в ней значение с единичного
            // на парное (key=perm, value=true)=>(key=perm, value=(true, true))
            if (isGranted) true to true
            // Если разрешение не было предоставлено (key=perm, value=false),
            // то запоминаем - была ли установлена галочка о том, чтобы не
            // спрашивать разрешение у юзера в будущем. Например, если галочка
            // для данного отказного разрешения БЫЛА установлена, то маповская
            // пара будет такой (key=perm, value=(false, FALSE)). А если галочка
            // НЕ БЫЛА установлена, то - (key=perm, value=(false, TRUE))
            else false to ActivityCompat.shouldShowRequestPermissionRationale(
                requireActivity(), perm
            )
        }
        // Если в очередном элементе мапы разрешение предоставлено, то в
        // вальюмной паре первый булеан будет true, иначе - false. Поэтому,
        // если хотя бы один элемент мапы в своей паре содержит первый
        // булеан false, то значит переменная 'все_разрешено' - тоже false
        val isAllGranted = !permissionsResult.values.map { it.first }
            .contains(false)
        // Если какое-то разрешение отсутствует, то берем из pendingAction
        // ранее подготовленный временный контентный Uri и удаляем его
        if (!isAllGranted) {
            val tempUri = when (val pendingAction = binding.pendingAction) {
                is PendingAction.CameraAction -> pendingAction.payload
                is PendingAction.EditAction -> pendingAction.payload.second
                else -> null
            }
            tempUri?.let { removeTempUri(it) }
        }
        viewModel.handlePermission(permissionsResult)
    }

    private fun callbackCamera(hasResult: Boolean) {
        val (payload) = binding.pendingAction as PendingAction.CameraAction
        // Если получили фото от камеры (оно было сохранено по указанному Uri)
        if (hasResult) {
            val inputStream = requireContext().contentResolver.openInputStream(payload)
            // выгружаем это фото на сервер
            viewModel.handleUploadPhoto(inputStream)
        }
        // Не получили фото с камеры, поэтому удаляем Uri
        else removeTempUri(payload)
    }

    private fun callbackGallery(resultUri: Uri?) {
        if (resultUri != null) {
            val inputStream = requireContext().contentResolver.openInputStream(resultUri)
            // Выгружаем фото из галереи на сервер
            viewModel.handleUploadPhoto(inputStream)
        }
    }

    private fun callbackEditPhoto(uri: Uri?) {
        // Если получили отредактированное изображение (по указанному Uri)
        if (uri != null) {
            val inputStream = requireContext().contentResolver.openInputStream(uri)
            // выгружаем это изображение на сервер
            viewModel.handleUploadPhoto(inputStream)
        }
        // Иначе - удаляем ранее подготовленный временный Uri
        else {
            val (payload) = binding.pendingAction as PendingAction.EditAction
            removeTempUri(payload.second)
        }
    }

    private fun callbackSettings(activityResult: ActivityResult?) {
        // todo: do something
    }

    override fun onDestroyView() {
        // Возвращаем SoftInputMode настройку рутовского окна к дефолту
        root.window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING)
        super.onDestroyView()
    }


    inner class ProfileBinding : Binding() {
        var pendingAction: PendingAction? = null

        var avatar: String by RenderProp("") {
            updateAvatar(it)
        }
        var name: String by RenderProp("") {
            et_name.setText(it, TextView.BufferType.EDITABLE)
        }
        var about: String by RenderProp("") {
            et_about.setText(it, TextView.BufferType.EDITABLE)
        }
        private var rating: Int by RenderProp(0) {
            val rat = "Rating: $it"
            tv_rating.text = rat
        }
        private var respect: Int by RenderProp(0) {
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

