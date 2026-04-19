package com.example.excusemyfrenchcompose.ui.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.platform.app.InstrumentationRegistry
import com.example.excusemyfrenchcompose.R
import com.example.excusemyfrenchcompose.ui.theme.ExcuseMyFrenchComposeTheme
import com.example.excusemyfrenchcompose.ui.viewmodel.InsultUiState
import com.example.excusemyfrenchcompose.ui.viewmodel.InsultViewModelInterface
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.junit.Rule
import org.junit.Test

class InsultDisplayTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun insultDisplay_loadingState_showsLoadingIndicator() {
        composeTestRule.setContent {
            ExcuseMyFrenchComposeTheme {
                InsultDisplay(viewModel = FakeViewModel(InsultUiState(isLoading = true)))
            }
        }

        composeTestRule.onNodeWithTag("loadingIndicator").assertIsDisplayed()
    }

    @Test
    fun insultDisplay_successState_showsInsultAndImage() {
        val testInsult = "Test Insult"
        val testBitmap = createTestBitmap()
        composeTestRule.setContent {
            ExcuseMyFrenchComposeTheme {
                InsultDisplay(viewModel = FakeViewModel(InsultUiState(insultText = testInsult, imageBitmap = testBitmap, isLoading = false)))
            }
        }

        composeTestRule.onNodeWithText(testInsult).assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription(context.getString(R.string.insult_image)).assertIsDisplayed()
    }

    @Test
    fun insultDisplay_errorState_showsErrorAndNoPlaceholder() {
        val errorText = context.getString(R.string.could_not_load)
        composeTestRule.setContent {
            ExcuseMyFrenchComposeTheme {
                InsultDisplay(viewModel = FakeViewModel(InsultUiState(error = errorText, isLoading = false)))
            }
        }

        composeTestRule.onNodeWithTag("errorText").assertIsDisplayed()
        composeTestRule.onNodeWithText(errorText).assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription(context.getString(R.string.placeholder_image)).assertDoesNotExist()
    }

    @Test
    fun insultDisplay_emptyState_showsPlaceholder() {
        val noInsultText = "No insult available"
        composeTestRule.setContent {
            ExcuseMyFrenchComposeTheme {
                InsultDisplay(viewModel = FakeViewModel(InsultUiState(insultText = noInsultText, imageBitmap = null, error = null, isLoading = false)))
            }
        }

        composeTestRule.onNodeWithText(noInsultText).assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription(context.getString(R.string.placeholder_image)).assertIsDisplayed()
    }

    @Test
    fun insultDisplay_muteButton_isVisible() {
        composeTestRule.setContent {
            ExcuseMyFrenchComposeTheme {
                InsultDisplay(viewModel = FakeViewModel(InsultUiState(isMuted = true, isLoading = false)))
            }
        }

        composeTestRule.onNodeWithTag("muteButton").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription(context.getString(R.string.unmute)).assertIsDisplayed()
    }

    @Test
    fun insultDisplay_muteButton_togglesIcon() {
        val mutableState = MutableStateFlow(InsultUiState(isMuted = true, isLoading = false))
        val fakeViewModel = object : InsultViewModelInterface {
            override val uiState: StateFlow<InsultUiState> = mutableState.asStateFlow()
            override fun toggleMute() { mutableState.value = mutableState.value.copy(isMuted = !mutableState.value.isMuted) }
            override fun speak(text: String) {}
            override fun retryFetch() {}
        }

        composeTestRule.setContent {
            ExcuseMyFrenchComposeTheme {
                InsultDisplay(viewModel = fakeViewModel)
            }
        }

        composeTestRule.onNodeWithContentDescription(context.getString(R.string.unmute)).assertIsDisplayed()
        composeTestRule.onNodeWithTag("muteButton").performClick()
        composeTestRule.onNodeWithContentDescription(context.getString(R.string.mute)).assertIsDisplayed()
    }

    private fun createTestBitmap(color: Color = Color.Blue): ImageBitmap {
        val bitmap = ImageBitmap(200, 100, androidx.compose.ui.graphics.ImageBitmapConfig.Argb8888)
        val canvas = androidx.compose.ui.graphics.Canvas(bitmap)
        canvas.drawRect(0f, 0f, 200f, 100f, androidx.compose.ui.graphics.Paint().apply { this.color = color })
        return bitmap
    }
}

private class FakeViewModel(private val state: InsultUiState) : InsultViewModelInterface {
    override val uiState: StateFlow<InsultUiState> = MutableStateFlow(state).asStateFlow()
    override fun toggleMute() {}
    override fun speak(text: String) {}
    override fun retryFetch() {}
}
