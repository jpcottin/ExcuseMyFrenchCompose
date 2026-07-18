package com.example.excusemyfrenchcompose.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import com.android.tools.screenshot.PreviewTest
import com.example.excusemyfrenchcompose.ui.theme.ExcuseMyFrenchComposeTheme
import com.example.excusemyfrenchcompose.ui.viewmodel.InsultUiState

@Preview(name = "Phone", device = Devices.PHONE, showBackground = true)
@Preview(name = "Foldable", device = Devices.FOLDABLE, showBackground = true)
@Preview(name = "Tablet", device = Devices.TABLET, showBackground = true)
@Preview(name = "Desktop", device = Devices.DESKTOP, showBackground = true)
annotation class FormFactorPreviews

private fun contentState() = InsultUiState(
    insultText = "Espèce d'idiot !",
    imageBitmap = null,
    isLoading = false
)

@PreviewTest
@FormFactorPreviews
@Composable
fun InsultDisplayFormFactors() {
    ExcuseMyFrenchComposeTheme {
        InsultDisplay(viewModel = FakeViewModel(contentState()))
    }
}

@PreviewTest
@FormFactorPreviews
@Composable
fun InsultDisplayLoadingFormFactors() {
    ExcuseMyFrenchComposeTheme {
        InsultDisplay(viewModel = FakeViewModel(InsultUiState(isLoading = true)))
    }
}

@PreviewTest
@FormFactorPreviews
@Composable
fun InsultDisplayErrorFormFactors() {
    ExcuseMyFrenchComposeTheme {
        InsultDisplay(
            viewModel = FakeViewModel(
                InsultUiState(error = "No internet connection. Please check your network.", isLoading = false)
            )
        )
    }
}
