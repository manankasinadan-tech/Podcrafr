package com.example.audio

import android.content.Context
import android.media.MediaPlayer
import android.media.PlaybackParams
import android.os.Build
import android.util.Log
import com.example.data.model.DialogueTurn
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File

data class PlayerState(
    val isPlaying: Boolean = false,
    val currentTurnIndex: Int = 0,
    val currentPositionMs: Long = 0L,
    val totalDurationMs: Long = 0L,
    val playbackSpeed: Float = 1.0f,
    val activeSpeaker: String = "",
    val activeLineText: String = "",
    val isMasterAudioLoaded: Boolean = false
)

class PodcastAudioPlayer(private val context: Context) {

    private var mediaPlayer: MediaPlayer? = null
    private var turns: List<DialogueTurn> = emptyList()
    private var masterAudioFile: File? = null

    private val _playerState = MutableStateFlow(PlayerState())
    val playerState: StateFlow<PlayerState> = _playerState.asStateFlow()

    private val playerScope = CoroutineScope(Dispatchers.Main)
    private var tickerJob: Job? = null

    fun loadEpisode(turnsList: List<DialogueTurn>, masterFile: File?) {
        stop()
        turns = turnsList
        masterAudioFile = masterFile

        val totalMs = turnsList.sumOf { it.durationMs }.coerceAtLeast(
            if (masterFile != null && masterFile.exists()) WavHelper.getWavDurationMs(masterFile) else 0L
        )

        _playerState.value = PlayerState(
            isPlaying = false,
            currentTurnIndex = 0,
            currentPositionMs = 0L,
            totalDurationMs = totalMs,
            playbackSpeed = _playerState.value.playbackSpeed,
            activeSpeaker = turnsList.firstOrNull()?.speaker ?: "",
            activeLineText = turnsList.firstOrNull()?.text ?: "",
            isMasterAudioLoaded = masterFile != null && masterFile.exists()
        )
    }

    fun play() {
        if (turns.isEmpty()) return

        if (mediaPlayer == null) {
            playTurn(_playerState.value.currentTurnIndex)
        } else {
            try {
                mediaPlayer?.start()
                _playerState.value = _playerState.value.copy(isPlaying = true)
                startTicker()
            } catch (e: Exception) {
                playTurn(_playerState.value.currentTurnIndex)
            }
        }
    }

    fun pause() {
        try {
            mediaPlayer?.pause()
        } catch (e: Exception) {
            // ignore
        }
        _playerState.value = _playerState.value.copy(isPlaying = false)
        tickerJob?.cancel()
    }

    fun togglePlayPause() {
        if (_playerState.value.isPlaying) {
            pause()
        } else {
            play()
        }
    }

    fun playTurn(index: Int) {
        if (turns.isEmpty()) return
        val clampedIndex = index.coerceIn(0, turns.size - 1)
        val turn = turns[clampedIndex]

        stopMediaPlayerOnly()

        val audioPath = turn.audioFilePath
        if (audioPath == null || !File(audioPath).exists()) {
            Log.w("PodcastAudioPlayer", "Audio file for turn $index does not exist")
            _playerState.value = _playerState.value.copy(
                currentTurnIndex = clampedIndex,
                activeSpeaker = turn.speaker,
                activeLineText = turn.text
            )
            return
        }

        try {
            mediaPlayer = MediaPlayer().apply {
                setDataSource(audioPath)
                prepare()
                setPlaybackSpeedInternal(_playerState.value.playbackSpeed)
                setOnCompletionListener {
                    // Advance to next turn automatically
                    if (clampedIndex < turns.size - 1) {
                        playTurn(clampedIndex + 1)
                    } else {
                        // Podcast finished
                        _playerState.value = _playerState.value.copy(
                            isPlaying = false,
                            currentTurnIndex = 0,
                            currentPositionMs = 0
                        )
                        tickerJob?.cancel()
                    }
                }
                start()
            }

            _playerState.value = _playerState.value.copy(
                isPlaying = true,
                currentTurnIndex = clampedIndex,
                activeSpeaker = turn.speaker,
                activeLineText = turn.text
            )

            startTicker()
        } catch (e: Exception) {
            Log.e("PodcastAudioPlayer", "Error playing turn $index: ${e.message}")
            _playerState.value = _playerState.value.copy(isPlaying = false)
        }
    }

    fun nextTurn() {
        if (turns.isNotEmpty() && _playerState.value.currentTurnIndex < turns.size - 1) {
            playTurn(_playerState.value.currentTurnIndex + 1)
        }
    }

    fun previousTurn() {
        if (turns.isNotEmpty() && _playerState.value.currentTurnIndex > 0) {
            playTurn(_playerState.value.currentTurnIndex - 1)
        }
    }

    fun setSpeed(speed: Float) {
        _playerState.value = _playerState.value.copy(playbackSpeed = speed)
        setPlaybackSpeedInternal(speed)
    }

    private fun setPlaybackSpeedInternal(speed: Float) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            try {
                mediaPlayer?.let { player ->
                    if (player.isPlaying) {
                        val params = player.playbackParams ?: PlaybackParams()
                        params.speed = speed
                        player.playbackParams = params
                    }
                }
            } catch (e: Exception) {
                Log.w("PodcastAudioPlayer", "Failed to set playback speed: ${e.message}")
            }
        }
    }

    private fun startTicker() {
        tickerJob?.cancel()
        tickerJob = playerScope.launch {
            while (isActive && _playerState.value.isPlaying) {
                try {
                    val turnIndex = _playerState.value.currentTurnIndex
                    val priorDuration = turns.take(turnIndex).sumOf { it.durationMs }
                    val currentTurnMs = mediaPlayer?.currentPosition?.toLong() ?: 0L
                    val totalElapsed = priorDuration + currentTurnMs

                    _playerState.value = _playerState.value.copy(
                        currentPositionMs = totalElapsed
                    )
                } catch (e: Exception) {
                    // ignore
                }
                delay(200)
            }
        }
    }

    private fun stopMediaPlayerOnly() {
        tickerJob?.cancel()
        try {
            mediaPlayer?.stop()
            mediaPlayer?.release()
        } catch (e: Exception) {
            // ignore
        }
        mediaPlayer = null
    }

    fun stop() {
        stopMediaPlayerOnly()
        _playerState.value = _playerState.value.copy(isPlaying = false, currentPositionMs = 0L)
    }

    fun release() {
        stop()
    }
}
