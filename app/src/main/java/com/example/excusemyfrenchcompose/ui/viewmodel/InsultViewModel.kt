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

class InsultViewModel(application: Application, private val apiService: InsultApiService = InsultApiService()) : AndroidViewModel(application), InsultViewModelInterface {

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
        if (!_uiState.value.isMuted && tts == null) {
            initializeTTS()
        }
    }

    private fun initializeTTS() {
        tts = TextToSpeech(getApplication()) { status ->
            if (status == TextToSpeech.SUCCESS) {
                val result = tts?.setLanguage(Locale.FRANCE)
                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    Log.e("TTS", "French language is not supported.")
                    _uiState.update { it.copy(error = getApplication<Application>().getString(R.string.tts_language_not_supported)) }
                }
            } else {
                Log.e("TTS", "TTS Initialization failed.")
                _uiState.update { it.copy(error = getApplication<Application>().getString(R.string.tts_init_failed)) }
            }
        }
    }

    override fun speak(text: String) {
        if (_uiState.value.isMuted) return

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
                val decodedBitmap: ImageBitmap?
                try {
                    decodedBitmap = ImageUtils.decodeImage(insultResponse.image.data)?.asImageBitmap()
                } catch (_: IllegalArgumentException) {
                    _uiState.value = InsultUiState(error = getApplication<Application>().getString(R.string.image_decoding_error), isLoading = false)
                    return // Important to return in this case
                }
                val insultText = insultResponse.insult.text.ifBlank { "No insult text provided" }

                _uiState.value = InsultUiState(
                    insultText = insultText,
                    imageBitmap = decodedBitmap,
                    isLoading = false,
                    error = null,
                    isMuted = _uiState.value.isMuted
                )
                speak(insultText)

            } else {
                _uiState.value = InsultUiState(error = getApplication<Application>().getString(R.string.could_not_load), isLoading = false, isMuted = _uiState.value.isMuted)
            }
        } catch (e: IOException) {
            Log.e("InsultViewModel", "Network error: ${e.message}", e)
            _uiState.value = InsultUiState(error = getApplication<Application>().getString(R.string.no_internet), isLoading = false, isMuted = _uiState.value.isMuted)
        } catch (e: Exception) {
            Log.e("InsultViewModel", "Error fetching insult: ${e.message}", e)
            _uiState.value = InsultUiState(error = getApplication<Application>().getString(R.string.could_not_load), isLoading = false, isMuted = _uiState.value.isMuted)
        }
    }

    override fun onCleared() {
        super.onCleared()
        tts?.stop()
        tts?.shutdown()
    }
}