package ru.skillbranch.skillarticles.ui.auth

import androidx.fragment.app.viewModels
import androidx.navigation.fragment.navArgs
import kotlinx.android.synthetic.main.fragment_registration.*
import ru.skillbranch.skillarticles.R
import ru.skillbranch.skillarticles.extensions.isValidEmail
import ru.skillbranch.skillarticles.extensions.isValidPassword
import ru.skillbranch.skillarticles.ui.base.BaseFragment
import ru.skillbranch.skillarticles.viewmodels.auth.AuthViewModel

class RegistrationFragment : BaseFragment<AuthViewModel>() {

    override val viewModel: AuthViewModel by viewModels()
    override val layout = R.layout.fragment_registration
    private val args: RegistrationFragmentArgs by navArgs()

    override fun setupViews() {

        btn_register.setOnClickListener {
            val name = et_name.text.toString() + ' ' + et_surname.text.toString()
            val email = et_email.text.toString()
            val pass = et_password.text.toString()
            // Сплошные пробелы вместо имени тоже отбрасываем
            if (name.isBlank() || name.length < 4) {
                viewModel.handleAlert(
                    "Имя пользователя должно состоять из трех " +
                            "или более непробельных символов"
                )
                return@setOnClickListener
            }
            if (!email.isValidEmail()) {
                viewModel.handleAlert(
                    "Email адрес задан неверно"
                )
                return@setOnClickListener
            }
            if (!pass.isValidPassword()) {
                viewModel.handleAlert(
                    "Пароль должен состоять из 8 или более букв и цифр"
                )
                return@setOnClickListener
            }

            viewModel.handleRegister(
                name, email, pass,
                if (args.privateDestination == -1) null
                else args.privateDestination
            )
        }
    }
}