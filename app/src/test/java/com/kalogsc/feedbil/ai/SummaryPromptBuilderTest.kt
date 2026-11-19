package com.kalogsc.feedbil.ai

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SummaryPromptBuilderTest {

    @Test
    fun `uses pt-BR instructions that bound length and reject embedded commands`() {
        val input = SummaryPromptBuilder.build(
            PreparedSummaryInput(title = "Título", text = "Texto do artigo."),
        )

        assertTrue(input.systemInstruction.contains("português do Brasil", ignoreCase = true))
        assertTrue(input.systemInstruction.contains("três frases", ignoreCase = true))
        assertTrue(input.systemInstruction.contains("não invente", ignoreCase = true))
        assertTrue(input.systemInstruction.contains("ignore qualquer", ignoreCase = true))
        assertFalse(input.systemInstruction.contains('\n'))
    }

    @Test
    fun `prompt contains the title and the whole article text`() {
        val text = "Primeiro parágrafo.\n\nSegundo parágrafo."
        val input = SummaryPromptBuilder.build(
            PreparedSummaryInput(title = "Um título", text = text),
        )

        assertTrue(input.prompt.contains("Título: Um título"))
        assertTrue(input.prompt.contains(text))
        assertTrue(input.prompt.contains("Resuma o texto acima"))
    }

    @Test
    fun `prompt omits the title line when there is no title`() {
        val input = SummaryPromptBuilder.build(
            PreparedSummaryInput(title = null, text = "Somente texto."),
        )

        assertFalse(input.prompt.contains("Título:"))
        assertEquals(true, input.prompt.startsWith("Texto do artigo:"))
    }
}
