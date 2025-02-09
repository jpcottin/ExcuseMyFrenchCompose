package com.example.excusemyfrenchcompose

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

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ExcluseMyFrenchComposeTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    // Get the ViewModel using viewModel()
                    val viewModel: InsultViewModel = viewModel()
                    InsultDisplay(
                        viewModel = viewModel, //Pass viewModel
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}