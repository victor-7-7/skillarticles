package ru.skillbranch.skillarticles.viewmodels.transcriptions

import androidx.lifecycle.SavedStateHandle
import ru.skillbranch.skillarticles.data.local.PrefManager
import ru.skillbranch.skillarticles.ui.RootActivity
import ru.skillbranch.skillarticles.viewmodels.base.BaseViewModel
import ru.skillbranch.skillarticles.viewmodels.base.IViewModelState

class TranscriptionsViewModel(handle: SavedStateHandle) :
    BaseViewModel<TranscriptionsState>(handle, TranscriptionsState()) {

    fun resetAllPreferences(root: RootActivity) {
        PrefManager.resetAllPrefs(root)
    }
}

class TranscriptionsState : IViewModelState
