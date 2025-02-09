package com.example.excusemyfrenchcompose.ui.viewmodel

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.example.excusemyfrenchcompose.data.model.Image
import com.example.excusemyfrenchcompose.data.model.Insult
import com.example.excusemyfrenchcompose.data.model.InsultResponse
import com.example.excusemyfrenchcompose.data.remote.InsultApiService
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
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
import io.mockk.every //For mocking
import io.mockk.mockkStatic //For mocking
import android.util.Log
import io.mockk.unmockkAll

@ExperimentalCoroutinesApi // Needed for runTest and Dispatchers.setMain
class InsultViewModelTest {

    // Executes tasks in the Architecture Components (like LiveData) synchronously.
    @get:Rule
    val instantExecutorRule = InstantTaskExecutorRule()

    private lateinit var viewModel: InsultViewModel
    private lateinit var mockApiService: InsultApiService

    @Before
    fun setup() {
        // Set the Main dispatcher to a test dispatcher.
        Dispatchers.setMain(StandardTestDispatcher())

        // Mock the Log class
        mockkStatic(Log::class)
        every { Log.d(any(), any()) } returns 0
        every { Log.e(any(), any()) } returns 0
        every { Log.e(any(), any(), any()) } returns 0  // Mock the version with throwable

        // Create a *mock* of the InsultApiService.  We don't want to make real network calls in our unit tests.
        mockApiService = mockk()

        // Create the ViewModel, passing in the mock service.
        viewModel = InsultViewModel(apiService = mockApiService)

    }

    @After
    fun tearDown() {
        Dispatchers.resetMain() // Reset the main dispatcher to the original one
        unmockkAll() // Unmock everything after each test
    }
    @Test
    fun `fetchInsult successfully updates uiState`() = runTest {
        // Arrange:  Define what the mock service should return.
        val testInsult = Insult("Test Insult", 1)
        val testImage = Image("test_data", "image/jpeg", 2)
        val testResponse = InsultResponse(testInsult, testImage)
        // coEvery is used for suspend functions
        coEvery { mockApiService.fetchInsult() } returns "{\"insult\": { \"text\": \"Test Insult\" , \"index\": 1}, \"image\": { \"data\": \"test_data\", \"mimetype\" : \"image/jpeg\",  \"indexImg\": 2} }"

        // Act: Call the function we want to test.
        viewModel.fetchInsult()
        advanceUntilIdle() // Important:  Wait for all coroutines to finish.

        // Assert: Check that the UI state is updated as expected.
        val uiState = viewModel.uiState.value
        assertEquals("Test Insult", uiState.insultText)
        //assertEquals("test_data", uiState.imageBitmap) // Can't compare directly
        assertFalse(uiState.isLoading)
        assertNull(uiState.error)
    }
    @Test
    fun `fetchInsult with null response updates uiState with error`() = runTest {
        coEvery { mockApiService.fetchInsult() } returns null

        viewModel.fetchInsult()
        advanceUntilIdle() // Important:  Wait for all coroutines to finish.


        val uiState = viewModel.uiState.value
        assertEquals("Error: Empty response body", uiState.error)
        assertTrue(uiState.insultText.isEmpty())
        assertNull(uiState.imageBitmap)
        assertFalse(uiState.isLoading) //Should not
    }
    @Test
    fun `fetchInsult with network error updates uiState with error`() = runTest {
        coEvery { mockApiService.fetchInsult() } throws IOException("Network error")

        viewModel.fetchInsult()
        advanceUntilIdle()

        val uiState = viewModel.uiState.value
        assertEquals("Error: java.io.IOException: Network error", uiState.error)
        assertTrue(uiState.insultText.isEmpty())
        assertNull(uiState.imageBitmap)
        assertFalse(uiState.isLoading)
    }
}