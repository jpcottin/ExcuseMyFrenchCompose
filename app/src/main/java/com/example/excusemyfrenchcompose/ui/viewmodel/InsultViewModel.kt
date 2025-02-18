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
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import java.util.Locale
import java.io.IOException

interface ResourceProvider {
    fun getString(resId: Int): String
    fun getString(resId: Int, vararg formatArgs: Any): String
}

class AndroidResourceProvider(private val application: Application) : ResourceProvider {
    override fun getString(resId: Int): String = application.getString(resId)
    override fun getString(resId: Int, vararg formatArgs: Any): String = application.getString(resId, *formatArgs)
}

data class InsultUiState(
    val insultText: String = "",
    val imageBitmap: ImageBitmap? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val isMuted: Boolean = true
)

class InsultViewModel(
    application: Application,
    private val apiService: InsultApiService = InsultApiService(),
    private val resourceProvider: ResourceProvider = AndroidResourceProvider(application)
) : AndroidViewModel(application), InsultViewModelInterface {

    private val _uiState = MutableStateFlow(InsultUiState(isLoading = true))
    override val uiState: StateFlow<InsultUiState> = _uiState.asStateFlow()

    private val json = Json { ignoreUnknownKeys = true }
    private var tts: TextToSpeech? = null
    private var fetchJob: Job? = null // Store the Job

    init {
        fetchInsultRepeatedly()
    }

    private fun fetchInsultRepeatedly() {
        fetchJob = viewModelScope.launch { // Assign the Job
            while (true) {
                fetchInsult()
                delay(5000)
            }
        }
    }

    override fun toggleMute() {
        _uiState.update { it.copy(isMuted = !it.isMuted) }
        if (!_uiState.value.isMuted && tts == null) {
            initializeTTS()
        }
    }

    private fun initializeTTS() {
        tts = TextToSpeech(getApplication()) { status ->
            if (status == TextToSpeech.SUCCESS) {
                val result = tts?.setLanguage(Locale.FRANCE)
                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    Log.e("TTS", "French is not supported")
                    _uiState.update { it.copy(error = resourceProvider.getString(R.string.tts_language_not_supported)) }
                }
            } else {
                Log.e("TTS", "TTS init failed")
                _uiState.update { it.copy(error = resourceProvider.getString(R.string.tts_init_failed)) }
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
            val response = apiService.fetchInsult()
            if (!response.isNullOrBlank()) {
                val insultResponse = json.decodeFromString<InsultResponse>(response)
                val bitmap: ImageBitmap?
                try {
                    bitmap = ImageUtils.decodeImage(insultResponse.image.data)?.asImageBitmap()
                } catch (_: IllegalArgumentException) {
                    _uiState.update { it.copy(error = resourceProvider.getString(R.string.image_decoding_error), isLoading = false) }
                    return
                }
                val insult = insultResponse.insult.text.ifBlank { resourceProvider.getString(R.string.no_insult_available) }
                _uiState.update {
                    it.copy(
                        insultText = insult,
                        imageBitmap = bitmap,
                        isLoading = false,
                        error = null
                    )
                }
                speak(insult) // Call speak here

            } else {
                _uiState.update { it.copy(error = resourceProvider.getString(R.string.could_not_load), isLoading = false) }
            }
        } catch (e: IOException) {
            _uiState.update { it.copy(error = resourceProvider.getString(R.string.no_internet, e.message.toString()), isLoading = false) }

        } catch (e: Exception) {
            _uiState.update { it.copy(error = resourceProvider.getString(R.string.could_not_load), isLoading = false) }
        }
    }

    override fun onCleared() {
        super.onCleared()
        tts?.stop()
        tts?.shutdown()
        fetchJob?.cancel() // Cancel the job in onCleared
    }
}