package ru.skillbranch.skillarticles.ui.auth

import android.view.WindowManager
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.navArgs
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.android.synthetic.main.fragment_registration.*
import ru.skillbranch.skillarticles.R
import ru.skillbranch.skillarticles.ui.base.BaseFragment
import ru.skillbranch.skillarticles.viewmodels.auth.AuthViewModel

@AndroidEntryPoint
class RegistrationFragment : BaseFragment<AuthViewModel>() {
    /*//------------------------------------------------------------
    */
    /** Only for testing *//*
    private var _mockFactory:
            ((SavedStateRegistryOwner) -> ViewModelProvider.Factory)? = null

    @VisibleForTesting(otherwise = VisibleForTesting.NONE)
    constructor(
        mockRoot: RootActivity,
        mockFactory: ((SavedStateRegistryOwner) -> ViewModelProvider.Factory)? = null
    ) : this() {
        _mockRoot = mockRoot
        _mockFactory = mockFactory
    }
    //------------------------------------------------------------*/

    override val viewModel: AuthViewModel by activityViewModels()
    override val layout = R.layout.fragment_registration
    private val args: RegistrationFragmentArgs by navArgs()

    override fun setupViews() {
        // Чтобы поле формы, имеющее фокус, не перекрывалось софт-клавиатурой
        root.window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN)

        btn_register.setOnClickListener {
            val name = et_name.text.toString() + ' ' + et_surname.text.toString()
            val email = et_email.text.toString()
            val pass = et_password.text.toString()

            viewModel.handleRegister(
                name, email, pass,
                if (args.privateDestination == -1) null
                else args.privateDestination
            )
        }
    }

    override fun onDestroyView() {
        // Возвращаем SoftInputMode настройку рутовского окна к дефолту
        root.window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING)
        super.onDestroyView()
    }
}