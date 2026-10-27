package io.github.jpcottin.excusemyfrench.ui.viewmodel

import android.app.Application
import android.util.Log
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.viewModelScope
import io.github.jpcottin.excusemyfrench.R
import io.github.jpcottin.excusemyfrench.data.model.Image
import io.github.jpcottin.excusemyfrench.data.model.Insult
import io.github.jpcottin.excusemyfrench.data.model.InsultResponse
import io.github.jpcottin.excusemyfrench.data.repository.InsultRepository
import io.github.jpcottin.excusemyfrench.data.settings.SettingsRepository
import io.github.jpcottin.excusemyfrench.service.TtsService
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import io.mockk.verify
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@ExperimentalCoroutinesApi
class InsultViewModelTest {

    @get:Rule
    val instantExecutorRule = InstantTaskExecutorRule()

    private lateinit var viewModel: InsultViewModel

    @MockK
    private lateinit var mockRepository: InsultRepository

    @MockK(relaxed = true)
    private lateinit var mockTtsService: TtsService

    @MockK(relaxed = true)
    private lateinit var mockSettings: SettingsRepository

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

        every { mockSettings.isMuted } returns flowOf(true)
        every { mockSettings.insultLevel } returns flowOf(1)

        viewModel = InsultViewModel(mockApplication, mockRepository, mockTtsService, mockSettings, decodeDispatcher = testDispatcher) { testDispatcher.scheduler.currentTime }
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        unmockkAll()
    }

    @Test
    fun `fetchInsult successfully updates uiState`() = runTest(testDispatcher) {
        coEvery { mockRepository.fetchInsult(any()) } returns InsultResponse(
            insult = Insult(text = "Test Insult", index = 1),
            image = Image(data = "test_data", mimetype = "image/jpeg", indexImg = 2)
        )

        viewModel.fetchInsult()

        val uiState = viewModel.uiState.value
        assertEquals("Test Insult", uiState.insultText)
        assertFalse(uiState.isLoading)
        assertNull(uiState.error)

        viewModel.viewModelScope.cancel()
    }

    @Test
    fun `fetchInsult with null response updates uiState with error`() = runTest(testDispatcher) {
        coEvery { mockRepository.fetchInsult(any()) } returns null

        viewModel.fetchInsult()

        val uiState = viewModel.uiState.value
        assertEquals("Could not load insult.  Please try again later.", uiState.error)
        assertTrue(uiState.insultText.isEmpty())
        assertNull(uiState.imageBitmap)
        assertFalse(uiState.isLoading)

        viewModel.viewModelScope.cancel()
    }

    @Test
    fun `fetchInsult with blank text shows the placeholder`() = runTest(testDispatcher) {
        coEvery { mockRepository.fetchInsult(any()) } returns InsultResponse(
            insult = Insult(text = "   ", index = 1),
            image = Image(data = "test_data", mimetype = "image/jpeg", indexImg = 2)
        )

        viewModel.fetchInsult()

        val uiState = viewModel.uiState.value
        assertEquals("No insult text provided", uiState.insultText)
        assertNull(uiState.error)
        assertFalse(uiState.isLoading)

        viewModel.viewModelScope.cancel()
    }

    @Test
    fun `fetchInsult with an unexpected exception reports could not load`() = runTest(testDispatcher) {
        coEvery { mockRepository.fetchInsult(any()) } throws IllegalStateException("malformed payload")

        viewModel.fetchInsult()

        val uiState = viewModel.uiState.value
        assertEquals("Could not load insult.  Please try again later.", uiState.error)
        assertTrue(uiState.insultText.isEmpty())
        assertFalse(uiState.isLoading)

        viewModel.viewModelScope.cancel()
    }

    @Test
    fun `autoRefresh fetches every interval and stops while paused`() = runTest(testDispatcher) {
        coEvery { mockRepository.fetchInsult(any()) } returns InsultResponse(
            insult = Insult(text = "Test Insult", index = 1),
            image = Image(data = "test_data", mimetype = "image/jpeg", indexImg = 2)
        )

        val job = backgroundScope.launch { viewModel.autoRefresh() }
        advanceTimeBy(25_000)   // ticks at 0 s, 10 s and 20 s
        coVerify(exactly = 3) { mockRepository.fetchInsult(any()) }

        viewModel.togglePause()
        assertTrue(viewModel.uiState.value.isPaused)
        advanceTimeBy(30_000)   // three more ticks, none of them fetches
        coVerify(exactly = 3) { mockRepository.fetchInsult(any()) }

        viewModel.togglePause()
        assertFalse(viewModel.uiState.value.isPaused)
        advanceTimeBy(10_000)   // the next tick fetches again
        coVerify(exactly = 4) { mockRepository.fetchInsult(any()) }

        job.cancel()
        viewModel.viewModelScope.cancel()
    }

    @Test
    fun `a restarted autoRefresh picks the interval up where it was`() = runTest(testDispatcher) {
        coEvery { mockRepository.fetchInsult(any()) } returns InsultResponse(
            insult = Insult(text = "Test Insult", index = 1),
            image = Image(data = "test_data", mimetype = "image/jpeg", indexImg = 2)
        )
        // The screen's clock: the test scheduler's time.
        val model = InsultViewModel(mockApplication, mockRepository, mockTtsService, mockSettings, decodeDispatcher = testDispatcher) { testScheduler.currentTime }

        val first = backgroundScope.launch { model.autoRefresh() }
        advanceTimeBy(1_000)
        coVerify(exactly = 1) { mockRepository.fetchInsult(any()) }
        first.cancel()                       // the activity is recreated (a rotation)...

        val second = backgroundScope.launch { model.autoRefresh() }
        advanceTimeBy(1_000)                 // ...and the new screen must not fetch again
        coVerify(exactly = 1) { mockRepository.fetchInsult(any()) }
        advanceTimeBy(8_500)                 // 10.5 s after the first fetch: the next tick
        coVerify(exactly = 2) { mockRepository.fetchInsult(any()) }

        second.cancel()
        model.viewModelScope.cancel()
    }

    @Test
    fun `fetchInsult with network error updates uiState with error`() = runTest(testDispatcher) {
        coEvery { mockRepository.fetchInsult(any()) } throws IOException("Network error")

        viewModel.fetchInsult()

        val uiState = viewModel.uiState.value
        assertEquals("No internet connection. Please check your network.", uiState.error)
        assertTrue(uiState.insultText.isEmpty())
        assertNull(uiState.imageBitmap)
        assertFalse(uiState.isLoading)

        viewModel.viewModelScope.cancel()
    }

    @Test
    fun `toggleMute persists the new value and starts TTS when unmuting`() = runTest(testDispatcher) {
        coEvery { mockSettings.setMuted(any()) } returns Unit
        every { mockTtsService.initialize(any()) } returns Unit

        viewModel.toggleMute()            // true -> false: unmuted
        advanceUntilIdle()
        coVerify(exactly = 1) { mockSettings.setMuted(false) }
        verify(exactly = 1) { mockTtsService.initialize(any()) }

        viewModel.toggleMute()            // false -> true: muted again, no TTS start
        advanceUntilIdle()
        coVerify(exactly = 1) { mockSettings.setMuted(true) }
        verify(exactly = 1) { mockTtsService.initialize(any()) }

        viewModel.viewModelScope.cancel()
    }

    @Test
    fun `the user's mute choice wins over a persisted value that arrives later`() = runTest(testDispatcher) {
        // The stored value is still being read when the user taps mute.
        every { mockSettings.isMuted } returns flow { delay(5_000); emit(true) }
        coEvery { mockSettings.setMuted(any()) } returns Unit
        every { mockTtsService.initialize(any()) } returns Unit
        val lateModel = InsultViewModel(mockApplication, mockRepository, mockTtsService, mockSettings, decodeDispatcher = testDispatcher) { testDispatcher.scheduler.currentTime }

        lateModel.toggleMute()            // default true -> false before the store answers
        assertFalse(lateModel.uiState.value.isMuted)
        advanceUntilIdle()                // the stored "true" arrives now
        assertFalse(lateModel.uiState.value.isMuted)

        lateModel.viewModelScope.cancel()
    }

    @Test
    fun `a persisted mute value is applied when the user has not chosen`() = runTest(testDispatcher) {
        every { mockSettings.isMuted } returns flowOf(false)
        val model = InsultViewModel(mockApplication, mockRepository, mockTtsService, mockSettings, decodeDispatcher = testDispatcher) { testDispatcher.scheduler.currentTime }
        assertTrue(model.uiState.value.isMuted)     // the default, until the store is read
        advanceUntilIdle()
        assertFalse(model.uiState.value.isMuted)

        model.viewModelScope.cancel()
    }

    @Test
    fun `muting stops the speech in progress and whatever was queued`() = runTest(testDispatcher) {
        advanceUntilIdle()
        viewModel.toggleMute()   // unmuted: the engine starts
        advanceUntilIdle()
        verify(exactly = 0) { mockTtsService.stop() }

        viewModel.toggleMute()   // muted again
        advanceUntilIdle()
        verify(exactly = 1) { mockTtsService.stop() }
        verify(exactly = 1) { mockTtsService.initialize(any()) }
    }

    @Test
    fun `a settings store that cannot be read leaves the defaults and does not crash`() = runTest(testDispatcher) {
        every { mockSettings.isMuted } returns flow { throw IOException("disk") }
        every { mockSettings.insultLevel } returns flow { throw IOException("disk") }
        val model = InsultViewModel(mockApplication, mockRepository, mockTtsService, mockSettings, decodeDispatcher = testDispatcher) { testDispatcher.scheduler.currentTime }
        advanceUntilIdle()
        assertTrue(model.uiState.value.isMuted)
        assertEquals(1, model.uiState.value.insultLevel)
        model.viewModelScope.cancel()
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
    fun `a level that cannot be saved still applies and refetches`() = runTest(testDispatcher) {
        coEvery { mockSettings.setInsultLevel(any()) } throws IOException("disk full")
        coEvery { mockRepository.fetchInsult(2) } returns InsultResponse(
            insult = Insult(text = "Level 2 Insult", index = 1, level = 2)
        )
        advanceUntilIdle()
        viewModel.setInsultLevel(2)
        advanceUntilIdle()
        coVerify(exactly = 1) { mockRepository.fetchInsult(2) }
        assertEquals("Level 2 Insult", viewModel.uiState.value.insultText)
        assertFalse(viewModel.uiState.value.isLoading)
        viewModel.viewModelScope.cancel()
    }

    @Test
    fun `a mute that cannot be saved still applies and does not crash`() = runTest(testDispatcher) {
        coEvery { mockSettings.setMuted(any()) } throws IOException("disk full")
        advanceUntilIdle()
        viewModel.toggleMute()
        advanceUntilIdle()
        assertFalse(viewModel.uiState.value.isMuted)
        viewModel.viewModelScope.cancel()
    }

    @Test
    fun `the first fetch waits for the saved level instead of using the default`() = runTest(testDispatcher) {
        // The store answers after the screen has started the loop, as on a cold start.
        every { mockSettings.insultLevel } returns flow { delay(500); emit(3) }
        val model = InsultViewModel(mockApplication, mockRepository, mockTtsService, mockSettings, decodeDispatcher = testDispatcher) { testScheduler.currentTime }

        val loop = backgroundScope.launch { model.autoRefresh() }
        advanceTimeBy(100)
        coVerify(exactly = 0) { mockRepository.fetchInsult(any()) }
        advanceTimeBy(1_000)
        coVerify(exactly = 1) { mockRepository.fetchInsult(3) }
        coVerify(exactly = 0) { mockRepository.fetchInsult(1) }

        loop.cancel()
        model.viewModelScope.cancel()
    }

    @Test
    fun `a level the store cannot read does not hold the first fetch`() = runTest(testDispatcher) {
        every { mockSettings.insultLevel } returns flow { delay(500); throw IOException("disk") }
        val model = InsultViewModel(mockApplication, mockRepository, mockTtsService, mockSettings, decodeDispatcher = testDispatcher) { testScheduler.currentTime }

        val loop = backgroundScope.launch { model.autoRefresh() }
        advanceTimeBy(1_000)
        coVerify(exactly = 1) { mockRepository.fetchInsult(1) }

        loop.cancel()
        model.viewModelScope.cancel()
    }

    @Test
    fun `a fetch cancelled with its screen is retried as soon as the loop restarts`() = runTest(testDispatcher) {
        coEvery { mockRepository.fetchInsult(any()) } coAnswers {
            delay(5_000); InsultResponse(insult = Insult(text = "Late", index = 1))
        }
        val model = InsultViewModel(mockApplication, mockRepository, mockTtsService, mockSettings, decodeDispatcher = testDispatcher) { testScheduler.currentTime }

        val first = backgroundScope.launch { model.autoRefresh() }
        advanceTimeBy(1_000)                 // the request is in flight...
        first.cancel()                       // ...when the screen is recreated

        val second = backgroundScope.launch { model.autoRefresh() }
        advanceTimeBy(1_000)
        coVerify(exactly = 2) { mockRepository.fetchInsult(any()) }
        advanceTimeBy(5_000)
        assertEquals("Late", model.uiState.value.insultText)
        assertFalse(model.uiState.value.isLoading)

        second.cancel()
        model.viewModelScope.cancel()
    }

    @Test
    fun `an insult that arrives after the screen has gone is shown but not spoken`() = runTest(testDispatcher) {
        coEvery { mockRepository.fetchInsult(any()) } coAnswers {
            delay(3_000); InsultResponse(insult = Insult(text = "Tard", index = 1))
        }
        advanceUntilIdle()
        viewModel.toggleMute()                                   // unmuted
        val loop = backgroundScope.launch { viewModel.autoRefresh() }
        advanceTimeBy(4_000)                                     // the first insult is shown and spoken
        verify(exactly = 1) { mockTtsService.speak(any()) }

        viewModel.fetchNext()                                    // Next on a slow network...
        advanceTimeBy(1_000)
        loop.cancel()                                            // ...then Home before the reply
        advanceUntilIdle()

        assertEquals("Tard", viewModel.uiState.value.insultText) // the content still lands
        verify(exactly = 1) { mockTtsService.speak(any()) }
        viewModel.viewModelScope.cancel()
    }

    @Test
    fun `speak is called when unmuted and insult is fetched`() = runTest(testDispatcher) {
        val testInsultText = "Test Insult"
        coEvery { mockRepository.fetchInsult(any()) } returns InsultResponse(
            insult = Insult(text = testInsultText, index = 1),
            image = Image(data = "test_data", mimetype = "image/jpeg", indexImg = 2)
        )

        viewModel.toggleMute()
        viewModel.fetchInsult()

        verify { mockTtsService.speak(testInsultText) }

        viewModel.viewModelScope.cancel()
    }

    @Test
    fun `speak is not called when muted`() = runTest(testDispatcher) {
        val testInsultText = "Test Insult"
        coEvery { mockRepository.fetchInsult(any()) } returns InsultResponse(
            insult = Insult(text = testInsultText, index = 1),
            image = Image(data = "test_data", mimetype = "image/jpeg", indexImg = 2)
        )

        viewModel.fetchInsult()
        verify(exactly = 0) { mockTtsService.speak(any()) }

        viewModel.toggleMute()
        viewModel.toggleMute()
        viewModel.fetchInsult()
        verify(exactly = 0) { mockTtsService.speak(any()) }

        viewModel.viewModelScope.cancel()
    }

    @Test
    fun `TTS initialization error updates uiState ttsError without hiding content`() = runTest(testDispatcher) {
        val errorMessage = "French language is not supported."
        every { mockTtsService.initialize(any()) } answers {
            firstArg<(String) -> Unit>().invoke(errorMessage)
        }

        viewModel.toggleMute() // This triggers ttsService.initialize

        val uiState = viewModel.uiState.value
        assertNotNull(uiState.ttsError)
        assertTrue(uiState.ttsError!!.contains(errorMessage))
        // The content error channel stays untouched so the insult remains visible.
        assertNull(uiState.error)

        viewModel.viewModelScope.cancel()
    }

    @Test
    fun `retryFetch resets loading state and fetches`() = runTest(testDispatcher) {
        coEvery { mockRepository.fetchInsult(any()) } returns InsultResponse(
            insult = Insult(text = "Retry Insult", index = 1),
            image = Image(data = "", mimetype = "", indexImg = 0)
        )

        viewModel.retryFetch()
        assertTrue(viewModel.uiState.value.isLoading)

        viewModel.viewModelScope.cancel()
    }

    @Test
    fun `persisted level is loaded on init`() = runTest(testDispatcher) {
        every { mockSettings.insultLevel } returns flowOf(3)

        val vm = InsultViewModel(mockApplication, mockRepository, mockTtsService, mockSettings, decodeDispatcher = testDispatcher) { testDispatcher.scheduler.currentTime }
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(3, vm.uiState.value.insultLevel)

        vm.viewModelScope.cancel()
    }

    @Test
    fun `setInsultLevel updates state, persists and refetches with new level`() = runTest(testDispatcher) {
        coEvery { mockRepository.fetchInsult(any()) } returns InsultResponse(
            insult = Insult(text = "Level 2 Insult", index = 1, level = 2),
            image = Image(data = "", mimetype = "", indexImg = 0)
        )

        viewModel.setInsultLevel(2)

        assertEquals(2, viewModel.uiState.value.insultLevel)
        assertTrue(viewModel.uiState.value.isLoading)

        testDispatcher.scheduler.advanceUntilIdle()

        coVerify { mockSettings.setInsultLevel(2) }
        coVerify { mockRepository.fetchInsult(2) }
        assertEquals("Level 2 Insult", viewModel.uiState.value.insultText)
        assertFalse(viewModel.uiState.value.isLoading)

        viewModel.viewModelScope.cancel()
    }

    @Test
    fun `setInsultLevel with unchanged level does not refetch`() = runTest(testDispatcher) {
        testDispatcher.scheduler.advanceUntilIdle()
        val currentLevel = viewModel.uiState.value.insultLevel

        viewModel.setInsultLevel(currentLevel)
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify(exactly = 0) { mockRepository.fetchInsult(any()) }
        coVerify(exactly = 0) { mockSettings.setInsultLevel(any()) }

        viewModel.viewModelScope.cancel()
    }

    @Test
    fun `fetchInsult uses current level`() = runTest(testDispatcher) {
        coEvery { mockRepository.fetchInsult(any()) } returns InsultResponse(
            insult = Insult(text = "Test Insult", index = 1, level = 1),
            image = Image(data = "", mimetype = "", indexImg = 0)
        )
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.fetchInsult()

        coVerify { mockRepository.fetchInsult(viewModel.uiState.value.insultLevel) }

        viewModel.viewModelScope.cancel()
    }
}
