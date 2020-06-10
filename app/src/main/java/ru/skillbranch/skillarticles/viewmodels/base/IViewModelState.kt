package ru.skillbranch.skillarticles.viewmodels.base

import androidx.lifecycle.SavedStateHandle

interface IViewModelState {
    /**
     * Override this if need save state in bundle
     * */
    fun save(outState: SavedStateHandle) {
        // default empty implementation
    }

    /**
     * Override this if need restore state from bundle
     * */
    fun restore(savedState: SavedStateHandle): IViewModelState {
        // default empty implementation
        return this
    }
}