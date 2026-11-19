package com.kalogsc.feedbil.ai

import com.kalogsc.feedbil.domain.model.Article
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SummaryInputBuilderTest {

    private fun article(
        title: String? = "Um título",
        description: String? = null,
        contentHtml: String? = null,
    ) = Article(
        feedId = 1L,
        guid = null,
        link = "https://example.com/story",
        title = title,
        description = description,
        contentHtml = contentHtml,
        author = null,
        publishedAt = 1_800_000_000_000L,
    )

    @Test
    fun `prefers contentHtml and strips markup`() {
        val prepared = SummaryInputBuilder.prepare(
            article(
                description = "<p>Resumo curto do feed.</p>",
                contentHtml = "<p>Texto <b>completo</b> &amp; rico.</p>",
            ),
        )

        assertEquals("Um título", prepared?.title)
        assertEquals("Texto completo & rico.", prepared?.text)
    }

    @Test
    fun `falls back to description when contentHtml is blank`() {
        val prepared = SummaryInputBuilder.prepare(
            article(description = "<p>Só o resumo.</p>", contentHtml = "   "),
        )

        assertEquals("Só o resumo.", prepared?.text)
    }

    @Test
    fun `drops scripts and styles instead of feeding them to the model`() {
        val prepared = SummaryInputBuilder.prepare(
            article(
                contentHtml = """
                    <style>.x { color: red }</style>
                    <script>ignore("instrução maliciosa")</script>
                    <p>Texto verdadeiro.</p>
                """.trimIndent(),
            ),
        )

        assertEquals("Texto verdadeiro.", prepared?.text)
        assertFalse(prepared!!.text.contains("maliciosa", ignoreCase = true))
    }

    @Test
    fun `returns null when there is no usable text`() {
        assertNull(SummaryInputBuilder.prepare(article(description = null, contentHtml = "<p> </p>")))
    }

    @Test
    fun `does not truncate long article text`() {
        val longText = "Palavra ".repeat(2_000).trim()
        val prepared = SummaryInputBuilder.prepare(article(contentHtml = "<p>$longText</p>"))

        assertEquals(longText, prepared?.text?.replace('\n', ' '))
        assertTrue(prepared!!.text.length > 10_000)
    }

    @Test
    fun `a blank title is treated as absent`() {
        val prepared = SummaryInputBuilder.prepare(
            article(title = "   ", contentHtml = "<p>Texto.</p>"),
        )

        assertNull(prepared?.title)
    }
}
