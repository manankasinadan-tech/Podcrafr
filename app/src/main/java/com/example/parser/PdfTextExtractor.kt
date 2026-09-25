package com.example.parser

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.util.zip.Inflater
import java.util.zip.InflaterInputStream

/**
 * Utility to extract text content from either plain text files (.txt, .md)
 * or PDF documents (.pdf) without requiring external heavy native dependencies.
 */
object PdfTextExtractor {

    suspend fun extractTextFromUri(context: Context, uri: Uri): Result<String> = withContext(Dispatchers.IO) {
        try {
            val contentResolver = context.contentResolver
            val mimeType = contentResolver.getType(uri) ?: ""
            val fileName = getFileName(context, uri).lowercase()

            val isPdf = mimeType.contains("pdf", ignoreCase = true) || fileName.endsWith(".pdf")

            contentResolver.openInputStream(uri)?.use { inputStream ->
                if (isPdf) {
                    val pdfText = extractTextFromPdfStream(inputStream)
                    if (pdfText.isNotBlank()) {
                        Result.success(pdfText)
                    } else {
                        Result.success("Document PDF importé ($fileName).\nLe texte extrait est vide ou scanné. Utilisez le bouton 'Humaniser avec l'IA' pour générer automatiquement le script de podcast !")
                    }
                } else {
                    // Plain text stream
                    val text = inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
                    Result.success(text)
                }
            } ?: Result.failure(IllegalStateException("Impossible d'ouvrir le fichier sélectionné"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Lightweight pure-Kotlin PDF stream text parser that inspects PDF objects,
     * inflates FlateDecode streams, and extracts character sequences enclosed in (text) Tj or [(t) e (xt)] TJ.
     */
    private fun extractTextFromPdfStream(inputStream: InputStream): String {
        val bytes = inputStream.readBytes()
        val textBuilder = StringBuilder()

        try {
            val content = String(bytes, Charsets.ISO_8859_1)

            // Find all streams
            val streamRegex = Regex("stream[\\r\\n]+(.*?)endstream", setOf(RegexOption.DOT_MATCHES_ALL))
            val matches = streamRegex.findAll(content)

            for (match in matches) {
                val streamStartIndex = match.groups[1]?.range?.first ?: continue
                val streamEndIndex = match.groups[1]?.range?.last ?: continue

                // Check preceding dictionary for /FlateDecode
                val precedingText = content.substring(maxOf(0, streamStartIndex - 200), streamStartIndex)
                val isFlate = precedingText.contains("/FlateDecode")

                val streamBytes = bytes.sliceArray(streamStartIndex..streamEndIndex)

                val decodedBytes = if (isFlate) {
                    try {
                        inflateBytes(streamBytes)
                    } catch (e: Exception) {
                        null
                    }
                } else {
                    streamBytes
                }

                if (decodedBytes != null) {
                    val streamStr = String(decodedBytes, Charsets.UTF_8)
                    extractTextOperators(streamStr, textBuilder)
                }
            }

            // Fallback: search for simple literal strings if streams were not decompressed
            if (textBuilder.length < 20) {
                val literalRegex = Regex("\\(([^()]{3,})\\)\\s*Tj")
                for (match in literalRegex.findAll(content)) {
                    val str = match.groupValues[1].trim()
                    if (str.isNotBlank()) {
                        textBuilder.append(str).append(" ")
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return textBuilder.toString().trim()
    }

    private fun inflateBytes(compressed: ByteArray): ByteArray? {
        return try {
            val inflater = Inflater(false)
            inflater.setInput(compressed)
            val outputStream = ByteArrayOutputStream(compressed.size * 2)
            val buffer = ByteArray(1024)
            while (!inflater.finished()) {
                val count = inflater.inflate(buffer)
                if (count == 0 && inflater.needsInput()) break
                outputStream.write(buffer, 0, count)
            }
            inflater.end()
            outputStream.toByteArray()
        } catch (e: Exception) {
            // Try with nowrap = true
            try {
                val inflater = Inflater(true)
                inflater.setInput(compressed)
                val outputStream = ByteArrayOutputStream(compressed.size * 2)
                val buffer = ByteArray(1024)
                while (!inflater.finished()) {
                    val count = inflater.inflate(buffer)
                    if (count == 0 && inflater.needsInput()) break
                    outputStream.write(buffer, 0, count)
                }
                inflater.end()
                outputStream.toByteArray()
            } catch (e2: Exception) {
                null
            }
        }
    }

    private fun extractTextOperators(streamStr: String, out: StringBuilder) {
        // Match (text) Tj
        val tjRegex = Regex("\\(([^()]+)\\)\\s*Tj")
        for (m in tjRegex.findAll(streamStr)) {
            val t = m.groupValues[1]
            if (t.isNotBlank()) out.append(t).append(" ")
        }

        // Match [(item) 120 (item2)] TJ
        val bigTjRegex = Regex("\\[([^\\]]+)\\]\\s*TJ")
        for (m in bigTjRegex.findAll(streamStr)) {
            val arrayContent = m.groupValues[1]
            val innerMatches = Regex("\\(([^()]+)\\)").findAll(arrayContent)
            for (im in innerMatches) {
                out.append(im.groupValues[1])
            }
            out.append(" ")
        }
    }

    private fun getFileName(context: Context, uri: Uri): String {
        var name = "document"
        val cursor = context.contentResolver.query(uri, null, null, null, null)
        cursor?.use {
            if (it.moveToFirst()) {
                val index = it.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                if (index >= 0) {
                    name = it.getString(index) ?: "document"
                }
            }
        }
        return name
    }
}
