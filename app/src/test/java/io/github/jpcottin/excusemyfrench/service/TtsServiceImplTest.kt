package io.github.jpcottin.excusemyfrench.service

import android.content.Context
import android.speech.tts.TextToSpeech
import io.github.jpcottin.excusemyfrench.R
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

/** The engine lifecycle behind [TtsServiceImpl]: readiness, queued text, failures and retries. */
class TtsServiceImplTest {

    private val context = mockk<Context>()
    private val engines = mutableListOf<TextToSpeech>()
    private val listeners = mutableListOf<TextToSpeech.OnInitListener>()
    private val errors = mutableListOf<String>()

    private val service = TtsServiceImpl(context) { _, listener ->
        listeners += listener
        mockk<TextToSpeech>(relaxed = true) {
            every { setLanguage(Locale.FRANCE) } returns TextToSpeech.LANG_AVAILABLE
        }.also { engines += it }
    }

    @Before
    fun setUp() {
        every { context.getString(R.string.tts_init_failed) } returns "TTS Initialization failed."
        every { context.getString(R.string.tts_language_not_supported) } returns "French language is not supported."
    }

    private fun initialize() = service.initialize { errors += it }

    @Test
    fun `text spoken before the engine is ready is spoken once it is`() {
        initialize()
        service.speak("Bonjour")
        verify(exactly = 0) { engines[0].speak(any(), any(), any(), any()) }

        listeners[0].onInit(TextToSpeech.SUCCESS)

        verify(exactly = 1) { engines[0].speak("Bonjour", TextToSpeech.QUEUE_FLUSH, null, any()) }
    }

    @Test
    fun `stop drops the queued text and stops the engine`() {
        initialize()
        service.speak("Bonjour")
        service.stop()
        listeners[0].onInit(TextToSpeech.SUCCESS)

        verify(exactly = 0) { engines[0].speak(any(), any(), any(), any()) }
        verify(exactly = 1) { engines[0].stop() }
    }

    @Test
    fun `a failed engine is reported and discarded so the next initialize retries`() {
        initialize()
        listeners[0].onInit(TextToSpeech.ERROR)
        assertEquals(listOf("TTS Initialization failed."), errors)
        verify(exactly = 1) { engines[0].shutdown() }

        initialize()
        assertEquals(2, engines.size)                        // a fresh engine, not a no-op
        listeners[1].onInit(TextToSpeech.SUCCESS)
        service.speak("Encore")
        verify(exactly = 1) { engines[1].speak("Encore", TextToSpeech.QUEUE_FLUSH, null, any()) }
    }

    @Test
    fun `a missing French voice is reported every time it is tried`() {
        val noFrench = TtsServiceImpl(context) { _, listener ->
            listeners += listener
            mockk<TextToSpeech>(relaxed = true) {
                every { setLanguage(Locale.FRANCE) } returns TextToSpeech.LANG_NOT_SUPPORTED
            }.also { engines += it }
        }
        noFrench.initialize { errors += it }
        listeners[0].onInit(TextToSpeech.SUCCESS)
        noFrench.initialize { errors += it }
        listeners[1].onInit(TextToSpeech.SUCCESS)

        assertEquals(listOf("French language is not supported.", "French language is not supported."), errors)
    }

    @Test
    fun `an engine that fails before its constructor returns is discarded too`() {
        val eager = TtsServiceImpl(context) { _, listener ->
            mockk<TextToSpeech>(relaxed = true).also { engines += it; listener.onInit(TextToSpeech.ERROR) }
        }
        eager.initialize { errors += it }
        eager.initialize { errors += it }

        assertEquals(2, errors.size)
        assertEquals(2, engines.size)
        verify(exactly = 1) { engines[0].shutdown() }
    }

    @Test
    fun `a healthy engine is initialized only once`() {
        initialize()
        listeners[0].onInit(TextToSpeech.SUCCESS)
        initialize()

        assertEquals(1, engines.size)
    }
}
