package com.example.excusemyfrenchcompose.ui.viewmodel

import kotlinx.coroutines.flow.StateFlow

interface InsultViewModelInterface {
    val uiState: StateFlow<InsultUiState>
    fun toggleMute()
    fun speak(text: String)
    fun retryFetch()
    fun togglePause()
    fun fetchNext()

    /**
     * Runs the auto-refresh loop until the calling coroutine is cancelled. Intended to be
     * driven from the UI inside `repeatOnLifecycle` so polling pauses while the app is not visible.
     */
    suspend fun autoRefresh()
}
