package com.example.excusemyfrenchcompose.ui.viewmodel

import android.app.Application
import android.speech.tts.TextToSpeech
import android.util.Log
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.excusemyfrenchcompose.R
import com.example.excusemyfrenchcompose.data.model.InsultResponse
import com.example.excusemyfrenchcompose.data.remote.InsultApiService
import com.example.excusemyfrenchcompose.util.ImageUtils
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import java.util.Locale
import java.io.IOException

data class InsultUiState(
    val insultText: String = "",
    val imageBitmap: ImageBitmap? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val isMuted: Boolean = true
)

class InsultViewModel(
    application: Application,
    private val apiService: InsultApiService = InsultApiService()
) : AndroidViewModel(application), InsultViewModelInterface {

    companion object {
        private const val REFRESH_INTERVAL = 5000L
        private const val TTS_UTTERANCE_ID = "insult_speech"
        private const val TAG = "InsultViewModel"
    }

    private val _uiState = MutableStateFlow(InsultUiState(isLoading = true))
    override val uiState: StateFlow<InsultUiState> = _uiState.asStateFlow()

    private val json = Json {
        ignoreUnknownKeys = true
    }

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

    override fun toggleMute() {
        _uiState.update { currentState ->
            currentState.copy(isMuted = !currentState.isMuted)
        }

        if (!_uiState.value.isMuted && tts == null) {
            initializeTTS()
        }
    }

    private fun initializeTTS() {
        tts = TextToSpeech(getApplication()) { status ->
            if (status == TextToSpeech.SUCCESS) {
                configureTTSLanguage()
            } else {
                handleTTSError(getApplication<Application>().getString(R.string.tts_init_failed))
            }
        }
    }

    private fun configureTTSLanguage() {
        val result = tts?.setLanguage(Locale.FRANCE)
        if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
            handleTTSError(getApplication<Application>().getString(R.string.tts_language_not_supported))
        }
    }

    private fun handleTTSError(errorMessage: String) {
        Log.e(TAG, "TTS Error: $errorMessage")
        _uiState.update { currentState ->
            currentState.copy(error = errorMessage)
        }
    }

    override fun speak(text: String) {
        if (_uiState.value.isMuted) return

        if (tts == null) {
            initializeTTS()
        }

        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, TTS_UTTERANCE_ID)
    }

    suspend fun fetchInsult() {
        try {
            val responseBodyString = apiService.fetchInsult()
            if (!responseBodyString.isNullOrBlank()) {
                processInsultResponse(responseBodyString)
            } else {
                handleError(getApplication<Application>().getString(R.string.could_not_load))
            }
        } catch (e: IOException) {
            Log.e(TAG, "Network error: ${e.message}", e)
            handleError(getApplication<Application>().getString(R.string.no_internet))
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching insult: ${e.message}", e)
            handleError(getApplication<Application>().getString(R.string.could_not_load))
        }
    }

    private fun processInsultResponse(responseBodyString: String) {
        try {
            val insultResponse = json.decodeFromString<InsultResponse>(responseBodyString)
            val decodedBitmap = decodeImageSafely(insultResponse.image.data)
            val insultText = insultResponse.insult.text.ifBlank { "No insult text provided" }

            updateUIWithSuccess(insultText, decodedBitmap)
            speak(insultText)
        } catch (e: Exception) {
            Log.e(TAG, "Error processing response: ${e.message}", e)
            handleError(getApplication<Application>().getString(R.string.could_not_load))
        }
    }

    private fun decodeImageSafely(imageData: String): ImageBitmap? {
        return try {
            ImageUtils.decodeImage(imageData)?.asImageBitmap()
        } catch (e: IllegalArgumentException) {
            Log.e(TAG, "Image decoding error: ${e.message}", e)
            handleError(getApplication<Application>().getString(R.string.image_decoding_error))
            null
        }
    }

    private fun updateUIWithSuccess(insultText: String, imageBitmap: ImageBitmap?) {
        _uiState.update { currentState ->
            currentState.copy(
                insultText = insultText,
                imageBitmap = imageBitmap,
                isLoading = false,
                error = null
            )
        }
    }

    private fun handleError(errorMessage: String) {
        _uiState.update { currentState ->
            currentState.copy(
                error = errorMessage,
                isLoading = false
            )
        }
    }

    fun retryFetch() {
        _uiState.update { currentState ->
            currentState.copy(isLoading = true, error = null)
        }
        viewModelScope.launch {
            fetchInsult()
        }
    }

    override fun onCleared() {
        super.onCleared()
        tts?.stop()
        tts?.shutdown()
    }
}