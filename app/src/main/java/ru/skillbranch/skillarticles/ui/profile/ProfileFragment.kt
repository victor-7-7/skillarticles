package ru.skillbranch.skillarticles.ui.profile

import androidx.fragment.app.viewModels
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import kotlinx.android.synthetic.main.fragment_profile.*
import ru.skillbranch.skillarticles.R
import ru.skillbranch.skillarticles.extensions.dpToIntPx
import ru.skillbranch.skillarticles.ui.base.BaseFragment
import ru.skillbranch.skillarticles.ui.base.Binding
import ru.skillbranch.skillarticles.ui.delegates.RenderProp
import ru.skillbranch.skillarticles.viewmodels.base.IViewModelState
import ru.skillbranch.skillarticles.viewmodels.profile.ProfileState
import ru.skillbranch.skillarticles.viewmodels.profile.ProfileViewModel

class ProfileFragment : BaseFragment<ProfileViewModel>() {

    override val viewModel: ProfileViewModel by viewModels()
    override val layout = R.layout.fragment_profile
    override val binding: ProfileBinding by lazy { ProfileBinding() }

    override fun setupViews() {

    }

    private fun updateAvatar(avatarUrl: String) {
        val avatarSize = root.dpToIntPx(168)
        Glide.with(root).load(avatarUrl)
            .apply(RequestOptions.circleCropTransform()).override(avatarSize)
            .into(iv_avatar)
    }

    inner class ProfileBinding : Binding() {
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
        }
    }
}
