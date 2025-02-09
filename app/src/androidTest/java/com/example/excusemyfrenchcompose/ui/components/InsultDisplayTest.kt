package com.example.excusemyfrenchcompose.ui.components

import androidx.compose.ui.test.junit4.createComposeRule
import org.junit.Rule
import org.junit.Test
import androidx.compose.ui.test.*
import com.example.excusemyfrenchcompose.ui.theme.ExcluseMyFrenchComposeTheme
import com.example.excusemyfrenchcompose.ui.viewmodel.InsultViewModel
import com.example.excusemyfrenchcompose.ui.viewmodel.InsultUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Color
import com.example.excusemyfrenchcompose.ui.viewmodel.InsultViewModelInterface // Import

class InsultDisplayTest {

    @get:Rule
    val composeTestRule = createComposeRule()


    @Test
    fun insultDisplay_loadingState() {
        val fakeViewModel = FakeViewModel(InsultUiState(isLoading = true))
        composeTestRule.setContent {
            ExcluseMyFrenchComposeTheme {
                InsultDisplay(viewModel = fakeViewModel)
            }
        }

        // Assert that the CircularProgressIndicator is displayed.
        composeTestRule.onNodeWithTag("loadingIndicator").assertIsDisplayed() // Add testTag to find it
    }


    @Test
    fun insultDisplay_successState() {
        val testInsult = "Test Insult"
        //You should provide a real image
        val testBitmap = createTestBitmap() // Use the helper function!
        val fakeViewModel = FakeViewModel(InsultUiState(insultText = testInsult, imageBitmap = testBitmap, isLoading = false))

        composeTestRule.setContent {
            ExcluseMyFrenchComposeTheme{
                InsultDisplay(viewModel = fakeViewModel)
            }
        }

        // Assert that the insult text is displayed.
        composeTestRule.onNodeWithText(testInsult).assertIsDisplayed()

        // Assert that the image is displayed (using content description).
        composeTestRule.onNodeWithContentDescription("Insult Image").assertIsDisplayed()
    }

    @Test
    fun insultDisplay_errorState() {
        val errorText = "Error: Network connection failed"
        val fakeViewModel = FakeViewModel(InsultUiState(error = errorText, isLoading = false))

        composeTestRule.setContent {
            ExcluseMyFrenchComposeTheme{
                InsultDisplay(viewModel = fakeViewModel)
            }
        }

        // Assert that the error text is displayed.
        composeTestRule.onNodeWithText("Error displaying image or decoding Base64 data.").assertIsDisplayed()

        //Assert that the placeholder is displayed
        composeTestRule.onNodeWithContentDescription("Placeholder Image").assertIsDisplayed()

    }
    @Test
    fun insultDisplay_empty_state() {
        val fakeViewModel = FakeViewModel(InsultUiState(insultText = "No insult available", imageBitmap = null, error = null, isLoading = false))

        composeTestRule.setContent {
            ExcluseMyFrenchComposeTheme{
                InsultDisplay(viewModel = fakeViewModel)
            }
        }

        // Assert that the default text is displayed.
        composeTestRule.onNodeWithText("No insult available").assertIsDisplayed()

        //Assert that the placeholder is displayed
        composeTestRule.onNodeWithContentDescription("Placeholder Image").assertIsDisplayed()
    }

    // Helper function to create a simple ImageBitmap for testing purposes.  This is fine.
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

// Create FakeViewModel for Preview and testing
class FakeViewModel(private val state: InsultUiState) : InsultViewModelInterface {
    override val uiState: StateFlow<InsultUiState> = MutableStateFlow(state).asStateFlow()
}

