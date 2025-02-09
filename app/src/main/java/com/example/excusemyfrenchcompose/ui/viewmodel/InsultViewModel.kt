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

// Interface for the ViewModel - Added
interface InsultViewModelInterface {
    val uiState: StateFlow<InsultUiState>
}

class InsultViewModel(private val apiService: InsultApiService = InsultApiService()) : ViewModel(), InsultViewModelInterface { // Implement interface

    private val _uiState = MutableStateFlow(InsultUiState(isLoading = true)) //Initial state is loading
    override val uiState: StateFlow<InsultUiState> = _uiState.asStateFlow()

    private val lenientJson = Json {
        ignoreUnknownKeys = true
        isLenient = true
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

    suspend fun fetchInsult() { // Made public for testing purposes
        // _uiState.value = _uiState.value.copy(isLoading = true) // We do it in init{} NO MORE HERE
        try {
            val responseBodyString = apiService.fetchInsult()
            if (!responseBodyString.isNullOrBlank()) {
                // JSON FIXING (Regex)
                var fixedBody = responseBodyString.replace(Regex("\"[^\"]+\"\\s+:"), { matchResult ->
                    matchResult.value.replace("\\s+".toRegex(), "")
                })

                fixedBody = fixedBody.replace(Regex("\"\\s+,"), { matchResult ->
                    "\"" + ","
                })

                Log.d("API Response - MODIFIED", "Modified Body: $fixedBody")
                val insultResponse = lenientJson.decodeFromString<InsultResponse>(fixedBody)
                val decodedBitmap = ImageUtils.decodeImage(insultResponse.image.data)

                _uiState.value = InsultUiState(
                    insultText = insultResponse.insult.text.ifBlank { "No insult text provided" },
                    imageBitmap = decodedBitmap?.asImageBitmap(),
                    isLoading = false, // Set loading to false on success
                    error = null
                )

            } else {
                _uiState.value = InsultUiState(error = "Error: Empty response body", isLoading = false) //Good
            }

        } catch (e: Exception) {
            Log.e("InsultViewModel", "Error fetching insult: ${e.message}", e)
            _uiState.value = InsultUiState(error = "Error: ${e.message}", isLoading = false) //Good

        }
    }
}