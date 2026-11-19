package com.kalogsc.feedbil.data.remote

import com.kalogsc.feedbil.domain.ArticleDates
import com.kalogsc.feedbil.domain.FeedDateParser
import com.kalogsc.feedbil.domain.model.Article
import com.prof18.rssparser.model.RssItem

/**
 * Maps parsed feed items to articles. Only fields actually supplied by the feed
 * are kept: blanks become `null`, entries without a link are skipped, and a
 * missing or unparseable date uses the observation time (see `DECISIONS.md`).
 */
object FeedArticleMapper {

    fun map(
        feedId: Long,
        items: List<RssItem>,
        observedAtEpochMillis: Long,
    ): List<Article> = items.mapNotNull { item ->
        val link = item.link.trimmedOrNull() ?: return@mapNotNull null
        Article(
            feedId = feedId,
            guid = item.guid.trimmedOrNull(),
            link = link,
            title = item.title.trimmedOrNull(),
            description = item.description.trimmedOrNull(),
            contentHtml = item.content.trimmedOrNull(),
            author = item.author.trimmedOrNull(),
            publishedAt = ArticleDates.resolvePublishedAt(
                FeedDateParser.parseEpochMillis(item.pubDate),
                observedAtEpochMillis,
            ),
        )
    }

    private fun String?.trimmedOrNull(): String? = this?.trim()?.takeIf { it.isNotEmpty() }
}
