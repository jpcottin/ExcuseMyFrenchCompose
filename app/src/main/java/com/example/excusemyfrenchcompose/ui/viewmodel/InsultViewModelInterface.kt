package com.example.excusemyfrenchcompose.ui.viewmodel

import kotlinx.coroutines.flow.StateFlow

// Interface for the ViewModel
interface InsultViewModelInterface {
    val uiState: StateFlow<InsultUiState>
    fun toggleMute()
    fun speak(text: String)
}
