package com.kalogsc.feedbil.data.remote

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FeedArticleMapperTest {

    private val feedId = 7L
    private val observedAt = 1_900_000_000_000L

    @Test
    fun `maps the fields supplied by the feed`() {
        val articles = FeedArticleMapper.map(
            feedId = feedId,
            items = listOf(
                rssItem(
                    guid = "post-1",
                    title = "First post",
                    author = "Ada",
                    link = "https://example.com/first",
                    pubDate = "Tue, 17 Nov 2026 09:00:00 GMT",
                    description = "Hello there.",
                    content = "<p>Hello <b>there</b>.</p>",
                ),
            ),
            observedAtEpochMillis = observedAt,
        )

        val article = articles.single()
        assertEquals(feedId, article.feedId)
        assertEquals("post-1", article.guid)
        assertEquals("First post", article.title)
        assertEquals("Ada", article.author)
        assertEquals("https://example.com/first", article.link)
        assertEquals("Hello there.", article.description)
        assertEquals("<p>Hello <b>there</b>.</p>", article.contentHtml)
        assertEquals(samplePublishedAt, article.publishedAt)
        assertFalse(article.isRead)
    }

    @Test
    fun `trims values and turns blanks into null`() {
        val article = FeedArticleMapper.map(
            feedId = feedId,
            items = listOf(
                rssItem(
                    guid = "  post-1  ",
                    title = "   ",
                    author = "",
                    link = "  https://example.com/first  ",
                    pubDate = "  ",
                    description = "  Description  ",
                    content = "  ",
                ),
            ),
            observedAtEpochMillis = observedAt,
        ).single()

        assertEquals("post-1", article.guid)
        assertNull(article.title)
        assertNull(article.author)
        assertEquals("https://example.com/first", article.link)
        assertEquals("Description", article.description)
        assertNull(article.contentHtml)
        assertEquals(observedAt, article.publishedAt)
    }

    @Test
    fun `an entry without a link is skipped`() {
        val articles = FeedArticleMapper.map(
            feedId = feedId,
            items = listOf(
                rssItem(title = "No link", pubDate = "Tue, 17 Nov 2026 09:00:00 GMT"),
                rssItem(title = "Blank link", link = "   "),
                rssItem(title = "Usable", link = "https://example.com/usable"),
            ),
            observedAtEpochMillis = observedAt,
        )

        assertEquals(listOf("https://example.com/usable"), articles.map { it.link })
    }

    @Test
    fun `a missing or invalid date uses the observation time`() {
        val articles = FeedArticleMapper.map(
            feedId = feedId,
            items = listOf(
                rssItem(link = "https://example.com/undated"),
                rssItem(link = "https://example.com/invalid", pubDate = "last Tuesday"),
            ),
            observedAtEpochMillis = observedAt,
        )

        assertTrue(articles.all { it.publishedAt == observedAt })
    }

    @Test
    fun `a missing guid stays null`() {
        val article = FeedArticleMapper.map(
            feedId = feedId,
            items = listOf(rssItem(link = "https://example.com/first")),
            observedAtEpochMillis = observedAt,
        ).single()

        assertNull(article.guid)
    }

    @Test
    fun `an empty batch maps to no articles`() {
        assertTrue(FeedArticleMapper.map(feedId, emptyList(), observedAt).isEmpty())
    }
}
