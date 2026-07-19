package io.github.jpcottin.excusemyfrench.ui.viewmodel

import kotlinx.coroutines.flow.StateFlow

interface InsultViewModelInterface {
    val uiState: StateFlow<InsultUiState>
    fun toggleMute()
    fun speak(text: String)
    fun retryFetch()
    fun togglePause()
    fun fetchNext()

    /** Sets the maximum insult level (1 = family-friendly, 2 = vulgar, 3 = offensive) and refetches. */
    fun setInsultLevel(level: Int)

    /**
     * Runs the auto-refresh loop until the calling coroutine is cancelled. Intended to be
     * driven from the UI inside `repeatOnLifecycle` so polling pauses while the app is not visible.
     */
    suspend fun autoRefresh()
}
