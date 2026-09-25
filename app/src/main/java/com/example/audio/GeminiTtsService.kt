package com.example.audio

import android.util.Base64
import android.util.Log
import com.example.data.model.PodcastVoice
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * Service to call Google AI Studio / Gemini TTS and Text generation APIs.
 */
class GeminiTtsService {

    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    /**
     * Synthesizes a dialogue turn into WAV audio bytes using Gemini TTS.
     */
    suspend fun synthesizeTurn(
        text: String,
        voice: PodcastVoice,
        apiKey: String
    ): Result<ByteArray> = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) {
            return@withContext Result.failure(IllegalStateException("Clé API Gemini non configurée"))
        }

        // Try primary TTS preview model first, then native audio preview
        val models = listOf(
            "gemini-2.5-flash-preview-tts",
            "gemini-2.5-flash-native-audio-preview-12-2025"
        )

        var lastError: Exception? = null

        for (model in models) {
            try {
                val url = "https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent?key=$apiKey"

                val promptWithDirection = "Speak naturally, authentically, and expressively in French as a podcast co-host named ${voice.displayName}. " +
                        "Adopt a ${voice.personalityFr} tone. Include natural pauses, laughter, and vocal inflections where indicated:\n$text"

                val requestJson = JSONObject().apply {
                    put("contents", JSONArray().apply {
                        put(JSONObject().apply {
                            put("parts", JSONArray().apply {
                                put(JSONObject().apply {
                                    put("text", promptWithDirection)
                                })
                            })
                        })
                    })
                    put("generationConfig", JSONObject().apply {
                        put("responseModalities", JSONArray().apply {
                            put("AUDIO")
                        })
                        put("speechConfig", JSONObject().apply {
                            put("voiceConfig", JSONObject().apply {
                                put("prebuiltVoiceConfig", JSONObject().apply {
                                    put("voiceName", voice.apiVoiceName)
                                })
                            })
                        })
                    })
                }

                val request = Request.Builder()
                    .url(url)
                    .post(requestJson.toString().toRequestBody(jsonMediaType))
                    .build()

                client.newCall(request).execute().use { response ->
                    val responseBody = response.body?.string() ?: ""
                    if (!response.isSuccessful) {
                        Log.w("GeminiTtsService", "Model $model returned error ${response.code}: $responseBody")
                        lastError = RuntimeException("Erreur API ($model ${response.code}): $responseBody")
                        return@use // continue to next model
                    }

                    val json = JSONObject(responseBody)
                    val candidates = json.optJSONArray("candidates")
                    if (candidates != null && candidates.length() > 0) {
                        val firstCandidate = candidates.getJSONObject(0)
                        val content = firstCandidate.optJSONObject("content")
                        val parts = content?.optJSONArray("parts")
                        if (parts != null && parts.length() > 0) {
                            for (i in 0 until parts.length()) {
                                val part = parts.getJSONObject(i)
                                val inlineData = part.optJSONObject("inlineData")
                                if (inlineData != null) {
                                    val mimeType = inlineData.optString("mimeType", "")
                                    val base64Data = inlineData.optString("data", "")
                                    if (base64Data.isNotBlank()) {
                                        val rawBytes = Base64.decode(base64Data, Base64.DEFAULT)

                                        // If PCM raw audio, wrap in WAV container
                                        val wavBytes = if (mimeType.contains("pcm", ignoreCase = true) || !isWavOrMp3(rawBytes)) {
                                            // Extract sample rate from mimeType if available (e.g. audio/pcm;rate=24000)
                                            val sampleRate = extractSampleRate(mimeType, 24000)
                                            WavHelper.pcmToWav(rawBytes, sampleRate)
                                        } else {
                                            rawBytes
                                        }
                                        return@withContext Result.success(wavBytes)
                                    }
                                }
                            }
                        }
                    }
                    lastError = RuntimeException("Aucun contenu audio reçu dans la réponse de $model")
                }
            } catch (e: Exception) {
                Log.e("GeminiTtsService", "Exception with model $model: ${e.message}")
                lastError = e
            }
        }

        Result.failure(lastError ?: RuntimeException("Échec de la génération audio"))
    }

    /**
     * Enhances a raw script with natural podcast reactions, conversational fillers,
     * and multi-speaker dialogue turns using gemini-3.5-flash.
     */
    suspend fun humanizeScript(
        rawText: String,
        apiKey: String
    ): Result<String> = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) {
            return@withContext Result.failure(IllegalStateException("Clé API Gemini non configurée"))
        }

        try {
            val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent?key=$apiKey"

            val prompt = """
                Tu es un producteur de podcast audio primé. Transforme le texte ou document suivant en un script de podcast extrêmement vivant, fluide et immersif entre 2 à 4 personnes choisies parmi :
                - Clara (Animatrice chaleureuse et souriante)
                - Alex (Co-animateur énergique, curieux et plein d'humour)
                - Marc (Expert réfléchi, voix grave et posée)
                - Sophie (Chroniqueuse vive, pétillante et expressive)
                - Thomas (Narrateur intense et charismatique)
                - Emma (Narratrice contemplative et douce)

                RÈGLES IMPORTANTES :
                1. Rends les dialogues ultra-naturels : petites hésitations, interjections spontanées ("Ah oui !", "Exactement...", "Mais attends, sérieux ?"), rires et relances vivantes.
                2. Ajoute des indications d'émotions et de réactions humaines entre crochets au début des répliques, par exemple :
                   Clara: [Souriante] Bienvenue dans cet épisode !
                   Alex: [Rires] Salut Clara ! Tu ne devineras jamais ce qui m'est arrivé.
                   Marc: [Posé] C'est pourtant une réalité incontestable...
                   Sophie: [Enthousiaste] Oh mais c'est génial ça !
                3. Ne mets AUCUN texte d'introduction ni de conclusion méta. Donne UNIQUEMENT le script prêt à lire ligne par ligne au format : 'Personnage: [Réaction] Texte'.

                Voici le contenu original :
                $rawText
            """.trimIndent()

            val requestJson = JSONObject().apply {
                put("contents", JSONArray().apply {
                    put(JSONObject().apply {
                        put("parts", JSONArray().apply {
                            put(JSONObject().apply {
                                put("text", prompt)
                            })
                        })
                    })
                })
            }

            val request = Request.Builder()
                .url(url)
                .post(requestJson.toString().toRequestBody(jsonMediaType))
                .build()

            client.newCall(request).execute().use { response ->
                val body = response.body?.string() ?: ""
                if (!response.isSuccessful) {
                    return@withContext Result.failure(RuntimeException("Erreur IA (${response.code}): $body"))
                }
                val json = JSONObject(body)
                val candidates = json.optJSONArray("candidates")
                val text = candidates?.optJSONObject(0)
                    ?.optJSONObject("content")
                    ?.optJSONArray("parts")
                    ?.optJSONObject(0)
                    ?.optString("text", "") ?: ""

                if (text.isNotBlank()) {
                    Result.success(text.trim())
                } else {
                    Result.failure(RuntimeException("Réponse vide de l'IA"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun isWavOrMp3(bytes: ByteArray): Boolean {
        if (bytes.size < 4) return false
        // WAV starts with "RIFF"
        if (bytes[0] == 'R'.code.toByte() && bytes[1] == 'I'.code.toByte() &&
            bytes[2] == 'F'.code.toByte() && bytes[3] == 'F'.code.toByte()) {
            return true
        }
        // MP3 ID3 or sync
        if (bytes[0] == 'I'.code.toByte() && bytes[1] == 'D'.code.toByte() && bytes[2] == '3'.code.toByte()) {
            return true
        }
        return false
    }

    private fun extractSampleRate(mimeType: String, defaultRate: Int): Int {
        val regex = Regex("rate=(\\d+)")
        val match = regex.find(mimeType)
        return match?.groupValues?.getOrNull(1)?.toIntOrNull() ?: defaultRate
    }
}
