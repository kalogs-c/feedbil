package com.kalogsc.feedbil.ai

import com.kalogsc.feedbil.domain.model.Article
import com.kalogsc.feedbil.ui.articles.ArticleBlock
import com.kalogsc.feedbil.ui.articles.ArticleContentFormatter

/** Article text prepared for the prompt. Null means there is nothing to summarize. */
data class PreparedSummaryInput(
    val title: String?,
    val text: String,
)

/**
 * Extracts the text that will be summarized. It reuses the M5 feed-content
 * sanitizer so scripts, styles and unsafe markup are handled exactly as they
 * are on screen: feed text is untrusted input, including any instructions in
 * it. The whole available text is kept — there is no truncation in the MVP.
 */
object SummaryInputBuilder {

    fun prepare(article: Article): PreparedSummaryInput? {
        val html = article.contentHtml?.takeIf { it.isNotBlank() }
            ?: article.description?.takeIf { it.isNotBlank() }
            ?: return null
        // The M5 formatter renders comments as plain text; strip them here so a
        // hidden instruction in a feed cannot reach the model.
        val text = ArticleContentFormatter.parse(html.replace(HTML_COMMENT, ""))
            .filterIsInstance<ArticleBlock.Paragraph>()
            .joinToString("\n\n") { it.text.text }
            .trim()
            .takeIf { it.isNotEmpty() }
            ?: return null
        val title = article.title?.trim()?.takeIf { it.isNotEmpty() }
        return PreparedSummaryInput(title = title, text = text)
    }

    private val HTML_COMMENT = Regex("(?s)<!--.*?-->")
}
