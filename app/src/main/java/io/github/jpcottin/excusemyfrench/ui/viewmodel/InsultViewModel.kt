package io.github.jpcottin.excusemyfrench.ui.viewmodel

import android.app.Application
import android.os.SystemClock
import android.util.Log
import androidx.annotation.StringRes
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import io.github.jpcottin.excusemyfrench.R
import io.github.jpcottin.excusemyfrench.data.repository.InsultRepository
import io.github.jpcottin.excusemyfrench.data.settings.DataStoreSettingsRepository
import io.github.jpcottin.excusemyfrench.data.settings.SettingsRepository
import io.github.jpcottin.excusemyfrench.service.TtsService
import io.github.jpcottin.excusemyfrench.util.ImageUtils
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.IOException

data class InsultUiState(
    val insultText: String = "",
    val imageBitmap: ImageBitmap? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val isMuted: Boolean = true,
    val isPaused: Boolean = false,
    val insultLevel: Int = DataStoreSettingsRepository.DEFAULT_LEVEL,
    // TTS problems must not hide the fetched content, so they are reported separately from [error].
    val ttsError: String? = null
)

class InsultViewModel(
    application: Application,
    private val repository: InsultRepository,
    private val ttsService: TtsService,
    private val settings: SettingsRepository,
    // Base64 + JPEG decoding of a server-sized photo takes tens of milliseconds: off the
    // main thread, where the view model's coroutines otherwise run.
    private val decodeDispatcher: CoroutineDispatcher = Dispatchers.Default,
    private val elapsedRealtimeMs: () -> Long = SystemClock::elapsedRealtime,
) : AndroidViewModel(application), InsultViewModelInterface {

    companion object {
        private const val REFRESH_INTERVAL = 10000L
        private const val TAG = "InsultViewModel"
    }

    private val _uiState = MutableStateFlow(InsultUiState(isLoading = true))
    override val uiState: StateFlow<InsultUiState> = _uiState.asStateFlow()

    // Once the user toggles mute, their explicit choice wins over the persisted value loaded async.
    private var muteOverridden = false

    // Same for the insult level picker.
    private var levelOverridden = false

    init {
        // The repository maps a read error to its defaults; this guard keeps any other
        // implementation (or a future error type) from taking the process down at start.
        viewModelScope.launch {
            val savedMuted = runCatching { settings.isMuted.first() }
                .onFailure { Log.e(TAG, "Could not read the mute setting", it) }.getOrNull() ?: return@launch
            if (!muteOverridden) {
                _uiState.update { it.copy(isMuted = savedMuted) }
            }
        }
        viewModelScope.launch {
            val savedLevel = runCatching { settings.insultLevel.first() }
                .onFailure { Log.e(TAG, "Could not read the level setting", it) }.getOrNull() ?: return@launch
            if (!levelOverridden) {
                _uiState.update { it.copy(insultLevel = savedLevel) }
            }
        }
    }

    // When the last fetch (of any kind) happened, so a loop restarted by the screen being
    // recreated (rotation, unfold, a return from the background) picks up the interval where
    // it was instead of fetching, and speaking, a fresh insult at once.
    private var lastFetchAtMs: Long? = null

    // True from the moment the screen's loop is cancelled until a screen runs it again: an
    // insult that arrives in between (a Next tap answered after Home, or a call) is shown, not
    // spoken over whatever the user is now doing.
    private var screenGone = false

    override suspend fun autoRefresh() {
        screenGone = false
        try {
            while (currentCoroutineContext().isActive) {
                val sinceLast = lastFetchAtMs?.let { elapsedRealtimeMs() - it }
                if (sinceLast != null && sinceLast < REFRESH_INTERVAL) {
                    delay(REFRESH_INTERVAL - sinceLast)
                    continue
                }
                if (!_uiState.value.isPaused) {
                    fetchInsult()
                }
                delay(REFRESH_INTERVAL)
            }
        } finally {
            screenGone = true
        }
    }

    // Serializes fetches so a user action (next, retry, level change) can't interleave with an
    // in-flight auto-refresh and let a slower, older response overwrite a newer one.
    private val fetchMutex = Mutex()

    @androidx.annotation.VisibleForTesting
    suspend fun fetchInsult() = fetchMutex.withLock {
        val startedAt = elapsedRealtimeMs()
        try {
            val response = repository.fetchInsult(_uiState.value.insultLevel)
            if (response != null) {
                val bitmap = withContext(decodeDispatcher) { ImageUtils.decodeImage(response.image.data) }?.asImageBitmap()
                val text = response.insult.text.ifBlank { "No insult text provided" }
                updateUIWithSuccess(text, bitmap)
                speak(text)
            } else {
                handleError(getString(R.string.could_not_load))
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: IOException) {
            Log.e(TAG, "Network error: ${e.message}", e)
            handleError(getString(R.string.no_internet))
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching insult: ${e.message}", e)
            handleError(getString(R.string.could_not_load))
        } finally {
            // A fetch cancelled with its screen produced nothing: the restarted loop must retry
            // at once rather than wait out the interval on a spinner.
            if (currentCoroutineContext().isActive) lastFetchAtMs = startedAt
        }
    }

    /** A setting that cannot be written is logged; the user's choice still applies for this session. */
    private suspend fun save(write: suspend () -> Unit) {
        try {
            write()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Could not save a setting", e)
        }
    }

    override fun toggleMute() {
        muteOverridden = true
        val newMuted = !_uiState.value.isMuted
        _uiState.update { it.copy(isMuted = newMuted, ttsError = null) }
        viewModelScope.launch { save { settings.setMuted(newMuted) } }
        if (newMuted) {
            // Muting silences the insult being spoken and anything queued for the engine.
            ttsService.stop()
        } else {
            ttsService.initialize { errorMessage ->
                handleTTSError(errorMessage)
            }
        }
    }

    override fun togglePause() {
        _uiState.update { it.copy(isPaused = !it.isPaused) }
    }

    override fun setInsultLevel(level: Int) {
        levelOverridden = true
        if (level == _uiState.value.insultLevel) return
        _uiState.update { it.copy(insultLevel = level, isLoading = true, error = null) }
        viewModelScope.launch {
            save { settings.setInsultLevel(level) }
            fetchInsult()   // the level applies for this session even when it could not be saved
        }
    }

    override fun fetchNext() {
        _uiState.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch { fetchInsult() }
    }

    override fun speak(text: String) {
        if (_uiState.value.isMuted || screenGone) return
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
        _uiState.update { it.copy(ttsError = message) }
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
