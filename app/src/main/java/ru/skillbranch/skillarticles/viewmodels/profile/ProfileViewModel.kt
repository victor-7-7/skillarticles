package ru.skillbranch.skillarticles.viewmodels.profile

import androidx.lifecycle.SavedStateHandle
import ru.skillbranch.skillarticles.data.repositories.ProfileRepository
import ru.skillbranch.skillarticles.viewmodels.base.BaseViewModel
import ru.skillbranch.skillarticles.viewmodels.base.IViewModelState

class ProfileViewModel(handle: SavedStateHandle) :
    BaseViewModel<ProfileState>(handle, ProfileState()) {

    private val repository = ProfileRepository

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

}

data class ProfileState(
    val avatar: String? = null,
    val name: String? = null,
    val about: String? = null,
    val rating: Int = 0,
    val respect: Int = 0
) : IViewModelState
