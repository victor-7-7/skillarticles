package ru.skillbranch.skillarticles.viewmodels.transcriptions


import androidx.lifecycle.SavedStateHandle
import dagger.hilt.android.lifecycle.HiltViewModel
import ru.skillbranch.skillarticles.data.local.PrefManager
import ru.skillbranch.skillarticles.ui.RootActivity
import ru.skillbranch.skillarticles.viewmodels.base.BaseViewModel
import ru.skillbranch.skillarticles.viewmodels.base.IViewModelState
import javax.inject.Inject

@HiltViewModel
class TranscriptionsViewModel @Inject constructor(
    handle: SavedStateHandle,
    private val prefs: PrefManager
) : BaseViewModel<TranscriptionsState>(handle, TranscriptionsState()) {

    fun resetAllPreferences(root: RootActivity) {
        // todo: implement

        val isPrefsDefault = with(prefs) {
            !isDarkMode && !isBigText && accessToken.isEmpty()
                    && refreshToken.isEmpty()
        }
        if (isPrefsDefault) return

        launchSafety {
            // Делаем ресет настроек приложения на девайсе
            prefs.resetAllPrefs(root)
        }
    }
}

class TranscriptionsState : IViewModelState
