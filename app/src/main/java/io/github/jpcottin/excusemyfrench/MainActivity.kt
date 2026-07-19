package io.github.jpcottin.excusemyfrench

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.jpcottin.excusemyfrench.ui.components.InsultDisplay
import io.github.jpcottin.excusemyfrench.ui.theme.ExcuseMyFrenchComposeTheme
import io.github.jpcottin.excusemyfrench.ui.viewmodel.InsultViewModel
import io.github.jpcottin.excusemyfrench.ui.viewmodel.InsultViewModelFactory

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            ExcuseMyFrenchComposeTheme {
                // Scaffold draws edge-to-edge and reports the system bar insets via innerPadding,
                // so the content stays clear of the status and navigation bars without extra handling.
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    val viewModel: InsultViewModel = viewModel(
                        factory = InsultViewModelFactory(application)
                    )
                    InsultDisplay(
                        viewModel = viewModel,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    )
                }
            }
        }
    }
}
