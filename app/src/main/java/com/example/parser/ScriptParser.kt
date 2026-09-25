package com.example.parser

import com.example.data.model.DialogueTurn
import com.example.data.model.PodcastVoice

object ScriptParser {

    private val SPEAKER_LINE_REGEX = Regex(
        "^\\s*(?:\\[([^\\]]+)\\]|([^:\\-–—\\[\\]]+?)(?:\\s*[:\\-–—]\\s*))(.*)$",
        RegexOption.MULTILINE
    )

    private val REACTION_BRACKETS_REGEX = Regex("^\\[([^\\]]+)\\]\\s*(.*)$")
    private val REACTION_PARENS_REGEX = Regex("^\\(([^\\)]+)\\)\\s*(.*)$")

    /**
     * Parses raw script text into structured DialogueTurns,
     * maintaining emotional reaction hints and speaker-voice mapping.
     */
    fun parseScript(
        rawScript: String,
        existingVoiceMap: Map<String, PodcastVoice> = emptyMap()
    ): Pair<List<DialogueTurn>, Map<String, PodcastVoice>> {
        val lines = rawScript.lines()
        val rawTurns = mutableListOf<ParsedLine>()
        var currentSpeaker = "Clara"
        var currentText = StringBuilder()
        var currentReaction = ""

        for (line in lines) {
            val trimmed = line.trim()
            if (trimmed.isEmpty()) continue

            val match = SPEAKER_LINE_REGEX.find(trimmed)
            if (match != null) {
                // If there's an ongoing turn, save it
                if (currentText.isNotEmpty()) {
                    rawTurns.add(ParsedLine(currentSpeaker, currentReaction, currentText.toString().trim()))
                    currentText = StringBuilder()
                    currentReaction = ""
                }

                val speakerGroup = match.groups[1]?.value ?: match.groups[2]?.value ?: "Intervenant"
                val restOfLine = match.groups[3]?.value?.trim() ?: ""

                currentSpeaker = cleanSpeakerName(speakerGroup)

                // Extract emotion tags at the beginning of the turn
                val (reaction, speechText) = extractReaction(restOfLine)
                currentReaction = reaction
                currentText.append(speechText)
            } else {
                // Continuation line
                if (currentText.isNotEmpty()) {
                    currentText.append(" ").append(trimmed)
                } else {
                    val (reaction, speechText) = extractReaction(trimmed)
                    currentReaction = reaction
                    currentText.append(speechText)
                }
            }
        }

        if (currentText.isNotEmpty()) {
            rawTurns.add(ParsedLine(currentSpeaker, currentReaction, currentText.toString().trim()))
        }

        // Build speaker to voice map
        val allSpeakers = rawTurns.map { it.speaker }.distinct()
        val voiceMap = buildVoiceMap(allSpeakers, existingVoiceMap)

        // Convert to DialogueTurn list
        val turns = rawTurns.mapIndexed { index, parsed ->
            val voice = voiceMap[parsed.speaker] ?: PodcastVoice.KORE
            DialogueTurn(
                id = index,
                speaker = parsed.speaker,
                text = parsed.text,
                reactionHints = parsed.reaction,
                voice = voice
            )
        }

        return Pair(turns, voiceMap)
    }

    private fun cleanSpeakerName(name: String): String {
        return name.trim()
            .replace(Regex("[^\\p{L}\\p{Nd}\\s_-]"), "")
            .trim()
            .ifBlank { "Intervenant" }
            .replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
    }

    private fun extractReaction(text: String): Pair<String, String> {
        val bracketMatch = REACTION_BRACKETS_REGEX.find(text)
        if (bracketMatch != null) {
            val reaction = "[${bracketMatch.groupValues[1]}]"
            val rest = bracketMatch.groupValues[2].trim()
            return Pair(reaction, rest)
        }

        val parenMatch = REACTION_PARENS_REGEX.find(text)
        if (parenMatch != null) {
            val reaction = "(${parenMatch.groupValues[1]})"
            val rest = parenMatch.groupValues[2].trim()
            return Pair(reaction, rest)
        }

        return Pair("", text)
    }

    /**
     * Maps speakers to the 6 distinct voices, prioritizing natural name matches
     * (e.g. "Clara" -> KORE, "Alex" -> PUCK, "Marc" -> CHARON, etc.)
     * and cycling gracefully for custom speaker names.
     */
    fun buildVoiceMap(
        speakers: List<String>,
        existingVoiceMap: Map<String, PodcastVoice>
    ): Map<String, PodcastVoice> {
        val result = mutableMapOf<String, PodcastVoice>()
        val availableVoices = PodcastVoice.entries.toList()
        var cycleIndex = 0

        for (speaker in speakers) {
            if (existingVoiceMap.containsKey(speaker)) {
                result[speaker] = existingVoiceMap.getValue(speaker)
                continue
            }

            // Name-based smart heuristic
            val lower = speaker.lowercase()
            val matchedVoice = when {
                lower.contains("clara") || lower.contains("femme") || lower.contains("animatrice") -> PodcastVoice.KORE
                lower.contains("alex") || lower.contains("homme 1") || lower.contains("jeune") -> PodcastVoice.PUCK
                lower.contains("marc") || lower.contains("expert") || lower.contains("professeur") -> PodcastVoice.CHARON
                lower.contains("sophie") || lower.contains("chronique") || lower.contains("fille") -> PodcastVoice.AOEDE
                lower.contains("thomas") || lower.contains("narrat") || lower.contains("journaliste") -> PodcastVoice.FENRIR
                lower.contains("emma") || lower.contains("poet") || lower.contains("douce") -> PodcastVoice.LEDA
                else -> {
                    val assigned = availableVoices[cycleIndex % availableVoices.size]
                    cycleIndex++
                    assigned
                }
            }
            result[speaker] = matchedVoice
        }

        return result
    }

    private data class ParsedLine(
        val speaker: String,
        val reaction: String,
        val text: String
    )
}
