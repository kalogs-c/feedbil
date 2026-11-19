package com.kalogsc.feedbil.ui.articles

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import com.kalogsc.feedbil.domain.model.Article
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ArticleContentFormatterTest {

    private fun article(description: String? = null, contentHtml: String? = null): Article = Article(
        feedId = 1,
        guid = null,
        link = "https://example.com/story",
        title = "Story",
        description = description,
        contentHtml = contentHtml,
        author = null,
        publishedAt = 0L,
    )

    @Test
    fun `prefers content html over description`() {
        val blocks = ArticleContentFormatter.format(
            article(description = "<p>short</p>", contentHtml = "<p>full content</p>"),
        )

        assertEquals(listOf("full content"), blocks.texts())
    }

    @Test
    fun `falls back to description`() {
        val blocks = ArticleContentFormatter.format(article(description = "<p>short</p>"))

        assertEquals(listOf("short"), blocks.texts())
    }

    @Test
    fun `absent content produces no blocks`() {
        assertTrue(ArticleContentFormatter.format(article()).isEmpty())
        assertTrue(ArticleContentFormatter.parse(null).isEmpty())
        assertTrue(ArticleContentFormatter.parse("   ").isEmpty())
    }

    @Test
    fun `keeps bold italic underline and safe links`() {
        val blocks = ArticleContentFormatter.parse(
            """<p>Plain <b>bold</b> <i>italic</i> <u>under</u> """ +
                """<a href="https://example.com/more">link</a>.</p>""",
        )

        val paragraph = blocks.single() as ArticleBlock.Paragraph
        assertEquals("Plain bold italic under link.", paragraph.text.text)
        paragraph.text.assertStyled("bold") { it.fontWeight == FontWeight.Bold }
        paragraph.text.assertStyled("italic") { it.fontStyle == FontStyle.Italic }
        paragraph.text.assertStyled("under") { it.textDecoration == TextDecoration.Underline }
        val links = paragraph.text.getLinkAnnotations(0, paragraph.text.length)
        assertEquals(1, links.size)
        assertEquals("https://example.com/more", (links.single().item as LinkAnnotation.Url).url)
    }

    @Test
    fun `drops scripts styles iframes and unsafe link targets`() {
        val blocks = ArticleContentFormatter.parse(
            """<script>alert('x')</script><style>.a{color:red}</style>""" +
                """<iframe src="https://tracker.example"></iframe>""" +
                """<p>Safe <a href="javascript:alert(1)">text</a></p>""",
        )

        assertEquals(listOf("Safe text"), blocks.texts())
        val paragraph = blocks.single() as ArticleBlock.Paragraph
        assertTrue(paragraph.text.getLinkAnnotations(0, paragraph.text.length).isEmpty())
    }

    @Test
    fun `extracts only http and https image urls`() {
        val blocks = ArticleContentFormatter.parse(
            """<img src="https://cdn.example/one.jpg">""" +
                """<img src="javascript:alert(1)">""" +
                """<img src="//cdn.example/two.jpg">""" +
                """<p>Text</p>""" +
                """<IMG SRC='http://cdn.example/three.png' />""",
        )

        assertEquals(
            listOf(
                ArticleBlock.Image("https://cdn.example/one.jpg"),
                ArticleBlock.Image("http://cdn.example/three.png"),
            ),
            blocks.filterIsInstance<ArticleBlock.Image>(),
        )
        assertEquals(listOf("Text"), blocks.texts())
    }

    @Test
    fun `decodes entities and preserves explicit line breaks`() {
        val blocks = ArticleContentFormatter.parse(
            "<p>A &amp; B<br>C &#x1F600; &lt;ok&gt; &nbsp; fim</p>",
        )

        assertEquals(listOf("A & B\nC 😀 <ok> \u00A0 fim"), blocks.texts())
    }

    @Test
    fun `separates paragraphs and list items`() {
        val blocks = ArticleContentFormatter.parse("<p>One</p><ul><li>Two</li><li>Three</li></ul>")

        assertEquals(listOf("One", "Two", "Three"), blocks.texts())
    }

    @Test
    fun `malformed markup does not crash`() {
        val blocks = ArticleContentFormatter.parse("<p>Broken <b>tag <img src=https://x/y.jpg")

        assertTrue(blocks.isNotEmpty())
        assertFalse(blocks.texts().joinToString(" ").contains("<b>"))
    }

    @Test
    fun `entity encoded unsafe urls are rejected`() {
        val blocks = ArticleContentFormatter.parse(
            """<p><a href="javascript&#58;alert(1)">link</a>""" +
                """<img src="javascript&#58;alert(1)"></p>""",
        )

        val paragraph = blocks.single() as ArticleBlock.Paragraph
        assertEquals("link", paragraph.text.text)
        assertTrue(paragraph.text.getLinkAnnotations(0, paragraph.text.length).isEmpty())
        assertTrue(blocks.filterIsInstance<ArticleBlock.Image>().isEmpty())
    }

    @Test
    fun `self closing skipped tags do not discard the rest of the article`() {
        val blocks = ArticleContentFormatter.parse(
            """<iframe src="https://tracker.example"/><p>Text after iframe</p>""",
        )

        assertEquals(listOf("Text after iframe"), blocks.texts())
    }

    @Test
    fun `closing tags with whitespace do not discard the rest of the article`() {
        val blocks = ArticleContentFormatter.parse(
            """<script>alert(1)</script ><p>Text after script</p>""",
        )

        assertEquals(listOf("Text after script"), blocks.texts())
    }

    @Test
    fun `unclosed skipped tag keeps following content as plain text`() {
        val blocks = ArticleContentFormatter.parse("<script>alert(1)<p>Still here</p>")

        assertTrue(blocks.texts().any { it.contains("Still here") })
    }

    @Test
    fun `image alt text is decoded and kept`() {
        val blocks = ArticleContentFormatter.parse(
            """<img src="https://cdn.example/a.jpg" alt="Foto &amp; texto">""",
        )

        assertEquals(
            ArticleBlock.Image("https://cdn.example/a.jpg", "Foto & texto"),
            blocks.single(),
        )
    }

    private fun AnnotatedString.assertStyled(expected: String, matches: (SpanStyle) -> Boolean) {
        assertTrue(spanStyles.any { matches(it.item) && substring(it.start, it.end) == expected })
    }

    private fun List<ArticleBlock>.texts(): List<String> =
        filterIsInstance<ArticleBlock.Paragraph>().map { it.text.text }
}
