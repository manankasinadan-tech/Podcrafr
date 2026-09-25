package com.example.audio

import android.content.Context
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import com.example.data.model.PodcastVoice
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Locale

/**
 * High-quality offline fallback using Android's native TextToSpeech engine.
 * Tailors pitch, speech rate, and voice characteristics for each of the 6 personalities.
 */
class AndroidTtsFallback(private val context: Context) : TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = null
    private var isInitialized = false
    private val initDeferred = CompletableDeferred<Boolean>()

    init {
        tts = TextToSpeech(context.applicationContext, this)
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = tts?.setLanguage(Locale.FRENCH)
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                tts?.setLanguage(Locale.getDefault())
            }
            isInitialized = true
            initDeferred.complete(true)
        } else {
            isInitialized = false
            initDeferred.complete(false)
        }
    }

    private suspend fun ensureReady(): Boolean {
        if (isInitialized) return true
        return try {
            initDeferred.await()
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Synthesizes text to a WAV audio file on disk using Android TTS.
     */
    suspend fun synthesizeToFile(
        text: String,
        voice: PodcastVoice,
        outputFile: File
    ): Boolean = withContext(Dispatchers.IO) {
        if (!ensureReady() || tts == null) return@withContext false

        val cleanText = cleanTextForTts(text)
        val utteranceId = "tts_${System.currentTimeMillis()}_${voice.id}"
        val deferred = CompletableDeferred<Boolean>()

        tts?.apply {
            setPitch(voice.pitchTts)
            setSpeechRate(voice.speedTts)
            setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(id: String?) {}
                override fun onDone(id: String?) {
                    if (id == utteranceId) deferred.complete(true)
                }
                override fun onError(id: String?) {
                    if (id == utteranceId) deferred.complete(false)
                }
            })
        }

        outputFile.parentFile?.mkdirs()
        val params = Bundle().apply {
            putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, utteranceId)
        }

        val result = tts?.synthesizeToFile(cleanText, params, outputFile, utteranceId)
        if (result != TextToSpeech.SUCCESS) {
            return@withContext false
        }

        try {
            deferred.await()
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Plays a voice preview sample directly through the speaker.
     */
    suspend fun speakSample(text: String, voice: PodcastVoice) {
        if (!ensureReady() || tts == null) return
        val cleanText = cleanTextForTts(text)
        tts?.apply {
            stop()
            setPitch(voice.pitchTts)
            setSpeechRate(voice.speedTts)
            speak(cleanText, TextToSpeech.QUEUE_FLUSH, null, "sample_${voice.id}")
        }
    }

    fun stop() {
        tts?.stop()
    }

    fun shutdown() {
        tts?.stop()
        tts?.shutdown()
        tts = null
    }

    private fun cleanTextForTts(input: String): String {
        // Strip stage directions like [Rires] or (enthousiaste) for raw TTS
        return input.replace(Regex("\\[.*?\\]"), "")
            .replace(Regex("\\(.*?\\)"), "")
            .trim()
    }
}
