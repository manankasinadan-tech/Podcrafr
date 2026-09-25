package com.example.ui

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.BuildConfig
import com.example.audio.AndroidTtsFallback
import com.example.audio.AudioExporter
import com.example.audio.GeminiTtsService
import com.example.audio.PlayerState
import com.example.audio.PodcastAudioPlayer
import com.example.audio.WavHelper
import com.example.data.local.PodCraftDatabase
import com.example.data.local.PodcastEntity
import com.example.data.model.DialogueTurn
import com.example.data.model.PodcastTemplate
import com.example.data.model.PodcastTemplates
import com.example.data.model.PodcastVoice
import com.example.parser.PdfTextExtractor
import com.example.parser.ScriptParser
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

sealed interface StudioUiEvent {
    data class ShowSnackbar(val message: String) : StudioUiEvent
    data class AudioExported(val file: File, val title: String) : StudioUiEvent
}

class PodcastViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs: SharedPreferences = application.getSharedPreferences("podcraft_prefs", Context.MODE_PRIVATE)
    private val database = PodCraftDatabase.getInstance(application)
    private val podcastDao = database.podcastDao()

    private val geminiService = GeminiTtsService()
    private val androidTts = AndroidTtsFallback(application)
    val audioPlayer = PodcastAudioPlayer(application)

    // Flow of saved podcasts from Room
    val savedPodcasts = podcastDao.getAllPodcasts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Player state from audio player
    val playerState: StateFlow<PlayerState> = audioPlayer.playerState

    // Script & Studio state
    private val _scriptText = MutableStateFlow(PodcastTemplates.list.first().script)
    val scriptText: StateFlow<String> = _scriptText.asStateFlow()

    private val _podcastTitle = MutableStateFlow(PodcastTemplates.list.first().title)
    val podcastTitle: StateFlow<String> = _podcastTitle.asStateFlow()

    private val _parsedTurns = MutableStateFlow<List<DialogueTurn>>(emptyList())
    val parsedTurns: StateFlow<List<DialogueTurn>> = _parsedTurns.asStateFlow()

    private val _speakerVoiceMap = MutableStateFlow<Map<String, PodcastVoice>>(emptyMap())
    val speakerVoiceMap: StateFlow<Map<String, PodcastVoice>> = _speakerVoiceMap.asStateFlow()

    // Generation state
    private val _isGeneratingAudio = MutableStateFlow(false)
    val isGeneratingAudio: StateFlow<Boolean> = _isGeneratingAudio.asStateFlow()

    private val _generationProgress = MutableStateFlow(0f)
    val generationProgress: StateFlow<Float> = _generationProgress.asStateFlow()

    private val _generationStatus = MutableStateFlow("")
    val generationStatus: StateFlow<String> = _generationStatus.asStateFlow()

    private val _isHumanizing = MutableStateFlow(false)
    val isHumanizing: StateFlow<Boolean> = _isHumanizing.asStateFlow()

    // Master audio file for current project
    private val _masterAudioFile = MutableStateFlow<File?>(null)
    val masterAudioFile: StateFlow<File?> = _masterAudioFile.asStateFlow()

    // API Key: default from BuildConfig, or custom from prefs
    private val _customApiKey = MutableStateFlow(
        prefs.getString("custom_gemini_api_key", "") ?: ""
    )
    val customApiKey: StateFlow<String> = _customApiKey.asStateFlow()

    private val _events = MutableSharedFlow<StudioUiEvent>()
    val events: SharedFlow<StudioUiEvent> = _events.asSharedFlow()

    private var generationJob: Job? = null

    init {
        // Initial parse of default template script
        updateScriptText(_scriptText.value)
    }

    fun getEffectiveApiKey(): String {
        val custom = _customApiKey.value.trim()
        if (custom.isNotEmpty()) return custom
        return try {
            BuildConfig.GEMINI_API_KEY
        } catch (e: Exception) {
            ""
        }
    }

    fun setCustomApiKey(key: String) {
        _customApiKey.value = key
        prefs.edit().putString("custom_gemini_api_key", key).apply()
    }

    fun updateScriptText(newText: String) {
        _scriptText.value = newText
        val (turns, voiceMap) = ScriptParser.parseScript(newText, _speakerVoiceMap.value)
        _parsedTurns.value = turns
        _speakerVoiceMap.value = voiceMap
    }

    fun updatePodcastTitle(newTitle: String) {
        _podcastTitle.value = newTitle
    }

    fun applyTemplate(template: PodcastTemplate) {
        _podcastTitle.value = template.title
        updateScriptText(template.script)
    }

    fun setSpeakerVoice(speaker: String, voice: PodcastVoice) {
        val updatedMap = _speakerVoiceMap.value.toMutableMap()
        updatedMap[speaker] = voice
        _speakerVoiceMap.value = updatedMap

        // Re-assign voices in turns
        _parsedTurns.value = _parsedTurns.value.map { turn ->
            if (turn.speaker == speaker) {
                turn.copy(voice = voice)
            } else {
                turn
            }
        }
    }

    /**
     * Import script from a selected text file or PDF document
     */
    fun importFileFromUri(uri: Uri) {
        viewModelScope.launch {
            _generationStatus.value = "Lecture du fichier importé..."
            val result = PdfTextExtractor.extractTextFromUri(getApplication(), uri)
            result.onSuccess { text ->
                if (text.isNotBlank()) {
                    updateScriptText(text)
                    _events.emit(StudioUiEvent.ShowSnackbar("Fichier importé avec succès !"))
                } else {
                    _events.emit(StudioUiEvent.ShowSnackbar("Le fichier importé est vide"))
                }
            }.onFailure { err ->
                _events.emit(StudioUiEvent.ShowSnackbar("Erreur lors de l'import : ${err.message}"))
            }
            _generationStatus.value = ""
        }
    }

    /**
     * Enhances the script using Gemini AI with natural podcast banter and sound reactions.
     */
    fun humanizeScript() {
        val apiKey = getEffectiveApiKey()
        if (apiKey.isBlank()) {
            viewModelScope.launch {
                _events.emit(StudioUiEvent.ShowSnackbar("Veuillez renseigner votre clé API Gemini dans les Paramètres"))
            }
            return
        }

        viewModelScope.launch {
            _isHumanizing.value = true
            val result = geminiService.humanizeScript(_scriptText.value, apiKey)
            result.onSuccess { humanizedText ->
                updateScriptText(humanizedText)
                _events.emit(StudioUiEvent.ShowSnackbar("Script transformé en podcast vivant !"))
            }.onFailure { err ->
                _events.emit(StudioUiEvent.ShowSnackbar("Erreur IA : ${err.message}"))
            }
            _isHumanizing.value = false
        }
    }

    /**
     * Generates audio for all turns using Google Gemini TTS (or Android TTS fallback if offline/no key)
     */
    fun generatePodcastAudio() {
        val turns = _parsedTurns.value
        if (turns.isEmpty()) {
            viewModelScope.launch {
                _events.emit(StudioUiEvent.ShowSnackbar("Aucune réplique trouvée dans le script"))
            }
            return
        }

        val apiKey = getEffectiveApiKey()
        val useGeminiTts = apiKey.isNotBlank()

        generationJob?.cancel()
        generationJob = viewModelScope.launch {
            _isGeneratingAudio.value = true
            _generationProgress.value = 0f

            val audioDir = File(getApplication<Application>().filesDir, "audio_turns_${System.currentTimeMillis()}").apply { mkdirs() }
            val updatedTurns = turns.toMutableList()

            var successCount = 0

            for (i in updatedTurns.indices) {
                val turn = updatedTurns[i]
                _generationStatus.value = "Génération de la réplique ${i + 1}/${updatedTurns.size} (${turn.speaker} - ${turn.voice.displayName})..."
                _generationProgress.value = (i.toFloat() / updatedTurns.size)

                val outputFile = File(audioDir, "turn_${i}_${turn.voice.id}.wav")

                var turnGenerated = false

                if (useGeminiTts) {
                    val result = geminiService.synthesizeTurn(turn.getPromptText(), turn.voice, apiKey)
                    result.onSuccess { wavBytes ->
                        withContext(Dispatchers.IO) {
                            outputFile.writeBytes(wavBytes)
                        }
                        turnGenerated = true
                    }.onFailure { err ->
                        Log.w("PodcastViewModel", "Gemini TTS failed for turn $i, falling back to Android TTS: ${err.message}")
                        // Fallback to Android TTS
                        turnGenerated = androidTts.synthesizeToFile(turn.text, turn.voice, outputFile)
                    }
                } else {
                    turnGenerated = androidTts.synthesizeToFile(turn.text, turn.voice, outputFile)
                }

                if (turnGenerated && outputFile.exists()) {
                    val duration = WavHelper.getWavDurationMs(outputFile)
                    updatedTurns[i] = turn.copy(
                        audioFilePath = outputFile.absolutePath,
                        durationMs = duration,
                        isGenerated = true
                    )
                    successCount++
                }
            }

            _parsedTurns.value = updatedTurns
            _generationProgress.value = 1.0f
            _generationStatus.value = "Assemblage du podcast audio final..."

            // Stitch all turns into a master WAV file
            val masterResult = AudioExporter.createMasterWav(getApplication(), updatedTurns, _podcastTitle.value)
            masterResult.onSuccess { masterFile ->
                _masterAudioFile.value = masterFile
                audioPlayer.loadEpisode(updatedTurns, masterFile)

                // Save to Room DB
                val totalDurationSec = (updatedTurns.sumOf { it.durationMs } / 1000).toInt()
                podcastDao.insertPodcast(
                    PodcastEntity(
                        title = _podcastTitle.value,
                        scriptText = _scriptText.value,
                        totalTurns = updatedTurns.size,
                        durationSec = totalDurationSec,
                        fullAudioPath = masterFile.absolutePath,
                        speakerCount = _speakerVoiceMap.value.size
                    )
                )

                _events.emit(StudioUiEvent.ShowSnackbar("Podcast généré avec succès ($successCount répliques) !"))
            }.onFailure {
                audioPlayer.loadEpisode(updatedTurns, null)
                _events.emit(StudioUiEvent.ShowSnackbar("Répliques audio prêtes à être écoutées"))
            }

            _isGeneratingAudio.value = false
            _generationStatus.value = ""
        }
    }

    fun cancelGeneration() {
        generationJob?.cancel()
        _isGeneratingAudio.value = false
        _generationStatus.value = ""
    }

    /**
     * Preview voice sample using Android TTS or Gemini TTS
     */
    fun previewVoice(voice: PodcastVoice) {
        viewModelScope.launch {
            val apiKey = getEffectiveApiKey()
            if (apiKey.isNotBlank()) {
                val result = geminiService.synthesizeTurn(voice.sampleTextFr, voice, apiKey)
                result.onSuccess { wavBytes ->
                    val sampleFile = File(getApplication<Application>().cacheDir, "sample_${voice.id}.wav")
                    withContext(Dispatchers.IO) {
                        sampleFile.writeBytes(wavBytes)
                    }
                    audioPlayer.loadEpisode(
                        listOf(DialogueTurn(0, voice.displayName, voice.sampleTextFr, "", voice, sampleFile.absolutePath, 3000L, true)),
                        sampleFile
                    )
                    audioPlayer.play()
                    return@launch
                }
            }
            // Fallback preview
            androidTts.speakSample(voice.sampleTextFr, voice)
        }
    }

    /**
     * Export master podcast audio (.wav) for sharing or saving
     */
    fun exportAudio() {
        viewModelScope.launch {
            val masterFile = _masterAudioFile.value
            if (masterFile != null && masterFile.exists()) {
                _events.emit(StudioUiEvent.AudioExported(masterFile, _podcastTitle.value))
            } else {
                val turns = _parsedTurns.value.filter { it.audioFilePath != null }
                if (turns.isEmpty()) {
                    _events.emit(StudioUiEvent.ShowSnackbar("Générez d'abord le podcast avant de l'exporter"))
                    return@launch
                }
                _generationStatus.value = "Création du fichier audio .wav..."
                val result = AudioExporter.createMasterWav(getApplication(), turns, _podcastTitle.value)
                result.onSuccess { file ->
                    _masterAudioFile.value = file
                    _events.emit(StudioUiEvent.AudioExported(file, _podcastTitle.value))
                }.onFailure { err ->
                    _events.emit(StudioUiEvent.ShowSnackbar("Erreur lors de l'export audio : ${err.message}"))
                }
                _generationStatus.value = ""
            }
        }
    }

    /**
     * Save master WAV to public Music folder
     */
    fun saveAudioToDevice() {
        viewModelScope.launch {
            val masterFile = _masterAudioFile.value ?: run {
                val turns = _parsedTurns.value.filter { it.audioFilePath != null }
                val result = AudioExporter.createMasterWav(getApplication(), turns, _podcastTitle.value)
                result.getOrNull()
            }

            if (masterFile == null || !masterFile.exists()) {
                _events.emit(StudioUiEvent.ShowSnackbar("Aucun fichier audio disponible à sauvegarder"))
                return@launch
            }

            val result = AudioExporter.saveToMusicDirectory(getApplication(), masterFile, _podcastTitle.value)
            result.onSuccess {
                _events.emit(StudioUiEvent.ShowSnackbar("Fichier WAV enregistré dans Musique / PodCraft !"))
            }.onFailure { err ->
                _events.emit(StudioUiEvent.ShowSnackbar("Échec de l'enregistrement : ${err.message}"))
            }
        }
    }

    /**
     * Load an existing saved podcast from the library
     */
    fun loadSavedPodcast(podcast: PodcastEntity) {
        _podcastTitle.value = podcast.title
        updateScriptText(podcast.scriptText)

        val audioPath = podcast.fullAudioPath
        if (audioPath != null) {
            val file = File(audioPath)
            if (file.exists()) {
                _masterAudioFile.value = file
                audioPlayer.loadEpisode(_parsedTurns.value, file)
            }
        }
    }

    fun deletePodcast(podcast: PodcastEntity) {
        viewModelScope.launch {
            podcastDao.deletePodcast(podcast)
            podcast.fullAudioPath?.let { path ->
                try { File(path).delete() } catch (e: Exception) {}
            }
            _events.emit(StudioUiEvent.ShowSnackbar("Épisode supprimé"))
        }
    }

    override fun onCleared() {
        super.onCleared()
        audioPlayer.release()
        androidTts.shutdown()
    }
}
