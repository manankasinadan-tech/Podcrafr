package com.example.audio

import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Utility for handling Linear PCM to WAV conversion, WAV headers,
 * and stitching dialogue turn audio files into a seamless master podcast file.
 */
object WavHelper {

    private const val DEFAULT_SAMPLE_RATE = 24000 // Standard Gemini Audio sample rate
    private const val DEFAULT_CHANNELS = 1        // Mono
    private const val DEFAULT_BITS_PER_SAMPLE = 16 // 16-bit PCM

    /**
     * Builds a standard 44-byte RIFF/WAVE header for linear PCM audio.
     */
    fun createWavHeader(
        pcmDataSize: Int,
        sampleRate: Int = DEFAULT_SAMPLE_RATE,
        channels: Int = DEFAULT_CHANNELS,
        bitsPerSample: Int = DEFAULT_BITS_PER_SAMPLE
    ): ByteArray {
        val totalDataLen = pcmDataSize + 36
        val byteRate = sampleRate * channels * bitsPerSample / 8
        val blockAlign = channels * bitsPerSample / 8

        val header = ByteArray(44)
        val buffer = ByteBuffer.wrap(header).order(ByteOrder.LITTLE_ENDIAN)

        // RIFF header
        buffer.put('R'.code.toByte())
        buffer.put('I'.code.toByte())
        buffer.put('F'.code.toByte())
        buffer.put('F'.code.toByte())
        buffer.putInt(totalDataLen)
        buffer.put('W'.code.toByte())
        buffer.put('A'.code.toByte())
        buffer.put('V'.code.toByte())
        buffer.put('E'.code.toByte())

        // "fmt " subchunk
        buffer.put('f'.code.toByte())
        buffer.put('m'.code.toByte())
        buffer.put('t'.code.toByte())
        buffer.put(' '.code.toByte())
        buffer.putInt(16) // Subchunk1Size for PCM
        buffer.putShort(1.toShort()) // AudioFormat 1 = PCM
        buffer.putShort(channels.toShort())
        buffer.putInt(sampleRate)
        buffer.putInt(byteRate)
        buffer.putShort(blockAlign.toShort())
        buffer.putShort(bitsPerSample.toShort())

        // "data" subchunk
        buffer.put('d'.code.toByte())
        buffer.put('a'.code.toByte())
        buffer.put('t'.code.toByte())
        buffer.put('a'.code.toByte())
        buffer.putInt(pcmDataSize)

        return header
    }

    /**
     * Wraps raw PCM bytes with a standard WAV header.
     */
    fun pcmToWav(
        pcmData: ByteArray,
        sampleRate: Int = DEFAULT_SAMPLE_RATE,
        channels: Int = DEFAULT_CHANNELS,
        bitsPerSample: Int = DEFAULT_BITS_PER_SAMPLE
    ): ByteArray {
        val header = createWavHeader(pcmData.size, sampleRate, channels, bitsPerSample)
        val wavBytes = ByteArray(header.size + pcmData.size)
        System.arraycopy(header, 0, wavBytes, 0, header.size)
        System.arraycopy(pcmData, 0, wavBytes, header.size, pcmData.size)
        return wavBytes
    }

    /**
     * Stitches multiple WAV dialogue turn files into a single master podcast WAV file.
     * Inserts a slight natural silence pause (e.g. 250ms) between turns.
     */
    fun stitchWavFiles(
        inputFiles: List<File>,
        outputFile: File,
        pauseMs: Int = 250,
        sampleRate: Int = DEFAULT_SAMPLE_RATE,
        channels: Int = DEFAULT_CHANNELS,
        bitsPerSample: Int = DEFAULT_BITS_PER_SAMPLE
    ): Boolean {
        if (inputFiles.isEmpty()) return false

        try {
            // First calculate total raw PCM bytes across all files
            var totalPcmBytes = 0L
            val pauseBytes = (sampleRate * channels * (bitsPerSample / 8) * (pauseMs / 1000.0)).toInt()
            val pauseBuffer = ByteArray(pauseBytes) // Silent PCM zeros

            val validFiles = inputFiles.filter { it.exists() && it.length() > 44 }
            if (validFiles.isEmpty()) return false

            for ((index, file) in validFiles.withIndex()) {
                val pcmLen = file.length() - 44
                if (pcmLen > 0) {
                    totalPcmBytes += pcmLen
                    if (index < validFiles.size - 1) {
                        totalPcmBytes += pauseBytes
                    }
                }
            }

            if (totalPcmBytes > Int.MAX_VALUE) {
                // Safeguard against extreme overflows
                totalPcmBytes = Int.MAX_VALUE.toLong()
            }

            // Write output file
            outputFile.parentFile?.mkdirs()
            FileOutputStream(outputFile).use { fos ->
                // Write master WAV header
                val header = createWavHeader(totalPcmBytes.toInt(), sampleRate, channels, bitsPerSample)
                fos.write(header)

                val buffer = ByteArray(8192)
                for ((index, file) in validFiles.withIndex()) {
                    FileInputStream(file).use { fis ->
                        // Skip 44-byte WAV header of individual file
                        fis.skip(44)
                        var bytesRead: Int
                        while (fis.read(buffer).also { bytesRead = it } != -1) {
                            fos.write(buffer, 0, bytesRead)
                        }
                    }
                    // Insert natural conversational pause between dialogue lines
                    if (index < validFiles.size - 1) {
                        fos.write(pauseBuffer)
                    }
                }
            }
            return true
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }
    }

    /**
     * Calculates duration in milliseconds for a WAV file.
     */
    fun getWavDurationMs(file: File, sampleRate: Int = DEFAULT_SAMPLE_RATE): Long {
        if (!file.exists() || file.length() <= 44) return 0L
        val pcmBytes = file.length() - 44
        val bytesPerSec = sampleRate * DEFAULT_CHANNELS * (DEFAULT_BITS_PER_SAMPLE / 8)
        return (pcmBytes * 1000L) / bytesPerSec
    }
}
