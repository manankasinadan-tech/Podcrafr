package com.example.data.model

/**
 * Represents a single turn of speech in a podcast dialogue.
 */
data class DialogueTurn(
    val id: Int,
    val speaker: String,
    val text: String,
    val reactionHints: String = "", // e.g. "(enthousiaste)", "(rires)", "[Chuchoté]"
    var voice: PodcastVoice = PodcastVoice.KORE,
    var audioFilePath: String? = null,
    var durationMs: Long = 0L,
    var isGenerated: Boolean = false,
    var isGenerating: Boolean = false
) {
    /**
     * Cleans up text for synthesis while keeping emotional directions for Gemini
     */
    fun getPromptText(): String {
        return if (reactionHints.isNotBlank()) {
            "$reactionHints $text"
        } else {
            text
        }
    }
}

/**
 * Model representing a full podcast project in the studio.
 */
data class PodcastProject(
    val id: String = System.currentTimeMillis().toString(),
    val title: String,
    val scriptRaw: String,
    val turns: List<DialogueTurn> = emptyList(),
    val speakers: Map<String, PodcastVoice> = emptyMap(),
    val fullAudioPath: String? = null,
    val totalDurationMs: Long = 0L
)
