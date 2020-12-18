package ru.skillbranch.skillarticles.viewmodels.transcriptions

import androidx.lifecycle.SavedStateHandle
import ru.skillbranch.skillarticles.data.local.PrefManager
import ru.skillbranch.skillarticles.ui.RootActivity
import ru.skillbranch.skillarticles.viewmodels.base.BaseViewModel
import ru.skillbranch.skillarticles.viewmodels.base.IViewModelState

class TranscriptionsViewModel(handle: SavedStateHandle) :
    BaseViewModel<TranscriptionsState>(handle, TranscriptionsState()) {

    fun resetAllPreferences(root: RootActivity) {
        val isPrefsDefault = with(PrefManager) {
            !isDarkMode && !isBigText && accessToken.isEmpty()
                    && refreshToken.isEmpty()
        }
        if (isPrefsDefault) return

        launchSafety {
            // Делаем ресет настроек приложения на девайсе
            PrefManager.resetAllPrefs(root)
        }
    }
}

class TranscriptionsState : IViewModelState
