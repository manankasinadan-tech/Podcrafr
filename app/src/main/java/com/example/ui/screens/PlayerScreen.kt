package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.DialogueTurn
import com.example.ui.PodcastViewModel
import com.example.ui.components.FlatCard
import com.example.ui.components.SpeakerAvatar
import com.example.ui.components.WaveVisualizer
import com.example.ui.theme.Slate200
import com.example.ui.theme.Slate400
import com.example.ui.theme.Slate500
import com.example.ui.theme.Slate700
import com.example.ui.theme.Slate800
import com.example.ui.theme.Slate900

@Composable
fun PlayerScreen(
    viewModel: PodcastViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val podcastTitle by viewModel.podcastTitle.collectAsState()
    val parsedTurns by viewModel.parsedTurns.collectAsState()
    val playerState by viewModel.playerState.collectAsState()
    val isGeneratingAudio by viewModel.isGeneratingAudio.collectAsState()
    val masterAudioFile by viewModel.masterAudioFile.collectAsState()

    val listState = rememberLazyListState()

    // Auto-scroll to active turn when playing
    LaunchedEffect(playerState.currentTurnIndex) {
        if (parsedTurns.isNotEmpty() && playerState.currentTurnIndex in parsedTurns.indices) {
            listState.animateScrollToItem(playerState.currentTurnIndex)
        }
    }

    val activeTurn = parsedTurns.getOrNull(playerState.currentTurnIndex)

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Spacer(modifier = Modifier.height(4.dp))

        // Episode Title Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = podcastTitle,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                    maxLines = 1
                )
                Text(
                    text = "${parsedTurns.size} répliques • ${formatDuration(playerState.totalDurationMs)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Slate500
                )
            }

            // Export button right in header
            FilledTonalButton(
                onClick = { viewModel.exportAudio() },
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.testTag("button_export_audio_header")
            ) {
                Icon(
                    imageVector = Icons.Default.Share,
                    contentDescription = "Exporter l'audio (.wav)",
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("Sortir l'audio", fontSize = 12.sp)
            }
        }

        // Active Speaker & Wave Visualizer Card
        FlatCard(
            modifier = Modifier.fillMaxWidth(),
            backgroundColor = activeTurn?.voice?.color?.copy(alpha = 0.08f) ?: MaterialTheme.colorScheme.surface,
            borderColor = activeTurn?.voice?.color?.copy(alpha = 0.4f) ?: Slate200
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        activeTurn?.let { turn ->
                            SpeakerAvatar(voice = turn.voice, size = 42.dp)
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = turn.speaker,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Voix : ${turn.voice.displayName} (${turn.voice.personalityFr})",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = turn.voice.color,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }

                    if (activeTurn?.reactionHints?.isNotBlank() == true) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(MaterialTheme.colorScheme.primaryContainer)
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = activeTurn.reactionHints,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // Audio wave animation
                WaveVisualizer(
                    isPlaying = playerState.isPlaying,
                    color = activeTurn?.voice?.color ?: MaterialTheme.colorScheme.primary
                )
            }
        }

        // Karaoke Synchronized Dialogue List
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .border(1.dp, Slate200, RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surface)
                .padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            itemsIndexed(parsedTurns) { index, turn ->
                val isActive = index == playerState.currentTurnIndex

                DialogueTurnItem(
                    turn = turn,
                    isActive = isActive,
                    isPlaying = isActive && playerState.isPlaying,
                    onClick = { viewModel.audioPlayer.playTurn(index) }
                )
            }
        }

        // Timeline Slider
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = formatDuration(playerState.currentPositionMs),
                    style = MaterialTheme.typography.bodySmall,
                    color = Slate500
                )
                Text(
                    text = formatDuration(playerState.totalDurationMs),
                    style = MaterialTheme.typography.bodySmall,
                    color = Slate500
                )
            }
            Slider(
                value = if (playerState.totalDurationMs > 0) {
                    (playerState.currentPositionMs.toFloat() / playerState.totalDurationMs).coerceIn(0f, 1f)
                } else 0f,
                onValueChange = {},
                enabled = false,
                colors = SliderDefaults.colors(
                    thumbColor = MaterialTheme.colorScheme.primary,
                    activeTrackColor = MaterialTheme.colorScheme.primary,
                    inactiveTrackColor = Slate200
                )
            )
        }

        // Player Controls Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Previous Turn
            IconButton(
                onClick = { viewModel.audioPlayer.previousTurn() },
                modifier = Modifier
                    .size(48.dp)
                    .testTag("button_prev_turn")
            ) {
                Icon(
                    imageVector = Icons.Default.SkipPrevious,
                    contentDescription = "Réplique précédente",
                    modifier = Modifier.size(28.dp),
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }

            // Big Play / Pause Flat Button
            Button(
                onClick = { viewModel.audioPlayer.togglePlayPause() },
                shape = CircleShape,
                modifier = Modifier
                    .size(64.dp)
                    .testTag("button_play_pause"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Icon(
                    imageVector = if (playerState.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (playerState.isPlaying) "Mettre en pause" else "Lire le podcast",
                    modifier = Modifier.size(32.dp),
                    tint = Color.White
                )
            }

            // Next Turn
            IconButton(
                onClick = { viewModel.audioPlayer.nextTurn() },
                modifier = Modifier
                    .size(48.dp)
                    .testTag("button_next_turn")
            ) {
                Icon(
                    imageVector = Icons.Default.SkipNext,
                    contentDescription = "Réplique suivante",
                    modifier = Modifier.size(28.dp),
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        // Speed Selector & Save to Device Actions
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Speed chips
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf(0.8f, 1.0f, 1.25f, 1.5f).forEach { speed ->
                    val isSelected = playerState.playbackSpeed == speed
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (isSelected) MaterialTheme.colorScheme.primary else Slate200)
                            .clickable { viewModel.audioPlayer.setSpeed(speed) }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "${speed}x",
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) Color.White else Slate700
                        )
                    }
                }
            }

            // Direct Save to Storage Button
            OutlinedButton(
                onClick = { viewModel.saveAudioToDevice() },
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.testTag("button_save_to_music")
            ) {
                Icon(
                    imageVector = Icons.Default.Download,
                    contentDescription = "Enregistrer dans l'appareil",
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("Enregistrer (.wav)", fontSize = 11.sp)
            }
        }
    }
}

@Composable
private fun DialogueTurnItem(
    turn: DialogueTurn,
    isActive: Boolean,
    isPlaying: Boolean,
    onClick: () -> Unit
) {
    val borderColor = if (isActive) turn.voice.color else Color.Transparent
    val bgColor = if (isActive) turn.voice.color.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surface

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .border(if (isActive) 1.5.dp else 0.dp, borderColor, RoundedCornerShape(8.dp))
            .background(bgColor)
            .clickable { onClick() }
            .padding(10.dp),
        verticalAlignment = Alignment.Top
    ) {
        SpeakerAvatar(voice = turn.voice, size = 32.dp)
        Spacer(modifier = Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = turn.speaker,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = if (isActive) turn.voice.color else Slate900
                )
                if (turn.reactionHints.isNotBlank()) {
                    Text(
                        text = turn.reactionHints,
                        style = MaterialTheme.typography.bodySmall,
                        color = Slate500,
                        fontSize = 11.sp
                    )
                }
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = turn.text,
                style = MaterialTheme.typography.bodyMedium,
                color = if (isActive) MaterialTheme.colorScheme.onSurface else Slate700,
                fontWeight = if (isActive) FontWeight.SemiBold else FontWeight.Normal
            )
        }
    }
}

private fun formatDuration(ms: Long): String {
    val totalSec = ms / 1000
    val min = totalSec / 60
    val sec = totalSec % 60
    return String.format("%02d:%02d", min, sec)
}
