package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.PodcastVoice
import com.example.parser.ScriptParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("PodCraft AI", appName)
    }

    @Test
    fun `test script parser extracts dialogue turns and reactions`() {
        val script = """
            Clara: [Souriante] Bienvenue à tous dans ce nouvel épisode !
            Alex: [Rires] Salut Clara ! Le sujet du jour est passionnant.
            Marc: [Posé] Absolument, regardons les chiffres.
        """.trimIndent()

        val (turns, voiceMap) = ScriptParser.parseScript(script)
        assertEquals(3, turns.size)
        assertEquals("Clara", turns[0].speaker)
        assertEquals("[Souriante]", turns[0].reactionHints)
        assertEquals("Alex", turns[1].speaker)
        assertEquals("[Rires]", turns[1].reactionHints)
        assertEquals("Marc", turns[2].speaker)
        assertEquals(PodcastVoice.KORE, voiceMap["Clara"])
        assertEquals(PodcastVoice.PUCK, voiceMap["Alex"])
        assertEquals(PodcastVoice.CHARON, voiceMap["Marc"])
    }
}
