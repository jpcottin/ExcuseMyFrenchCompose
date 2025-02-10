package com.example.excusemyfrenchcompose.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.excusemyfrenchcompose.R
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Color
import com.example.excusemyfrenchcompose.ui.theme.ExcluseMyFrenchComposeTheme
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import com.example.excusemyfrenchcompose.ui.viewmodel.InsultViewModelInterface
import com.example.excusemyfrenchcompose.ui.viewmodel.InsultUiState
import androidx.compose.ui.platform.testTag
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp


@Composable
fun InsultDisplay(viewModel: InsultViewModelInterface, modifier: Modifier = Modifier) {

    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Text (at least 15% of the screen height, centered vertically and horizontally)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = with(LocalDensity.current) { (0.15f * context.resources.displayMetrics.heightPixels).toDp() })
                    .wrapContentHeight(Alignment.CenterVertically),
                contentAlignment = Alignment.Center

            ) {
                Text(
                    text = uiState.insultText,
                    style = MaterialTheme.typography.headlineLarge,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(16.dp))

            if (uiState.isLoading) {
                CircularProgressIndicator(modifier = Modifier.testTag("loadingIndicator"))
            } else {
                val imageBitmap = uiState.imageBitmap
                // Image (constrained to 90% of remaining width/height)
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {

                    if (imageBitmap != null) {
                        Image(
                            bitmap = imageBitmap,
                            contentDescription = "Insult Image",
                            contentScale = ContentScale.Fit,
                            modifier = Modifier
                                .aspectRatio(imageBitmap.width.toFloat() / imageBitmap.height.toFloat())
                                .fillMaxWidth(0.9f)
                                .fillMaxHeight(0.9f)
                        )
                    } else {
                        Image(
                            painter = painterResource(id = R.drawable.ic_launcher_foreground),
                            contentDescription = "Placeholder Image",
                            contentScale = ContentScale.Fit,
                            modifier = Modifier
                                .fillMaxWidth(0.9f)
                                .fillMaxHeight(0.9f)
                                .aspectRatio(1f)
                        )
                        Text("Error displaying image or decoding Base64 data.")
                    }
                }
            }
        }
        // Mute/Unmute Button (Lower-Right Corner)
        IconButton(
            onClick = { viewModel.toggleMute() },
            modifier = Modifier
                .align(Alignment.BottomEnd) // Align to bottom-end (lower-right)

        ) {
            Icon(
                imageVector = if (uiState.isMuted) Icons.Filled.VolumeOff else Icons.Filled.VolumeUp,
                contentDescription = if (uiState.isMuted) "Unmute" else "Mute",
                tint = MaterialTheme.colorScheme.onSurface // Use a suitable color
            )
        }
    }
}
class InsultUiStatePreviewProvider : PreviewParameterProvider<InsultUiState> {
    override val values: Sequence<InsultUiState> = sequenceOf(
        InsultUiState(isLoading = true), // Loading state
        InsultUiState(insultText = "Test Insult", imageBitmap = createTestBitmap(), isLoading = false), // Success state
        InsultUiState(insultText = "Another Test Insult", imageBitmap = createTestBitmap(Color.Red), isLoading = false), // Another success
        InsultUiState(error = "Error: Network connection failed", isLoading = false), // Error state
        InsultUiState(insultText = "", imageBitmap = null, error = null, isLoading = false) // No insult, no image

    )

    // Helper function to create a simple ImageBitmap for preview purposes.
    private fun createTestBitmap(color: Color = Color.Blue): ImageBitmap {
        val width = 200
        val height = 100
        val bitmap = ImageBitmap(width, height, androidx.compose.ui.graphics.ImageBitmapConfig.Argb8888)
        val canvas = androidx.compose.ui.graphics.Canvas(bitmap)
        canvas.drawRect(
            left = 0f,
            top = 0f,
            right = width.toFloat(),
            bottom = height.toFloat(),
            paint = androidx.compose.ui.graphics.Paint().apply {
                this.color = color
            }
        )
        return bitmap
    }
}

// Create FakeViewModel for Preview
class FakeViewModel(private val state: InsultUiState) : InsultViewModelInterface {
    override val uiState: StateFlow<InsultUiState> = MutableStateFlow(state).asStateFlow()
    override fun toggleMute() {
        // Mock implementation for preview
    }
    override fun speak(text: String) {
        // Mock implementation for preview
    }
}


@Preview(showBackground = true)
@Composable
fun InsultDisplayPreview(
    @PreviewParameter(InsultUiStatePreviewProvider::class) uiState: InsultUiState
) {
    ExcluseMyFrenchComposeTheme {
        InsultDisplay(viewModel = FakeViewModel(uiState))
    }
}