package com.example.excusemyfrenchcompose.ui.components

import com.example.excusemyfrenchcompose.R
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Color
import com.example.excusemyfrenchcompose.ui.theme.ExcluseMyFrenchComposeTheme
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import com.example.excusemyfrenchcompose.ui.viewmodel.InsultViewModelInterface
import com.example.excusemyfrenchcompose.ui.viewmodel.InsultUiState
import androidx.compose.ui.test.junit4.createComposeRule
import org.junit.Rule
import org.junit.Test
import androidx.compose.ui.test.*
import androidx.test.platform.app.InstrumentationRegistry


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

        composeTestRule.onNodeWithTag("loadingIndicator").assertIsDisplayed()
    }


    @Test
    fun insultDisplay_successState() {
        val testInsult = "Test Insult"
        val testBitmap = createTestBitmap()
        val fakeViewModel = FakeViewModel(InsultUiState(insultText = testInsult, imageBitmap = testBitmap, isLoading = false))

        composeTestRule.setContent {
            ExcluseMyFrenchComposeTheme{
                InsultDisplay(viewModel = fakeViewModel)
            }
        }

        composeTestRule.onNodeWithText(testInsult).assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Insult Image").assertIsDisplayed()
    }

    @Test
    fun insultDisplay_errorState() {
        // Get the context from InstrumentationRegistry
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val errorText = context.getString(R.string.could_not_load) // Use the correct string resource!
        val fakeViewModel = FakeViewModel(InsultUiState(error = errorText, isLoading = false))

        composeTestRule.setContent {
            ExcluseMyFrenchComposeTheme{
                InsultDisplay(viewModel = fakeViewModel)
            }
        }

        // Assert that the CORRECT error text is displayed.
        composeTestRule.onNodeWithText(errorText).assertIsDisplayed()

        // Placeholder image is NOT displayed when there's an error
        composeTestRule.onNodeWithContentDescription("Placeholder Image").assertDoesNotExist()
    }

    @Test
    fun insultDisplay_empty_state() {
        val fakeViewModel = FakeViewModel(InsultUiState(insultText = "No insult available", imageBitmap = null, error = null, isLoading = false))

        composeTestRule.setContent {
            ExcluseMyFrenchComposeTheme{
                InsultDisplay(viewModel = fakeViewModel)
            }
        }
        composeTestRule.onNodeWithText("No insult available").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Placeholder Image").assertIsDisplayed()
    }

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

// Corrected FakeViewModel
class FakeViewModel(private val state: InsultUiState) : InsultViewModelInterface {
    override val uiState: StateFlow<InsultUiState> = MutableStateFlow(state).asStateFlow()

    override fun toggleMute() {
        // Add a (usually empty) implementation for testing.
    }
    override fun speak(text: String) {
        // Add empty implementation
    }
}