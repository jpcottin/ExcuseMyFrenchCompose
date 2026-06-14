package com.example.excusemyfrenchcompose.ui.viewmodel

import android.app.Application
import android.speech.tts.TextToSpeech
import android.util.Log
import androidx.annotation.StringRes
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.excusemyfrenchcompose.R
import com.example.excusemyfrenchcompose.data.repository.InsultRepository
import com.example.excusemyfrenchcompose.data.settings.SettingsRepository
import com.example.excusemyfrenchcompose.service.TtsService
import com.example.excusemyfrenchcompose.util.ImageUtils
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.IOException

data class InsultUiState(
    val insultText: String = "",
    val imageBitmap: ImageBitmap? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val isMuted: Boolean = true,
    val isPaused: Boolean = false
)

class InsultViewModel(
    application: Application,
    private val repository: InsultRepository,
    private val ttsService: TtsService,
    private val settings: SettingsRepository
) : AndroidViewModel(application), InsultViewModelInterface {

    companion object {
        private const val REFRESH_INTERVAL = 10000L
        private const val TAG = "InsultViewModel"
    }

    private val _uiState = MutableStateFlow(InsultUiState(isLoading = true))
    override val uiState: StateFlow<InsultUiState> = _uiState.asStateFlow()

    // Once the user toggles mute, their explicit choice wins over the persisted value loaded async.
    private var muteOverridden = false

    init {
        viewModelScope.launch {
            val savedMuted = settings.isMuted.first()
            if (!muteOverridden) {
                _uiState.update { it.copy(isMuted = savedMuted) }
            }
        }
    }

    override suspend fun autoRefresh() {
        while (currentCoroutineContext().isActive) {
            if (!_uiState.value.isPaused) {
                fetchInsult()
            }
            delay(REFRESH_INTERVAL)
        }
    }

    @androidx.annotation.VisibleForTesting
    suspend fun fetchInsult() {
        try {
            val response = repository.fetchInsult()
            if (response != null) {
                val bitmap = decodeImageSafely(response.image.data)
                val text = response.insult.text.ifBlank { "No insult text provided" }
                updateUIWithSuccess(text, bitmap)
                speak(text)
            } else {
                handleError(getString(R.string.could_not_load))
            }
        } catch (e: IOException) {
            Log.e(TAG, "Network error: ${e.message}", e)
            handleError(getString(R.string.no_internet))
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching insult: ${e.message}", e)
            handleError(getString(R.string.could_not_load))
        }
    }

    override fun toggleMute() {
        muteOverridden = true
        val newMuted = !_uiState.value.isMuted
        _uiState.update { it.copy(isMuted = newMuted) }
        viewModelScope.launch { settings.setMuted(newMuted) }
        if (!newMuted) {
            ttsService.initialize { errorMessage ->
                handleTTSError(errorMessage)
            }
        }
    }

    override fun togglePause() {
        _uiState.update { it.copy(isPaused = !it.isPaused) }
    }

    override fun fetchNext() {
        _uiState.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch { fetchInsult() }
    }

    override fun speak(text: String) {
        if (_uiState.value.isMuted) return
        ttsService.initialize { errorMessage ->
            handleTTSError(errorMessage)
        }
        ttsService.speak(text)
    }

    override fun retryFetch() {
        _uiState.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch { fetchInsult() }
    }

    private fun handleTTSError(message: String) {
        Log.e(TAG, "TTS error: $message")
        _uiState.update { it.copy(error = message) }
    }

    private fun decodeImageSafely(imageData: String): ImageBitmap? {
        return try {
            ImageUtils.decodeImage(imageData)?.asImageBitmap()
        } catch (e: IllegalArgumentException) {
            Log.e(TAG, "Image decoding error: ${e.message}", e)
            handleError(getString(R.string.image_decoding_error))
            null
        }
    }

    private fun updateUIWithSuccess(insultText: String, imageBitmap: ImageBitmap?) {
        _uiState.update { it.copy(insultText = insultText, imageBitmap = imageBitmap, isLoading = false, error = null) }
    }

    private fun handleError(message: String) {
        _uiState.update { it.copy(error = message, isLoading = false) }
    }

    private fun getString(@StringRes resId: Int): String = getApplication<Application>().getString(resId)

    override fun onCleared() {
        super.onCleared()
        ttsService.shutdown()
    }
}
