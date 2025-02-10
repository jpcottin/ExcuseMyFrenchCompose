package com.example.excusemyfrenchcompose.ui.viewmodel

import android.app.Application
import android.speech.tts.TextToSpeech
import android.util.Log
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.excusemyfrenchcompose.data.model.InsultResponse
import com.example.excusemyfrenchcompose.data.remote.InsultApiService
import com.example.excusemyfrenchcompose.util.ImageUtils
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import java.util.Locale

// UI State
data class InsultUiState(
    val insultText: String = "",
    val imageBitmap: ImageBitmap? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val isMuted: Boolean = true // Add isMuted state
)

class InsultViewModel(application: Application, private val apiService: InsultApiService = InsultApiService()) : AndroidViewModel(application), InsultViewModelInterface {

    private val _uiState = MutableStateFlow(InsultUiState(isLoading = true))
    override val uiState: StateFlow<InsultUiState> = _uiState.asStateFlow()

    private val json = Json {
        ignoreUnknownKeys = true
    }

    // Lazily initialized TextToSpeech
    private var tts: TextToSpeech? = null

    init {
        fetchInsultRepeatedly()
    }

    private fun fetchInsultRepeatedly() {
        viewModelScope.launch {
            while (true) {
                fetchInsult()
                delay(5000)
            }
        }
    }

    override fun toggleMute() {
        _uiState.update { currentState ->
            currentState.copy(isMuted = !currentState.isMuted)
        }
        //If we unmute and had an error during the initialization, re-initialize
        if(!_uiState.value.isMuted && tts == null){
            initializeTTS()
        }
    }

    private fun initializeTTS() {
        tts = TextToSpeech(
            getApplication() // Corrected: Call getApplication() directly
        ) { status ->
            if (status == TextToSpeech.SUCCESS) {
                val result = tts?.setLanguage(Locale.FRANCE)
                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    Log.e("TTS", "French language is not supported.")
                    // Handle the error. Maybe disable the unmute button, show a message, etc.
                    _uiState.update { it.copy(error = "French language is not supported.") } // Update to reflect error
                }
            } else {
                Log.e("TTS", "Initialization failed.")
                _uiState.update { it.copy(error = "TTS Initialization failed.") } // Update UI state
            }
        }
    }
    override fun speak(text: String) {
        if (_uiState.value.isMuted) return // Do nothing if muted

        if (tts == null) {
            initializeTTS()
        }

        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "")
    }

    suspend fun fetchInsult() {
        try {
            val responseBodyString = apiService.fetchInsult()
            if (!responseBodyString.isNullOrBlank()) {
                val insultResponse = json.decodeFromString<InsultResponse>(responseBodyString)
                val decodedBitmap = ImageUtils.decodeImage(insultResponse.image.data)
                val insultText = insultResponse.insult.text.ifBlank { "No insult text provided" }

                _uiState.value = InsultUiState(
                    insultText = insultText,
                    imageBitmap = decodedBitmap?.asImageBitmap(),
                    isLoading = false,
                    error = null,
                    isMuted = _uiState.value.isMuted // Preserve the mute state
                )

                speak(insultText) // Speak the insult!

            } else {
                _uiState.value = InsultUiState(error = "Error: Empty response body", isLoading = false, isMuted = _uiState.value.isMuted)
            }

        } catch (e: Exception) {
            Log.e("InsultViewModel", "Error fetching insult: ${e.message}", e)
            _uiState.value = InsultUiState(error = "Error: ${e.message}", isLoading = false, isMuted = _uiState.value.isMuted)
        }
    }
    override fun onCleared() {
        super.onCleared()
        tts?.stop()
        tts?.shutdown()
    }
}