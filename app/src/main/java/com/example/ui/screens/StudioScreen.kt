package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.PodcastTemplate
import com.example.data.model.PodcastTemplates
import com.example.data.model.PodcastVoice
import com.example.ui.PodcastViewModel
import com.example.ui.components.FlatCard
import com.example.ui.components.SpeakerAvatar
import com.example.ui.theme.Emerald500
import com.example.ui.theme.Indigo50
import com.example.ui.theme.Indigo600
import com.example.ui.theme.Slate200
import com.example.ui.theme.Slate500
import com.example.ui.theme.Slate700
import com.example.ui.theme.Slate800

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun StudioScreen(
    viewModel: PodcastViewModel,
    onNavigateToPlayer: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scriptText by viewModel.scriptText.collectAsState()
    val podcastTitle by viewModel.podcastTitle.collectAsState()
    val parsedTurns by viewModel.parsedTurns.collectAsState()
    val speakerVoiceMap by viewModel.speakerVoiceMap.collectAsState()
    val isGeneratingAudio by viewModel.isGeneratingAudio.collectAsState()
    val generationProgress by viewModel.generationProgress.collectAsState()
    val generationStatus by viewModel.generationStatus.collectAsState()
    val isHumanizing by viewModel.isHumanizing.collectAsState()

    // File picker for PDF and Text files (.pdf, .txt, .md)
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { viewModel.importFileFromUri(it) }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(4.dp))
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Studio Podcast",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "Transformez texte ou PDF en podcast réaliste avec 6 voix",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Slate500
                    )
                }
            }
        }

        // Title Input
        item {
            FlatCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "Titre du podcast",
                        style = MaterialTheme.typography.labelMedium,
                        color = Slate500,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = podcastTitle,
                        onValueChange = { viewModel.updatePodcastTitle(it) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_podcast_title"),
                        singleLine = true,
                        placeholder = { Text("Ex: L'Onde Tech - Épisode 1") },
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                            focusedBorderColor = MaterialTheme.colorScheme.primary
                        )
                    )
                }
            }
        }

        // Templates & File Import row
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Modèles prêts à lire",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )

                    // Import PDF / Text button
                    FilledTonalButton(
                        onClick = { filePickerLauncher.launch("*/*") },
                        modifier = Modifier.testTag("button_import_file"),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.UploadFile,
                            contentDescription = "Importer un fichier PDF ou texte",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Importer PDF / Texte", fontSize = 12.sp)
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    PodcastTemplates.list.forEach { template ->
                        TemplateChip(
                            template = template,
                            isSelected = podcastTitle == template.title,
                            onSelect = { viewModel.applyTemplate(template) }
                        )
                    }
                }
            }
        }

        // Script Editor Box
        item {
            FlatCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Script de dialogue (${parsedTurns.size} répliques)",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )

                        // Humanize with Gemini button
                        OutlinedButton(
                            onClick = { viewModel.humanizeScript() },
                            enabled = !isHumanizing && scriptText.isNotBlank(),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.testTag("button_humanize_ai")
                        ) {
                            if (isHumanizing) {
                                CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Humanisation...", fontSize = 12.sp)
                            } else {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = Emerald500
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Humaniser avec l'IA", fontSize = 12.sp)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Format : Nom: [Réaction] Réplique (ex: Clara: [Rires] Bienvenue !)",
                        style = MaterialTheme.typography.bodySmall,
                        color = Slate500
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = scriptText,
                        onValueChange = { viewModel.updateScriptText(it) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(240.dp)
                            .testTag("input_script_text"),
                        placeholder = { Text("Tapez ou collez votre script de dialogue ici...") },
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                            focusedBorderColor = MaterialTheme.colorScheme.primary
                        )
                    )
                }
            }
        }

        // Speakers & 6 Voices Assignment Card
        item {
            FlatCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Intervenants & Voix (${speakerVoiceMap.size})",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "6 voix disponibles",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    if (speakerVoiceMap.isEmpty()) {
                        Text(
                            text = "Aucun intervenant détecté. Commencez les lignes par un nom (ex: Clara: ...)",
                            style = MaterialTheme.typography.bodySmall,
                            color = Slate500
                        )
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            speakerVoiceMap.forEach { (speaker, voice) ->
                                SpeakerVoiceRow(
                                    speaker = speaker,
                                    selectedVoice = voice,
                                    onVoiceChange = { newVoice -> viewModel.setSpeakerVoice(speaker, newVoice) },
                                    onPreviewVoice = { viewModel.previewVoice(voice) }
                                )
                            }
                        }
                    }
                }
            }
        }

        // Generation Progress Overlay or Action Button
        item {
            if (isGeneratingAudio) {
                FlatCard(
                    modifier = Modifier.fillMaxWidth(),
                    backgroundColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f),
                    borderColor = MaterialTheme.colorScheme.primary
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Génération du podcast en cours...",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            IconButton(onClick = { viewModel.cancelGeneration() }) {
                                Icon(Icons.Default.Close, contentDescription = "Annuler")
                            }
                        }

                        LinearProgressIndicator(
                            progress = { generationProgress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp))
                        )

                        Text(
                            text = generationStatus,
                            style = MaterialTheme.typography.bodySmall,
                            color = Slate700
                        )
                    }
                }
            } else {
                Button(
                    onClick = {
                        viewModel.generatePodcastAudio()
                        onNavigateToPlayer()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("button_generate_podcast"),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.GraphicEq,
                        contentDescription = null,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Générer et écouter le podcast",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun TemplateChip(
    template: PodcastTemplate,
    isSelected: Boolean,
    onSelect: () -> Unit
) {
    val borderColor = if (isSelected) MaterialTheme.colorScheme.primary else Slate200
    val bgColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .border(1.dp, borderColor, RoundedCornerShape(8.dp))
            .background(bgColor)
            .clickable { onSelect() }
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Column {
            Text(
                text = template.title,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = if (isSelected) MaterialTheme.colorScheme.primary else Slate800
            )
            Text(
                text = template.category,
                style = MaterialTheme.typography.bodySmall,
                color = Slate500,
                fontSize = 11.sp
            )
        }
    }
}

@Composable
private fun SpeakerVoiceRow(
    speaker: String,
    selectedVoice: PodcastVoice,
    onVoiceChange: (PodcastVoice) -> Unit,
    onPreviewVoice: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .border(1.dp, Slate200, RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            SpeakerAvatar(voice = selectedVoice, size = 36.dp)
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    text = speaker,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "${selectedVoice.displayName} (${selectedVoice.roleFr})",
                    style = MaterialTheme.typography.bodySmall,
                    color = selectedVoice.color,
                    fontSize = 12.sp
                )
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            // Preview voice sample
            IconButton(
                onClick = onPreviewVoice,
                modifier = Modifier
                    .size(36.dp)
                    .testTag("preview_btn_$speaker")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                    contentDescription = "Écouter l'extrait vocal",
                    tint = selectedVoice.color,
                    modifier = Modifier.size(18.dp)
                )
            }

            // Voice selector dropdown
            Box {
                OutlinedButton(
                    onClick = { expanded = true },
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.testTag("select_voice_btn_$speaker")
                ) {
                    Text(selectedVoice.displayName, fontSize = 12.sp)
                }

                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    PodcastVoice.entries.forEach { voice ->
                        DropdownMenuItem(
                            text = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(voice.color)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text(
                                            text = "${voice.displayName} - ${voice.genderFr}",
                                            fontWeight = if (voice == selectedVoice) FontWeight.Bold else FontWeight.Normal
                                        )
                                        Text(
                                            text = voice.roleFr,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = Slate500,
                                            fontSize = 11.sp
                                        )
                                    }
                                }
                            },
                            onClick = {
                                onVoiceChange(voice)
                                expanded = false
                            }
                        )
                    }
                }
            }
        }
    }
}
