package com.kalogsc.feedbil.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class ShareTextBuilderTest {

    @Test
    fun `normal share puts the title and source url on separate lines`() {
        val text = ShareTextBuilder.build(
            ShareContent.Normal(title = "Um título", url = SOURCE_URL),
            untitled = UNTITLED,
        )

        assertEquals("Um título\n$SOURCE_URL", text)
    }

    @Test
    fun `normal share falls back to the localized untitled label`() {
        val missing = ShareTextBuilder.build(
            ShareContent.Normal(title = null, url = SOURCE_URL),
            untitled = UNTITLED,
        )
        val blank = ShareTextBuilder.build(
            ShareContent.Normal(title = "   ", url = SOURCE_URL),
            untitled = UNTITLED,
        )

        assertEquals("$UNTITLED\n$SOURCE_URL", missing)
        assertEquals(missing, blank)
    }

    @Test
    fun `a feed title with line breaks stays on one line`() {
        val text = ShareTextBuilder.build(
            ShareContent.Normal(title = "Linha um\nLinha dois\r\nLinha três", url = SOURCE_URL),
            untitled = UNTITLED,
        )

        assertEquals("Linha um Linha dois Linha três\n$SOURCE_URL", text)
    }

    @Test
    fun `ai share contains only the summary and the source url`() {
        val text = ShareTextBuilder.build(
            ShareContent.Ai(summary = "Resumo curto.", url = SOURCE_URL),
            untitled = UNTITLED,
        )

        assertEquals("Resumo curto.\n\n$SOURCE_URL", text)
    }

    @Test
    fun `ai share does not add a generated-content label`() {
        val text = ShareTextBuilder.build(
            ShareContent.Ai(summary = "Resumo curto.", url = SOURCE_URL),
            untitled = UNTITLED,
        )

        assertFalse(text.contains("Resumo gerado por IA", ignoreCase = true))
    }

    private companion object {
        const val SOURCE_URL = "https://example.com/story"
        const val UNTITLED = "Sem título"
    }
}
