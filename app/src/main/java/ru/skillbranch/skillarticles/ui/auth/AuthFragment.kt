package ru.skillbranch.skillarticles.ui.auth

import android.text.Spannable
import androidx.core.text.set
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.navArgs
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.android.synthetic.main.fragment_auth.*
import ru.skillbranch.skillarticles.R
import ru.skillbranch.skillarticles.extensions.attrValue
import ru.skillbranch.skillarticles.ui.base.BaseFragment
import ru.skillbranch.skillarticles.ui.custom.spans.UnderlineSpan
import ru.skillbranch.skillarticles.viewmodels.auth.AuthViewModel
import ru.skillbranch.skillarticles.viewmodels.base.NavigationCommand

@AndroidEntryPoint
class AuthFragment : BaseFragment<AuthViewModel>() {

    override val viewModel: AuthViewModel by activityViewModels()
    override val layout = R.layout.fragment_auth
    private val args: AuthFragmentArgs by navArgs()

    override fun setupViews() {
        btn_login.setOnClickListener {
            viewModel.handleLogin(
                et_login.text.toString(), et_password.text.toString(),
                if (args.privateDestination == -1) null else args.privateDestination
            )
//            val action = AuthFragmentDirections.finishLogin()
//            findNavController().navigate(action)
        }

        tv_register.setOnClickListener {
            val action = AuthFragmentDirections
                .actionAuthFragmentToRegistrationFragment(args.privateDestination)
            // navigate to registration's page
            viewModel.navigate(NavigationCommand.To(action.actionId, action.arguments))
        }

        tv_privacy.setOnClickListener {
            // navigate to privacy policy
            viewModel.navigate(NavigationCommand.To(R.id.page_privacy_policy))
        }

        val color = root.attrValue(R.attr.colorPrimary)

        (tv_privacy.text as Spannable).let { spannable ->
            spannable[0..spannable.length] = UnderlineSpan(color)
        }
        (tv_access_code.text as Spannable).let { spannable ->
            spannable[0..spannable.length] = UnderlineSpan(color)
        }
    }
}
