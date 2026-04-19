package com.example.excusemyfrenchcompose.ui.components

import android.content.res.Configuration.UI_MODE_NIGHT_YES
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.ImageBitmapConfig
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import com.example.excusemyfrenchcompose.ui.theme.ExcuseMyFrenchComposeTheme
import com.example.excusemyfrenchcompose.ui.viewmodel.InsultUiState
import com.example.excusemyfrenchcompose.ui.viewmodel.InsultViewModelInterface
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

internal class FakeViewModel(private val state: InsultUiState) : InsultViewModelInterface {
    override val uiState: StateFlow<InsultUiState> = MutableStateFlow(state).asStateFlow()
    override fun toggleMute() {}
    override fun speak(text: String) {}
    override fun retryFetch() {}
}

internal class InsultUiStatePreviewProvider : PreviewParameterProvider<InsultUiState> {
    override val values: Sequence<InsultUiState> = sequenceOf(
        InsultUiState(isLoading = true),
        InsultUiState(insultText = "Espèce d'idiot !", imageBitmap = createPreviewBitmap(), isLoading = false),
        InsultUiState(error = "Error: Network connection failed", isLoading = false),
        InsultUiState(insultText = "", imageBitmap = null, error = null, isLoading = false)
    )
}

private fun createPreviewBitmap(color: Color = Color.Blue): ImageBitmap {
    val bitmap = ImageBitmap(200, 100, ImageBitmapConfig.Argb8888)
    Canvas(bitmap).drawRect(0f, 0f, 200f, 100f, Paint().apply { this.color = color })
    return bitmap
}

@Preview(showBackground = true, name = "Phone Portrait")
@Composable
fun InsultDisplayPreview(
    @PreviewParameter(InsultUiStatePreviewProvider::class) uiState: InsultUiState
) {
    ExcuseMyFrenchComposeTheme {
        InsultDisplay(viewModel = FakeViewModel(uiState))
    }
}

@Preview(showBackground = true, name = "Tablet Wide", device = "spec:width=1280dp,height=800dp,dpi=240")
@Composable
fun InsultDisplayTabletPreview() {
    ExcuseMyFrenchComposeTheme {
        InsultDisplay(viewModel = FakeViewModel(InsultUiState(insultText = "Espèce d'idiot !", imageBitmap = createPreviewBitmap(), isLoading = false)))
    }
}

@Preview(showBackground = true, name = "Dark Mode", uiMode = UI_MODE_NIGHT_YES)
@Composable
fun InsultDisplayDarkPreview() {
    ExcuseMyFrenchComposeTheme {
        InsultDisplay(viewModel = FakeViewModel(InsultUiState(insultText = "Espèce d'abruti !", isLoading = false)))
    }
}

@Preview(showBackground = true, name = "Loading State")
@Composable
fun InsultDisplayLoadingPreview() {
    ExcuseMyFrenchComposeTheme {
        InsultDisplay(viewModel = FakeViewModel(InsultUiState(isLoading = true)))
    }
}

@Preview(showBackground = true, name = "Error State")
@Composable
fun InsultDisplayErrorPreview() {
    ExcuseMyFrenchComposeTheme {
        InsultDisplay(viewModel = FakeViewModel(InsultUiState(error = "No internet connection. Please check your network.", isLoading = false)))
    }
}
