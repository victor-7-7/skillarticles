package ru.skillbranch.skillarticles.ui.auth

import androidx.annotation.VisibleForTesting
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.navArgs
import androidx.savedstate.SavedStateRegistryOwner
import kotlinx.android.synthetic.main.fragment_registration.*
import ru.skillbranch.skillarticles.R
import ru.skillbranch.skillarticles.ui.RootActivity
import ru.skillbranch.skillarticles.ui.base.BaseFragment
import ru.skillbranch.skillarticles.viewmodels.auth.AuthViewModel

class RegistrationFragment() : BaseFragment<AuthViewModel>() {
    //------------------------------------------------------------
    /** Only for testing */
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
    //------------------------------------------------------------


    override val viewModel: AuthViewModel by viewModels()
    override val layout = R.layout.fragment_registration
    private val args: RegistrationFragmentArgs by navArgs()

    override fun setupViews() {

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
}