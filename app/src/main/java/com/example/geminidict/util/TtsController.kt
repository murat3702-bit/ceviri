package com.example.geminidict.util

import android.content.Context
import android.speech.tts.TextToSpeech
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import java.util.Locale

class TtsController(context: Context) {
    private var ready = false
    private val tts = TextToSpeech(context) { ready = it == TextToSpeech.SUCCESS }

    /** Başarılıysa true döner; motor hazır değilse veya dil verisi yoksa false. */
    fun speak(text: String, locale: Locale): Boolean {
        if (!ready) return false
        val r = tts.setLanguage(locale)
        if (r == TextToSpeech.LANG_MISSING_DATA || r == TextToSpeech.LANG_NOT_SUPPORTED) return false
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "utterance")
        return true
    }

    fun shutdown() {
        tts.stop()
        tts.shutdown()
    }
}

@Composable
fun rememberTts(): TtsController {
    val ctx = LocalContext.current.applicationContext
    val tts = remember { TtsController(ctx) }
    DisposableEffect(Unit) { onDispose { tts.shutdown() } }
    return tts
}
