package io.github.jpcottin.excusemyfrench.service

import android.content.Context
import android.speech.tts.TextToSpeech
import io.github.jpcottin.excusemyfrench.R
import java.util.Locale

interface TtsService {
    fun initialize(onError: (String) -> Unit)
    fun speak(text: String)
    /** Stops what is being spoken and forgets anything queued; the engine stays ready. */
    fun stop()
    fun shutdown()
}

/** How a [TextToSpeech] engine is created; the default is the platform constructor. */
typealias TtsEngineFactory = (Context, TextToSpeech.OnInitListener) -> TextToSpeech

class TtsServiceImpl(
    private val context: Context,
    private val engineFactory: TtsEngineFactory = { ctx, listener -> TextToSpeech(ctx, listener) },
) : TtsService {
    private var tts: TextToSpeech? = null
    // The engine initializes asynchronously; text spoken before it is ready is held here and
    // spoken from the init callback so the first utterance after unmuting is not dropped.
    private var isReady = false
    private var pendingText: String? = null
    private val TTS_UTTERANCE_ID = "insult_speech"

    override fun initialize(onError: (String) -> Unit) {
        if (tts != null) return
        // A failed engine is discarded so the next initialize() creates a fresh one and reports
        // again; keeping it would make every later attempt a silent no-op. The listener may run
        // before the constructor returns, when `tts` is still null, hence the flag.
        var failed = false
        val engine = engineFactory(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                val result = tts?.setLanguage(Locale.FRANCE)
                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    failed = true
                    onError(context.getString(R.string.tts_language_not_supported))
                    discardEngine()
                } else {
                    isReady = true
                    pendingText?.let { speak(it) }
                    pendingText = null
                }
            } else {
                failed = true
                onError(context.getString(R.string.tts_init_failed))
                discardEngine()
            }
        }
        tts = engine
        if (failed) discardEngine()
    }

    override fun speak(text: String) {
        if (isReady) {
            tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, TTS_UTTERANCE_ID)
        } else {
            pendingText = text
        }
    }

    override fun stop() {
        pendingText = null
        tts?.stop()
    }

    override fun shutdown() {
        pendingText = null
        discardEngine()
    }

    private fun discardEngine() {
        isReady = false
        tts?.stop()
        tts?.shutdown()
        tts = null
    }
}
