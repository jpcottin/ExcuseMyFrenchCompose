package io.github.jpcottin.excusemyfrench.service

import android.content.Context
import android.speech.tts.TextToSpeech
import io.github.jpcottin.excusemyfrench.R
import java.util.Locale

interface TtsService {
    fun initialize(onError: (String) -> Unit)
    fun speak(text: String)
    fun shutdown()
}

class TtsServiceImpl(private val context: Context) : TtsService {
    private var tts: TextToSpeech? = null
    // The engine initializes asynchronously; text spoken before it is ready is held here and
    // spoken from the init callback so the first utterance after unmuting is not dropped.
    private var isReady = false
    private var pendingText: String? = null
    private val TTS_UTTERANCE_ID = "insult_speech"

    override fun initialize(onError: (String) -> Unit) {
        if (tts != null) return
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                val result = tts?.setLanguage(Locale.FRANCE)
                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    onError(context.getString(R.string.tts_language_not_supported))
                } else {
                    isReady = true
                    pendingText?.let { speak(it) }
                    pendingText = null
                }
            } else {
                onError(context.getString(R.string.tts_init_failed))
            }
        }
    }

    override fun speak(text: String) {
        if (isReady) {
            tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, TTS_UTTERANCE_ID)
        } else {
            pendingText = text
        }
    }

    override fun shutdown() {
        isReady = false
        pendingText = null
        tts?.stop()
        tts?.shutdown()
        tts = null
    }
}
