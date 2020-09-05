package ru.skillbranch.skillarticles.viewmodels.transcriptions

import androidx.lifecycle.SavedStateHandle
import ru.skillbranch.skillarticles.data.local.PrefManager
import ru.skillbranch.skillarticles.data.repositories.ProfileRepository
import ru.skillbranch.skillarticles.ui.RootActivity
import ru.skillbranch.skillarticles.viewmodels.base.BaseViewModel
import ru.skillbranch.skillarticles.viewmodels.base.IViewModelState

class TranscriptionsViewModel(handle: SavedStateHandle) :
    BaseViewModel<TranscriptionsState>(handle, TranscriptionsState()) {

    fun resetAllPreferences(root: RootActivity) {
        val isPrefsDefault = with(PrefManager) {
            profile?.avatar == null && !isDarkMode && !isBigText
                    && accessToken.isEmpty() && refreshToken.isEmpty()
        }
        if (isPrefsDefault) return

        launchSafety {
            // Убираем аватар юзер с сервера и из настроек
            if (PrefManager.profile?.avatar != null)
                ProfileRepository.removeAvatar()
            // Делаем ресет остальных настроек на девайсе
            PrefManager.resetAllPrefs(root)
        }
    }
}

class TranscriptionsState : IViewModelState
