package com.example.excusemyfrenchcompose.ui.viewmodel

import android.app.Application
import android.speech.tts.TextToSpeech
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.example.excusemyfrenchcompose.data.remote.InsultApiService
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.impl.annotations.MockK
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
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
import io.mockk.every
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.runCurrent

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

    private lateinit var testDispatcher: TestDispatcher // Use a TestDispatcher


    @Before
    fun setup() {
        MockKAnnotations.init(this)
        testDispatcher = StandardTestDispatcher() // Initialize the TestDispatcher
        Dispatchers.setMain(testDispatcher) // Set the TestDispatcher as the Main dispatcher

        mockkStatic(Log::class)
        every { Log.d(any(), any()) } returns 0
        every { Log.e(any(), any()) } returns 0
        every { Log.e(any(), any(), any()) } returns 0


        viewModel = InsultViewModel(mockApplication, mockApiService)
        viewModel.initializeTTSForTest(mockTTS)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        unmockkAll()
    }


    @Test
    fun `fetchInsult successfully updates uiState`() = runTest(testDispatcher) { // Pass testDispatcher
        // Arrange
        coEvery { mockApiService.fetchInsult() } returns "{\"insult\": { \"text\": \"Test Insult\", \"index\": 1}, \"image\": { \"data\": \"test_data\", \"mimetype\": \"image/jpeg\", \"indexImg\": 2} }"

        // Act
        viewModel.fetchInsult() // Don't call fetchInsultRepeatedly directly
        runCurrent() // Run pending tasks *immediately*
        advanceUntilIdle() // Then advance time until all tasks are complete

        // Assert
        val uiState = viewModel.uiState.value
        assertEquals("Test Insult", uiState.insultText)
        assertFalse(uiState.isLoading)
        assertNull(uiState.error)
    }


    @Test
    fun `fetchInsult with null response updates uiState with error`() = runTest(testDispatcher) { // Pass testDispatcher
        coEvery { mockApiService.fetchInsult() } returns null

        viewModel.fetchInsult()
        runCurrent() // Run pending tasks
        advanceUntilIdle() // Advance time

        val uiState = viewModel.uiState.value
        assertEquals("Error: Empty response body", uiState.error)
        assertTrue(uiState.insultText.isEmpty())
        assertNull(uiState.imageBitmap)
        assertFalse(uiState.isLoading) //Should not
    }

    @Test
    fun `fetchInsult with network error updates uiState with error`() = runTest(testDispatcher) { // Pass testDispatcher
        coEvery { mockApiService.fetchInsult() } throws IOException("Network error")

        viewModel.fetchInsult()
        runCurrent() // Run pending tasks
        advanceUntilIdle() // Advance time


        val uiState = viewModel.uiState.value
        assertEquals("Error: java.io.IOException: Network error", uiState.error)
        assertTrue(uiState.insultText.isEmpty())
        assertNull(uiState.imageBitmap)
        assertFalse(uiState.isLoading)
    }

    @Test
    fun `toggleMute updates isMuted state`() = runTest(testDispatcher) { // Pass testDispatcher
        // Initial state should be muted (true)
        assertTrue(viewModel.uiState.value.isMuted)

        // Toggle mute (should become unmuted - false)
        viewModel.toggleMute()
        runCurrent() // Run pending tasks
        advanceUntilIdle() // Advance Time
        assertFalse(viewModel.uiState.value.isMuted)

        // Toggle again (should become muted - true)
        viewModel.toggleMute()
        runCurrent() // Run pending tasks
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.isMuted)
    }

    @Test
    fun `speak is called when unmuted and insult is fetched`() = runTest(testDispatcher) { // Pass testDispatcher

        val testInsultText = "Test Insult"
        coEvery { mockApiService.fetchInsult() } returns "{\"insult\": { \"text\": \"$testInsultText\", \"index\": 1}, \"image\": { \"data\": \"test_data\", \"mimetype\": \"image/jpeg\", \"indexImg\": 2} }"

        // Act: Unmute and fetch
        viewModel.toggleMute() // Unmute first
        runCurrent() // Run pending tasks
        advanceUntilIdle()
        viewModel.fetchInsult()
        runCurrent() // Run pending tasks related to fetchInsult and speak
        advanceUntilIdle()


        // Assert: Verify that speak() was called with the correct text
        verify { mockTTS.speak(testInsultText, TextToSpeech.QUEUE_FLUSH, null, "") }
    }

    @Test
    fun `speak is not called when muted`() = runTest(testDispatcher) {// Pass testDispatcher

        val testInsultText = "Test Insult"
        coEvery { mockApiService.fetchInsult() } returns "{\"insult\": { \"text\": \"$testInsultText\", \"index\": 1}, \"image\": { \"data\": \"test_data\", \"mimetype\": \"image/jpeg\", \"indexImg\": 2} }"

        // Act: Fetch insult while muted
        viewModel.fetchInsult()
        runCurrent() // Run pending tasks
        advanceUntilIdle()

        // Assert: Verify that speak() was NOT called
        verify(exactly = 0) { mockTTS.speak(any(), any(), any(), any()) }

        // Act: Unmute, then mute again, then fetch
        viewModel.toggleMute()
        viewModel.toggleMute()
        runCurrent()
        advanceUntilIdle()
        viewModel.fetchInsult()
        runCurrent()
        advanceUntilIdle()


        // Assert: Verify that speak() was still NOT called
        verify(exactly = 0) { mockTTS.speak(any(), any(), any(), any()) }
    }


    @Test
    fun `TTS initialization error updates uiState`() = runTest(testDispatcher) { // Pass testDispatcher
        // Simulate initialization failure.  Because we use a relaxed mock,
        // we don't need to stub setLanguage().  It will just return a default value.
        every { mockTTS.setLanguage(any()) } returns TextToSpeech.ERROR

        // Act: Unmute to trigger initialization.
        viewModel.toggleMute()
        runCurrent()
        advanceUntilIdle()

        // Assert: UI state should have an error.
        val uiState = viewModel.uiState.value
        assertNotNull(uiState.error)
        assertTrue(uiState.error!!.contains("TTS Initialization failed"))
    }

    // Helper function (no changes needed)
    fun InsultViewModel.initializeTTSForTest(mockTTS: TextToSpeech) {
        val field = this.javaClass.getDeclaredField("tts")
        field.isAccessible = true
        field.set(this, mockTTS)
    }
}