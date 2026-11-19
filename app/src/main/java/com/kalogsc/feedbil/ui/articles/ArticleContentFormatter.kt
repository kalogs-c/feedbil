package com.kalogsc.feedbil.ui.articles

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import com.kalogsc.feedbil.domain.HttpUrlValidator
import com.kalogsc.feedbil.domain.model.Article

/** A readable piece of feed content. Images are blocks so they can size naturally. */
sealed interface ArticleBlock {
    data class Paragraph(val text: AnnotatedString) : ArticleBlock
    data class Image(val url: String, val alt: String? = null) : ArticleBlock
}

/**
 * Turns the HTML supplied by a feed into Compose text and image blocks.
 *
 * Feed content is untrusted: scripts, styles, iframes and every non-http(s)
 * URL are dropped. Only a small set of formatting tags is kept. The parser is
 * deliberately forgiving — malformed markup produces less formatting, never
 * an exception, and never discards the rest of the article — and has no
 * Android dependency so it can be tested on the JVM.
 */
object ArticleContentFormatter {

    private const val MAX_HTML_CHARS = 200_000

    private val tagName = Regex("</?([A-Za-z][A-Za-z0-9:-]*)")
    private val whitespace = Regex("\\s+")

    private val skippedTags = setOf(
        "script", "style", "iframe", "noscript", "svg", "object", "embed", "form",
    )
    private val paragraphTags = setOf(
        "p", "div", "li", "ul", "ol", "blockquote", "h1", "h2", "h3", "h4", "h5", "h6",
        "table", "tr", "section", "article", "figure", "figcaption",
    )
    private val skippedCloseTags = skippedTags.associateWith {
        Regex("""(?i)</\s*$it\s*>""")
    }
    private val srcAttribute = attributeRegex("src")
    private val hrefAttribute = attributeRegex("href")
    private val altAttribute = attributeRegex("alt")

    /** Prefer the full `content:encoded` field; fall back to the description. */
    fun format(article: Article): List<ArticleBlock> =
        parse(article.contentHtml ?: article.description)

    fun parse(html: String?): List<ArticleBlock> {
        if (html.isNullOrBlank()) return emptyList()

        val source = html.take(MAX_HTML_CHARS)
        val blocks = mutableListOf<ArticleBlock>()
        var paragraph = Paragraph()

        fun finishParagraph() {
            paragraph.toAnnotatedString()?.let { blocks += ArticleBlock.Paragraph(it) }
            paragraph = Paragraph()
        }

        var index = 0
        while (index < source.length) {
            if (source[index] != '<') {
                val next = source.indexOf('<', index).let { if (it < 0) source.length else it }
                val text = decodeEntities(source.substring(index, next)).replace(whitespace, " ")
                if (text.isNotEmpty()) paragraph.append(text)
                index = next
                continue
            }

            val tagEnd = findTagEnd(source, index)
            if (tagEnd < 0) {
                paragraph.append("<")
                index++
                continue
            }

            val rawTag = source.substring(index, tagEnd + 1)
            index = tagEnd + 1

            val nameMatch = tagName.find(rawTag)
            if (nameMatch == null) {
                paragraph.append(decodeEntities(rawTag))
                continue
            }

            val closing = rawTag.startsWith("</")
            when (val name = nameMatch.groupValues[1].lowercase()) {
                in skippedTags -> if (!closing) {
                    index = skipSkippedContent(source, index, rawTag, name)
                }

                "img" -> if (!closing) {
                    val src = HttpUrlValidator.sanitize(decodeEntities(attribute(rawTag, srcAttribute)))
                    if (src != null) {
                        val alt = decodeEntities(attribute(rawTag, altAttribute))
                            .trim()
                            .takeIf { it.isNotEmpty() }
                        finishParagraph()
                        blocks += ArticleBlock.Image(src, alt)
                    }
                }

                "br" -> paragraph.append("\n")
                in paragraphTags -> finishParagraph()
                "b", "strong" -> {
                    paragraph.flushRun()
                    paragraph.bold = !closing
                }

                "i", "em" -> {
                    paragraph.flushRun()
                    paragraph.italic = !closing
                }

                "u" -> {
                    paragraph.flushRun()
                    paragraph.underline = !closing
                }

                "a" -> {
                    paragraph.flushRun()
                    paragraph.link = if (closing) null else {
                        HttpUrlValidator.sanitize(decodeEntities(attribute(rawTag, hrefAttribute)))
                    }
                }

                else -> Unit
            }
        }

        finishParagraph()
        return blocks
    }

    /**
     * Moves past the body of a skipped tag. Self-closing tags have no body, and
     * closing tags tolerate whitespace. If no closing tag is found, parsing
     * continues after the opening tag: its body may be shown as plain text, but
     * a malformed tag can never hide the rest of the article.
     */
    private fun skipSkippedContent(source: String, start: Int, rawTag: String, name: String): Int {
        if (rawTag.trimEnd().endsWith("/")) return start
        val close = skippedCloseTags.getValue(name).find(source, start) ?: return start
        return close.range.last + 1
    }

    /** Finds the `>` that closes a tag, ignoring `>` inside quoted attributes. */
    private fun findTagEnd(source: String, start: Int): Int {
        var quote: Char? = null
        for (i in start + 1 until source.length) {
            val char = source[i]
            when {
                quote != null -> if (char == quote) quote = null
                char == '"' || char == '\'' -> quote = char
                char == '>' -> return i
            }
        }
        return -1
    }

    private fun attributeRegex(name: String): Regex = Regex(
        """(?i)(?<=\s)${Regex.escape(name)}\s*=\s*(?:"([^"]*)"|'([^']*)'|([^\s>]+))""",
    )

    private fun attribute(tag: String, regex: Regex): String? {
        val match = regex.find(tag) ?: return null
        return match.groupValues[1]
            .ifEmpty { match.groupValues[2] }
            .ifEmpty { match.groupValues[3] }
    }

    private val namedEntities = mapOf(
        "amp" to "&", "lt" to "<", "gt" to ">", "quot" to "\"", "apos" to "'",
        "nbsp" to "\u00A0", "ndash" to "–", "mdash" to "—", "hellip" to "…",
        "lsquo" to "‘", "rsquo" to "’", "ldquo" to "“", "rdquo" to "”",
        "bull" to "•", "middot" to "·", "copy" to "©", "reg" to "®", "trade" to "™",
        "aacute" to "á", "Aacute" to "Á", "eacute" to "é", "Eacute" to "É",
        "iacute" to "í", "Iacute" to "Í", "oacute" to "ó", "Oacute" to "Ó",
        "uacute" to "ú", "Uacute" to "Ú", "atilde" to "ã", "Atilde" to "Ã",
        "ccedil" to "ç", "Ccedil" to "Ç",
    )

    private fun decodeEntities(text: String?): String {
        if (text == null) return ""
        if ('&' !in text) return text
        val result = StringBuilder(text.length)
        var index = 0
        while (index < text.length) {
            if (text[index] != '&') {
                result.append(text[index])
                index++
                continue
            }
            val semicolon = text.indexOf(';', index + 1)
            if (semicolon < 0 || semicolon - index > 12) {
                result.append('&')
                index++
                continue
            }
            val entity = text.substring(index + 1, semicolon)
            val decoded = when {
                entity.startsWith("#x", ignoreCase = true) ->
                    entity.substring(2).toIntOrNull(16)?.let(::codePoint)
                entity.startsWith("#") -> entity.substring(1).toIntOrNull()?.let(::codePoint)
                else -> namedEntities[entity]
            }
            if (decoded != null) {
                result.append(decoded)
                index = semicolon + 1
            } else {
                result.append('&')
                index++
            }
        }
        return result.toString()
    }

    private fun codePoint(value: Int): String? =
        if (value in 0..0x10FFFF && value !in 0xD800..0xDFFF) {
            String(Character.toChars(value))
        } else {
            null
        }

    /** Accumulates one paragraph, applying style spans to each text run. */
    private class Paragraph {
        private val builder = AnnotatedString.Builder()
        private val run = StringBuilder()

        var bold = false
        var italic = false
        var underline = false
        var link: String? = null

        fun append(text: String) {
            run.append(text)
        }

        fun flushRun() {
            if (run.isEmpty()) return
            val start = builder.length
            builder.append(run.toString())
            val end = builder.length
            if (bold) builder.addStyle(SpanStyle(fontWeight = FontWeight.Bold), start, end)
            if (italic) builder.addStyle(SpanStyle(fontStyle = FontStyle.Italic), start, end)
            if (underline) {
                builder.addStyle(SpanStyle(textDecoration = TextDecoration.Underline), start, end)
            }
            link?.let { builder.addLink(LinkAnnotation.Url(it), start, end) }
            run.setLength(0)
        }

        fun toAnnotatedString(): AnnotatedString? {
            flushRun()
            if (builder.length == 0) return null
            val full = builder.toAnnotatedString()
            val leading = full.text.takeWhile { it.isWhitespace() }.length
            val trailing = full.text.takeLastWhile { it.isWhitespace() }.length
            if (leading + trailing >= full.text.length) return null
            return full.subSequence(leading, full.text.length - trailing)
        }
    }
}
