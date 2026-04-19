package com.example.excusemyfrenchcompose.ui.viewmodel

import android.app.Application
import android.speech.tts.TextToSpeech
import android.util.Log
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.viewModelScope
import com.example.excusemyfrenchcompose.R
import com.example.excusemyfrenchcompose.data.remote.InsultApiService
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.io.IOException

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
        testDispatcher = StandardTestDispatcher()
        Dispatchers.setMain(testDispatcher)

        mockkStatic(Log::class)
        every { Log.d(any(), any()) } returns 0
        every { Log.e(any(), any()) } returns 0
        every { Log.e(any(), any(), any()) } returns 0

        every { mockApplication.getString(R.string.could_not_load) } returns "Could not load insult.  Please try again later."
        every { mockApplication.getString(R.string.no_internet) } returns "No internet connection. Please check your network."
        every { mockApplication.getString(R.string.tts_init_failed) } returns "TTS Initialization failed."
        every { mockApplication.getString(R.string.tts_language_not_supported) } returns "French language is not supported."
        every { mockApplication.getString(R.string.image_decoding_error) } returns "Error displaying image or decoding Base64 data."

        viewModel = InsultViewModel(mockApplication, mockApiService)
        viewModel.initializeTTSForTest(mockTTS)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        unmockkAll()
    }

    @Test
    fun `fetchInsult successfully updates uiState`() = runTest(testDispatcher) {
        coEvery { mockApiService.fetchInsult() } returns "{\"insult\": { \"text\": \"Test Insult\", \"index\": 1}, \"image\": { \"data\": \"test_data\", \"mimetype\": \"image/jpeg\", \"indexImg\": 2} }"

        viewModel.fetchInsult()

        val uiState = viewModel.uiState.value
        assertEquals("Test Insult", uiState.insultText)
        assertFalse(uiState.isLoading)
        assertNull(uiState.error)

        viewModel.viewModelScope.cancel()
    }

    @Test
    fun `fetchInsult with null response updates uiState with error`() = runTest(testDispatcher) {
        coEvery { mockApiService.fetchInsult() } returns null

        viewModel.fetchInsult()

        val uiState = viewModel.uiState.value
        assertEquals("Could not load insult.  Please try again later.", uiState.error)
        assertTrue(uiState.insultText.isEmpty())
        assertNull(uiState.imageBitmap)
        assertFalse(uiState.isLoading)

        viewModel.viewModelScope.cancel()
    }

    @Test
    fun `fetchInsult with network error updates uiState with error`() = runTest(testDispatcher) {
        coEvery { mockApiService.fetchInsult() } throws IOException("Network error")

        viewModel.fetchInsult()

        val uiState = viewModel.uiState.value
        assertEquals("No internet connection. Please check your network.", uiState.error)
        assertTrue(uiState.insultText.isEmpty())
        assertNull(uiState.imageBitmap)
        assertFalse(uiState.isLoading)

        viewModel.viewModelScope.cancel()
    }

    @Test
    fun `toggleMute updates isMuted state`() = runTest(testDispatcher) {
        assertTrue(viewModel.uiState.value.isMuted)

        viewModel.toggleMute()
        assertFalse(viewModel.uiState.value.isMuted)

        viewModel.toggleMute()
        assertTrue(viewModel.uiState.value.isMuted)

        viewModel.viewModelScope.cancel()
    }

    @Test
    fun `speak is called when unmuted and insult is fetched`() = runTest(testDispatcher) {
        val testInsultText = "Test Insult"
        coEvery { mockApiService.fetchInsult() } returns "{\"insult\": { \"text\": \"$testInsultText\", \"index\": 1}, \"image\": { \"data\": \"test_data\", \"mimetype\": \"image/jpeg\", \"indexImg\": 2} }"

        viewModel.toggleMute()
        viewModel.fetchInsult()

        verify { mockTTS.speak(testInsultText, TextToSpeech.QUEUE_FLUSH, null, "insult_speech") }

        viewModel.viewModelScope.cancel()
    }

    @Test
    fun `speak is not called when muted`() = runTest(testDispatcher) {
        val testInsultText = "Test Insult"
        coEvery { mockApiService.fetchInsult() } returns "{\"insult\": { \"text\": \"$testInsultText\", \"index\": 1}, \"image\": { \"data\": \"test_data\", \"mimetype\": \"image/jpeg\", \"indexImg\": 2} }"

        viewModel.fetchInsult()
        verify(exactly = 0) { mockTTS.speak(any(), any(), any(), any()) }

        viewModel.toggleMute()
        viewModel.toggleMute()
        viewModel.fetchInsult()
        verify(exactly = 0) { mockTTS.speak(any(), any(), any(), any()) }

        viewModel.viewModelScope.cancel()
    }

    @Test
    fun `TTS language not supported updates uiState with error`() = runTest(testDispatcher) {
        every { mockTTS.setLanguage(any()) } returns TextToSpeech.LANG_MISSING_DATA

        // configureTTSLanguage() is private — invoke via reflection
        val method = InsultViewModel::class.java.getDeclaredMethod("configureTTSLanguage")
        method.isAccessible = true
        method.invoke(viewModel)

        val uiState = viewModel.uiState.value
        assertNotNull(uiState.error)
        assertTrue(uiState.error!!.contains("French language is not supported"))

        viewModel.viewModelScope.cancel()
    }

    private fun InsultViewModel.initializeTTSForTest(mockTTS: TextToSpeech) {
        val field = this.javaClass.getDeclaredField("tts")
        field.isAccessible = true
        field.set(this, mockTTS)
    }
}
