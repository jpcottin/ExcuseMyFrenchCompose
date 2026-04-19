package com.example.excusemyfrenchcompose.ui.viewmodel

import kotlinx.coroutines.flow.StateFlow

interface InsultViewModelInterface {
    val uiState: StateFlow<InsultUiState>
    fun toggleMute()
    fun speak(text: String)
    fun retryFetch()
}
