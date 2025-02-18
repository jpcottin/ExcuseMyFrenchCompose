package com.example.excusemyfrenchcompose.ui.viewmodel

import android.app.Application
import android.speech.tts.TextToSpeech
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.example.excusemyfrenchcompose.R
import com.example.excusemyfrenchcompose.data.remote.InsultApiService
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.impl.annotations.MockK
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.io.IOException
import io.mockk.mockkStatic
import android.util.Log
import app.cash.turbine.test // Import Turbine's test function
import io.mockk.every
import kotlinx.coroutines.test.TestDispatcher

@ExperimentalCoroutinesApi
class InsultViewModelTest {

    @get:Rule
    val instantExecutorRule = InstantTaskExecutorRule()

    private lateinit var viewModel: InsultViewModel
    @MockK
    private lateinit var mockApiService: InsultApiService

    @MockK(relaxed = true)
    private lateinit var mockTTS: TextToSpeech

    @MockK
    private lateinit var mockApplication: Application
    private lateinit var testDispatcher: TestDispatcher


    @Before
    fun setup() {
        MockKAnnotations.init(this)
        testDispatcher = StandardTestDispatcher() //Use StandardTestDispatcher
        Dispatchers.setMain(testDispatcher)

        mockkStatic(Log::class)
        every { Log.d(any(), any()) } returns 0
        every { Log.e(any(), any()) } returns 0
        every { Log.e(any(), any(), any()) } returns 0

        val fakeResourceProvider = object : ResourceProvider {
            override fun getString(resId: Int): String {
                return when (resId) {
                    R.string.could_not_load -> "Could not load insult"
                    R.string.no_internet -> "No internet connection: %s"
                    R.string.tts_init_failed -> "TTS Initialization failed"
                    R.string.tts_language_not_supported -> "French language is not supported"
                    R.string.image_decoding_error -> "The image could not be decoded."
                    R.string.no_insult_available -> "No insult available"
                    else -> "Unknown resource ID"
                }
            }

            override fun getString(resId: Int, vararg formatArgs: Any): String {
                return when (resId) {
                    R.string.no_internet -> String.format("No internet connection: %s", *formatArgs)
                    else -> getString(resId)
                }
            }
        }

        viewModel = InsultViewModel(mockApplication, mockApiService, fakeResourceProvider)
        viewModel.initializeTTSForTest(mockTTS)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        unmockkAll()
    }

    @Test
    fun `fetchInsult successfully updates uiState`() = runTest {
        val testInsult = "Test Insult"
        coEvery { mockApiService.fetchInsult() } returns "{\"insult\": { \"text\": \"$testInsult\", \"index\": 1}, \"image\": { \"data\": \"test_data\", \"mimetype\": \"image/jpeg\", \"indexImg\": 2} }"

        viewModel.uiState.test {
            //Initial state should be loading
            assertEquals(true, awaitItem().isLoading)
            viewModel.fetchInsult()  // Trigger the fetch
            assertEquals(testInsult, awaitItem().insultText) // The next emission *should* be the success state.
            assertNull(expectMostRecentItem().error) //Final state, useful if there is no delay
            cancelAndIgnoreRemainingEvents() // Good practice
        }
    }

    @Test
    fun `fetchInsult with null response updates uiState with error`() = runTest {
        coEvery { mockApiService.fetchInsult() } returns null

        viewModel.uiState.test {
            assertEquals(true, awaitItem().isLoading)
            viewModel.fetchInsult()
            val errorState = awaitItem()
            assertEquals("Could not load insult", errorState.error)
            assertEquals("No insult available", errorState.insultText)
            assertNull(errorState.imageBitmap)
            assertFalse(errorState.isLoading)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `fetchInsult with network error updates uiState with error`() = runTest {
        val exceptionMessage = "Network error"
        coEvery { mockApiService.fetchInsult() } throws IOException(exceptionMessage)

        viewModel.uiState.test {
            assertEquals(true, awaitItem().isLoading)
            viewModel.fetchInsult()
            val errorState = awaitItem()
            assertEquals("No internet connection: $exceptionMessage", errorState.error)
            assertEquals("No insult available", errorState.insultText)
            assertNull(errorState.imageBitmap)
            assertFalse(errorState.isLoading)
            cancelAndIgnoreRemainingEvents()
        }
    }
    @Test
    fun `toggleMute updates isMuted state`() = runTest {
        viewModel.uiState.test {
            assertTrue(awaitItem().isMuted) // Initial state
            viewModel.toggleMute()
            assertFalse(awaitItem().isMuted) // Check for the update
            viewModel.toggleMute()
            assertTrue(awaitItem().isMuted)  // Check for toggling back
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `speak is called when unmuted and insult is fetched`() = runTest {
        val testInsultText = "Test Insult"
        coEvery { mockApiService.fetchInsult() } returns "{\"insult\": { \"text\": \"$testInsultText\", \"index\": 1}, \"image\": { \"data\": \"test_data\", \"mimetype\": \"image/jpeg\", \"indexImg\": 2} }"

        viewModel.uiState.test {
            awaitItem() //Initial
            viewModel.toggleMute() // Unmute
            awaitItem() //wait for mute state to be updated
            viewModel.fetchInsult() // Fetch the insult
            awaitItem() //Wait for the state update
            verify { mockTTS.speak(testInsultText, TextToSpeech.QUEUE_FLUSH, null, "") }
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `speak is not called when muted`() = runTest {
        val testInsultText = "Test Insult"
        coEvery { mockApiService.fetchInsult() } returns "{\"insult\": { \"text\": \"$testInsultText\", \"index\": 1}, \"image\": { \"data\": \"test_data\", \"mimetype\": \"image/jpeg\", \"indexImg\": 2} }"

        viewModel.uiState.test{
            awaitItem() // Initial State
            viewModel.fetchInsult() // Fetch while muted
            awaitItem() // Wait result
            verify(exactly = 0) { mockTTS.speak(any(), any(), any(), any()) }

            viewModel.toggleMute() //unmute
            awaitItem() // wait mute update
            viewModel.toggleMute() //mute again
            awaitItem()//wait mute update
            viewModel.fetchInsult() //fetch again
            awaitItem() // Wait result
            verify(exactly = 0) { mockTTS.speak(any(), any(), any(), any()) }
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `TTS initialization error updates uiState`() = runTest {
        every { mockTTS.setLanguage(any()) } returns TextToSpeech.ERROR

        viewModel.uiState.test {
            awaitItem() //Initial State
            viewModel.toggleMute() // Trigger TTS initialization
            val errorState = awaitItem() // Should emit the error state
            assertNotNull(errorState.error)
            assertEquals("TTS Initialization failed", errorState.error)
            cancelAndIgnoreRemainingEvents()
        }
    }

    // Helper function
    fun InsultViewModel.initializeTTSForTest(mockTTS: TextToSpeech) {
        val field = this.javaClass.getDeclaredField("tts")
        field.isAccessible = true
        field.set(this, mockTTS)
    }
}