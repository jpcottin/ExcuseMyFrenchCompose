package com.example.excusemyfrenchcompose

import android.app.Application
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.excusemyfrenchcompose.ui.components.InsultDisplay
import com.example.excusemyfrenchcompose.ui.theme.ExcluseMyFrenchComposeTheme
import com.example.excusemyfrenchcompose.ui.viewmodel.InsultViewModel
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Surface
import androidx.compose.material3.MaterialTheme
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.excusemyfrenchcompose.data.remote.InsultApiService


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        title = ""

        setContent {
            ExcluseMyFrenchComposeTheme {
                Surface(
                    modifier = Modifier
                        .fillMaxSize()
                        .windowInsetsPadding(WindowInsets.systemBars),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Scaffold(
                        modifier = Modifier.fillMaxSize(),
                        topBar = { /* Intentionally left empty */ }
                    ) { innerPadding ->
                        // Use a custom ViewModelProvider.Factory
                        val viewModel: InsultViewModel = viewModel(
                            factory = InsultViewModelFactory(application, InsultApiService())
                        )
                        InsultDisplay(
                            viewModel = viewModel,
                            modifier = Modifier.padding(innerPadding)
                        )
                    }
                }
            }
        }
    }
}

// Custom ViewModel Factory
class InsultViewModelFactory(
    private val application: Application,
    private val apiService: InsultApiService
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(InsultViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return InsultViewModel(application, apiService) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}