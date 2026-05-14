package com.example.excusemyfrenchcompose.service

import android.content.Context
import android.speech.tts.TextToSpeech
import com.example.excusemyfrenchcompose.R
import java.util.Locale

interface TtsService {
    fun initialize(onError: (String) -> Unit)
    fun speak(text: String)
    fun shutdown()
}

class TtsServiceImpl(private val context: Context) : TtsService {
    private var tts: TextToSpeech? = null
    private val TTS_UTTERANCE_ID = "insult_speech"

    override fun initialize(onError: (String) -> Unit) {
        if (tts != null) return
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                val result = tts?.setLanguage(Locale.FRANCE)
                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    onError(context.getString(R.string.tts_language_not_supported))
                }
            } else {
                onError(context.getString(R.string.tts_init_failed))
            }
        }
    }

    override fun speak(text: String) {
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, TTS_UTTERANCE_ID)
    }

    override fun shutdown() {
        tts?.stop()
        tts?.shutdown()
        tts = null
    }
}
