package com.example.excusemyfrenchcompose.ui.viewmodel

import android.util.Log
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.excusemyfrenchcompose.data.model.InsultResponse
import com.example.excusemyfrenchcompose.data.remote.InsultApiService
import com.example.excusemyfrenchcompose.util.ImageUtils
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json

// UI State
data class InsultUiState(
    val insultText: String = "",
    val imageBitmap: ImageBitmap? = null,
    val isLoading: Boolean = false,
    val error: String? = null
)

// Interface for the ViewModel
interface InsultViewModelInterface {
    val uiState: StateFlow<InsultUiState>
}

class InsultViewModel(private val apiService: InsultApiService = InsultApiService()) : ViewModel(), InsultViewModelInterface {

    private val _uiState = MutableStateFlow(InsultUiState(isLoading = true)) //Initial state is loading
    override val uiState: StateFlow<InsultUiState> = _uiState.asStateFlow()

    private val json = Json {
        ignoreUnknownKeys = true
    }

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

    suspend fun fetchInsult() {
        try {
            val responseBodyString = apiService.fetchInsult()
            if (!responseBodyString.isNullOrBlank()) {
                val insultResponse = json.decodeFromString<InsultResponse>(responseBodyString) // Use the simplified parsing
                val decodedBitmap = ImageUtils.decodeImage(insultResponse.image.data)

                _uiState.value = InsultUiState(
                    insultText = insultResponse.insult.text.ifBlank { "No insult text provided" },
                    imageBitmap = decodedBitmap?.asImageBitmap(),
                    isLoading = false,
                    error = null
                )

            } else {
                _uiState.value = InsultUiState(error = "Error: Empty response body", isLoading = false)
            }

        } catch (e: Exception) {
            Log.e("InsultViewModel", "Error fetching insult: ${e.message}", e)
            _uiState.value = InsultUiState(error = "Error: ${e.message}", isLoading = false)

        }
    }
}