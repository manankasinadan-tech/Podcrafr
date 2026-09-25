package com.example.data.model

import androidx.compose.ui.graphics.Color
import com.example.ui.theme.VoiceColorAoede
import com.example.ui.theme.VoiceColorCharon
import com.example.ui.theme.VoiceColorFenrir
import com.example.ui.theme.VoiceColorKore
import com.example.ui.theme.VoiceColorLeda
import com.example.ui.theme.VoiceColorPuck

/**
 * 6 Human Podcast Voices available for multi-voice podcasts.
 * Maps to Gemini prebuilt voices and Android TTS fallback parameters.
 */
enum class PodcastVoice(
    val id: String,
    val apiVoiceName: String,     // Official Gemini prebuilt voice
    val displayName: String,      // Human character name
    val genderFr: String,
    val roleFr: String,           // Podcast role
    val personalityFr: String,    // Tone & style
    val pitchTts: Float,          // Android TTS fallback pitch
    val speedTts: Float,          // Android TTS fallback rate
    val color: Color,
    val sampleTextFr: String
) {
    KORE(
        id = "kore",
        apiVoiceName = "Kore",
        displayName = "Clara",
        genderFr = "Féminin",
        roleFr = "Animatrice principale",
        personalityFr = "Chaleureuse, posée, bienveillante et naturelle",
        pitchTts = 1.05f,
        speedTts = 1.0f,
        color = VoiceColorKore,
        sampleTextFr = "Bonjour à toutes et à tous ! Bienvenue dans cet épisode de podcast."
    ),
    PUCK(
        id = "puck",
        apiVoiceName = "Puck",
        displayName = "Alex",
        genderFr = "Masculin",
        roleFr = "Co-animateur dynamique",
        personalityFr = "Énergique, jeune, curieux, spontané et plein d'humour",
        pitchTts = 1.0f,
        speedTts = 1.05f,
        color = VoiceColorPuck,
        sampleTextFr = "Salut tout le monde ! Franchement aujourd'hui, le sujet est juste incroyable !"
    ),
    CHARON(
        id = "charon",
        apiVoiceName = "Charon",
        displayName = "Marc",
        genderFr = "Masculin",
        roleFr = "Expert & Analyste",
        personalityFr = "Posé, voix grave, réfléchi, captivant et précis",
        pitchTts = 0.80f,
        speedTts = 0.95f,
        color = VoiceColorCharon,
        sampleTextFr = "Si l'on regarde les faits avec recul, l'explication est bien plus fascinante."
    ),
    AOEDE(
        id = "aoede",
        apiVoiceName = "Sophie",
        displayName = "Sophie",
        genderFr = "Féminin",
        roleFr = "Chroniqueuse pétillante",
        personalityFr = "Expressive, vive, souriante, pleine de réparties",
        pitchTts = 1.15f,
        speedTts = 1.04f,
        color = VoiceColorAoede,
        sampleTextFr = "Ah attends Marc, là je suis obligée d'intervenir ! C'est dingue non ?"
    ),
    FENRIR(
        id = "fenrir",
        apiVoiceName = "Fenrir",
        displayName = "Thomas",
        genderFr = "Masculin",
        roleFr = "Narrateur percutant",
        personalityFr = "Intense, charismatique, voix radiophonique puissante",
        pitchTts = 0.88f,
        speedTts = 0.97f,
        color = VoiceColorFenrir,
        sampleTextFr = "Plongeons immédiatement au cœur de cette histoire hors du commun."
    ),
    LEDA(
        id = "leda",
        apiVoiceName = "Leda",
        displayName = "Emma",
        genderFr = "Féminin",
        roleFr = "Narratrice intimiste",
        personalityFr = "Douce, contemplative, émotive et poétique",
        pitchTts = 1.0f,
        speedTts = 0.92f,
        color = VoiceColorLeda,
        sampleTextFr = "Prenez une grande inspiration... Imaginez un monde où tout serait différent."
    );

    companion object {
        fun fromId(id: String): PodcastVoice = entries.firstOrNull { it.id.equals(id, ignoreCase = true) } ?: KORE
        fun fromApiName(name: String): PodcastVoice = entries.firstOrNull { it.apiVoiceName.equals(name, ignoreCase = true) } ?: KORE
    }
}
