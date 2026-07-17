package com.example.teluguphotoquote

import android.app.Activity
import android.content.Intent
import android.speech.RecognizerIntent
import java.util.Locale

enum class SpeechLanguage(val bcp47Tag: String, val label: String) {
    TELUGU("te-IN", "తెలుగు"),
    ENGLISH("en-IN", "English")
}

/**
 * Builds the intent for Android's built-in speech recognizer UI.
 * A language toggle (Telugu / English) should be shown to the user before
 * calling this, since SpeechRecognizer performs best with a single fixed
 * target language rather than trying to auto-detect code-mixed speech.
 */
object SpeechInputHelper {

    fun createRecognizerIntent(language: SpeechLanguage): Intent {
        return Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
            )
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, language.bcp47Tag)
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak now (${language.label})")
        }
    }

    /** Call before launching, to show a friendly message if no voice engine is present. */
    fun isAvailable(activity: Activity): Boolean {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
        return intent.resolveActivity(activity.packageManager) != null
    }

    fun extractResultText(data: Intent?): String? {
        val results = data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
        return results?.firstOrNull()
    }
}
