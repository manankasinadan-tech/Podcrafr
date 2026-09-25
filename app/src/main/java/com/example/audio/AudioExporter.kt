package com.example.audio

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import androidx.core.content.FileProvider
import com.example.data.model.DialogueTurn
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

object AudioExporter {

    /**
     * Stitches turns into a master WAV file and provides a FileProvider share intent.
     */
    suspend fun createMasterWav(
        context: Context,
        turns: List<DialogueTurn>,
        podcastTitle: String
    ): Result<File> = withContext(Dispatchers.IO) {
        try {
            val audioFiles = turns.mapNotNull { turn ->
                turn.audioFilePath?.let { File(it) }?.takeIf { it.exists() }
            }

            if (audioFiles.isEmpty()) {
                return@withContext Result.failure(IllegalStateException("Aucun fichier audio n'a été généré pour ce script"))
            }

            val exportDir = File(context.filesDir, "exports").apply { mkdirs() }
            val cleanTitle = podcastTitle.replace(Regex("[^a-zA-Z0-9_-]"), "_").take(30).ifBlank { "podcast" }
            val outputFile = File(exportDir, "PodCraft_${cleanTitle}_${System.currentTimeMillis()}.wav")

            val success = WavHelper.stitchWavFiles(audioFiles, outputFile)
            if (success && outputFile.exists()) {
                Result.success(outputFile)
            } else {
                Result.failure(RuntimeException("Erreur lors de l'assemblage audio du podcast"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Shares the master WAV file using standard Android system sharing.
     */
    fun shareAudioFile(context: Context, audioFile: File, title: String): Intent {
        val uri: Uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.provider",
            audioFile
        )

        return Intent(Intent.ACTION_SEND).apply {
            type = "audio/wav"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "Épisode Podcast : $title")
            putExtra(Intent.EXTRA_TEXT, "Écoutez cet épisode de podcast créé avec PodCraft AI : $title")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }

    /**
     * Saves the WAV file directly to the device's public Music or Downloads directory.
     */
    suspend fun saveToMusicDirectory(context: Context, sourceFile: File, title: String): Result<Uri?> = withContext(Dispatchers.IO) {
        try {
            val cleanTitle = title.replace(Regex("[^a-zA-Z0-9_-]"), "_").take(30).ifBlank { "podcast" }
            val fileName = "PodCraft_${cleanTitle}_${System.currentTimeMillis()}.wav"

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val resolver = context.contentResolver
                val contentValues = ContentValues().apply {
                    put(MediaStore.Audio.Media.DISPLAY_NAME, fileName)
                    put(MediaStore.Audio.Media.MIME_TYPE, "audio/wav")
                    put(MediaStore.Audio.Media.RELATIVE_PATH, Environment.DIRECTORY_MUSIC + "/PodCraft")
                    put(MediaStore.Audio.Media.IS_PENDING, 1)
                }

                val uri = resolver.insert(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, contentValues)
                    ?: return@withContext Result.failure(IllegalStateException("Impossible de créer l'entrée MediaStore"))

                resolver.openOutputStream(uri)?.use { out ->
                    FileInputStream(sourceFile).use { input ->
                        input.copyTo(out)
                    }
                }

                contentValues.clear()
                contentValues.put(MediaStore.Audio.Media.IS_PENDING, 0)
                resolver.update(uri, contentValues, null, null)

                Result.success(uri)
            } else {
                val musicDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC)
                val appDir = File(musicDir, "PodCraft").apply { mkdirs() }
                val destFile = File(appDir, fileName)

                FileInputStream(sourceFile).use { input ->
                    FileOutputStream(destFile).use { output ->
                        input.copyTo(output)
                    }
                }
                Result.success(Uri.fromFile(destFile))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
