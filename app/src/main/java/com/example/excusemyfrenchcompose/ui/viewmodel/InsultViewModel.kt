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
import com.example.excusemyfrenchcompose.util.ImageUtils
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.IOException
import java.util.Locale

data class InsultUiState(
    val insultText: String = "",
    val imageBitmap: ImageBitmap? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val isMuted: Boolean = true
)

class InsultViewModel(
    application: Application,
    private val repository: InsultRepository
) : AndroidViewModel(application), InsultViewModelInterface {

    companion object {
        private const val REFRESH_INTERVAL = 5000L
        private const val TTS_UTTERANCE_ID = "insult_speech"
        private const val TAG = "InsultViewModel"
    }

    private val _uiState = MutableStateFlow(InsultUiState(isLoading = true))
    override val uiState: StateFlow<InsultUiState> = _uiState.asStateFlow()

    private var tts: TextToSpeech? = null

    init {
        fetchInsultRepeatedly()
    }

    private fun fetchInsultRepeatedly() {
        viewModelScope.launch {
            while (isActive) {
                fetchInsult()
                delay(REFRESH_INTERVAL)
            }
        }
    }

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
        _uiState.update { it.copy(isMuted = !it.isMuted) }
        if (!_uiState.value.isMuted && tts == null) {
            initializeTTS()
        }
    }

    override fun speak(text: String) {
        if (_uiState.value.isMuted) return
        if (tts == null) initializeTTS()
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, TTS_UTTERANCE_ID)
    }

    override fun retryFetch() {
        _uiState.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch { fetchInsult() }
    }

    private fun initializeTTS() {
        if (tts != null) return
        tts = TextToSpeech(getApplication()) { status ->
            if (status == TextToSpeech.SUCCESS) {
                configureTTSLanguage()
            } else {
                handleTTSError(getString(R.string.tts_init_failed))
            }
        }
    }

    private fun configureTTSLanguage() {
        val result = tts?.setLanguage(Locale.FRANCE)
        if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
            handleTTSError(getString(R.string.tts_language_not_supported))
        }
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
        tts?.stop()
        tts?.shutdown()
    }
}
