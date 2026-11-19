package com.kalogsc.feedbil.data.local

import android.content.Context
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ArticleStorageTest {

    private lateinit var context: Context
    private lateinit var database: FeedbilDatabase
    private lateinit var feedDao: FeedDao
    private lateinit var articleDao: ArticleDao

    @Before
    fun setUp() {
        context = testContext()
        database = createInMemoryDatabase(context)
        feedDao = database.feedDao()
        articleDao = database.articleDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    private suspend fun createFeed(url: String = "https://example.com/rss"): Long =
        feedDao.insert(feedEntity(url = url))

    @Test
    fun `articles are associated with the feed they belong to`() = runBlocking {
        val first = createFeed("https://first.example.com/rss")
        val second = createFeed("https://second.example.com/rss")
        articleDao.insertAll(
            listOf(
                articleEntity(feedId = first, link = "https://first.example.com/1", publishedAt = 10),
                articleEntity(feedId = first, link = "https://first.example.com/2", publishedAt = 20),
                articleEntity(feedId = second, link = "https://second.example.com/1", publishedAt = 30),
            ),
        )

        assertEquals(listOf(20L, 10L), articleDao.getByFeed(first).map { it.publishedAt })
        assertEquals(listOf(30L), articleDao.getByFeed(second).map { it.publishedAt })
    }

    @Test
    fun `article fields supplied by the feed are stored unchanged`() = runBlocking {
        val feedId = createFeed()
        articleDao.insertAll(
            listOf(
                articleEntity(
                    feedId = feedId,
                    link = "https://example.com/post",
                    publishedAt = 1_700_000_000_000,
                    guid = "guid-1",
                ),
            ),
        )

        val stored = articleDao.getByFeed(feedId).single()

        assertEquals("guid-1", stored.guid)
        assertEquals("https://example.com/post", stored.link)
        assertEquals("Title for https://example.com/post", stored.title)
        assertEquals("Description for https://example.com/post", stored.description)
    }

    @Test
    fun `duplicate link in the same feed is ignored`() = runBlocking {
        val feedId = createFeed()

        val rowIds = articleDao.insertAll(
            listOf(
                articleEntity(feedId = feedId, link = "https://example.com/same", publishedAt = 10),
                articleEntity(feedId = feedId, link = "https://example.com/same", publishedAt = 20),
            ),
        )

        assertEquals(listOf(true, false), rowIds.map { it != -1L })
        assertEquals(1, articleDao.countByFeed(feedId))
        assertEquals(10L, articleDao.getByFeed(feedId).single().publishedAt)
    }

    @Test
    fun `duplicate guid in the same feed is ignored even with a new link`() = runBlocking {
        val feedId = createFeed()

        val rowIds = articleDao.insertAll(
            listOf(
                articleEntity(feedId = feedId, link = "https://example.com/old-url", publishedAt = 10, guid = "guid-1"),
                articleEntity(feedId = feedId, link = "https://example.com/new-url", publishedAt = 20, guid = "guid-1"),
            ),
        )

        assertEquals(listOf(true, false), rowIds.map { it != -1L })
        assertEquals(1, articleDao.countByFeed(feedId))
    }

    @Test
    fun `same link with a different guid merges into one article`() = runBlocking {
        val feedId = createFeed()

        articleDao.insertAll(
            listOf(
                articleEntity(feedId = feedId, link = "https://example.com/shared", publishedAt = 10, guid = "a"),
                articleEntity(feedId = feedId, link = "https://example.com/shared", publishedAt = 20, guid = "b"),
            ),
        )

        assertEquals(1, articleDao.countByFeed(feedId))
    }

    @Test
    fun `the same link in two different feeds is stored twice`() = runBlocking {
        val first = createFeed("https://first.example.com/rss")
        val second = createFeed("https://second.example.com/rss")
        val link = "https://example.com/also-syndicated"

        articleDao.insertAll(
            listOf(
                articleEntity(feedId = first, link = link, publishedAt = 10),
                articleEntity(feedId = second, link = link, publishedAt = 10),
            ),
        )

        assertEquals(1, articleDao.countByFeed(first))
        assertEquals(1, articleDao.countByFeed(second))
    }

    @Test
    fun `articles without a guid are all stored despite the null guid column`() = runBlocking {
        val feedId = createFeed()

        articleDao.insertAll(
            listOf(
                articleEntity(feedId = feedId, link = "https://example.com/1", publishedAt = 10),
                articleEntity(feedId = feedId, link = "https://example.com/2", publishedAt = 20),
                articleEntity(feedId = feedId, link = "https://example.com/3", publishedAt = 30),
            ),
        )

        assertEquals(3, articleDao.countByFeed(feedId))
    }

    @Test
    fun `expired articles are deleted per feed only`() = runBlocking {
        val first = createFeed("https://first.example.com/rss")
        val second = createFeed("https://second.example.com/rss")
        articleDao.insertAll(
            listOf(
                articleEntity(feedId = first, link = "https://first.example.com/old", publishedAt = 100),
                articleEntity(feedId = first, link = "https://first.example.com/new", publishedAt = 500),
                articleEntity(feedId = second, link = "https://second.example.com/old", publishedAt = 100),
            ),
        )

        val deleted = articleDao.deleteExpired(feedId = first, cutoffEpochMillis = 200)

        assertEquals(1, deleted)
        assertEquals(listOf(500L), articleDao.getByFeed(first).map { it.publishedAt })
        assertEquals(1, articleDao.countByFeed(second))
    }

    @Test
    fun `articles exactly at the cutoff are retained`() = runBlocking {
        val feedId = createFeed()
        articleDao.insertAll(
            listOf(
                articleEntity(feedId = feedId, link = "https://example.com/boundary", publishedAt = 200),
                articleEntity(feedId = feedId, link = "https://example.com/older", publishedAt = 199),
            ),
        )

        articleDao.deleteExpired(feedId = feedId, cutoffEpochMillis = 200)

        val remaining = articleDao.getByFeed(feedId)
        assertEquals(1, remaining.size)
        assertEquals(200L, remaining.single().publishedAt)
    }

    @Test
    fun `trimming keeps the newest hundred articles`() = runBlocking {
        val feedId = createFeed()
        articleDao.insertAll(
            (1..105).map { index ->
                articleEntity(feedId = feedId, link = "https://example.com/$index", publishedAt = index.toLong())
            },
        )

        val deleted = articleDao.trimToLimit(feedId = feedId, maxArticles = 100)

        assertEquals(5, deleted)
        val remaining = articleDao.getByFeed(feedId)
        assertEquals(100, remaining.size)
        assertEquals(105L, remaining.first().publishedAt)
        assertEquals(6L, remaining.last().publishedAt)
    }

    @Test
    fun `trimming keeps unread articles in preference to read ones`() = runBlocking {
        val feedId = createFeed()
        val unreadCount = 30
        articleDao.insertAll(
            (0 until 120).map { index ->
                articleEntity(
                    feedId = feedId,
                    link = "https://example.com/$index",
                    publishedAt = index.toLong(),
                    lastReadAt = if (index < unreadCount) null else 9_000L,
                )
            },
        )

        articleDao.trimToLimit(feedId = feedId, maxArticles = 100)

        val remaining = articleDao.getByFeed(feedId)
        assertEquals(100, remaining.size)
        assertEquals(unreadCount, remaining.count { it.lastReadAt == null })
        assertEquals(70, remaining.count { it.lastReadAt != null })
        assertTrue(remaining.filter { it.lastReadAt != null }.none { it.publishedAt < 50 })
        assertTrue(remaining.filter { it.lastReadAt == null }.all { it.publishedAt < unreadCount })
    }

    @Test
    fun `trimming one feed does not touch another feed`() = runBlocking {
        val trimmed = createFeed("https://trimmed.example.com/rss")
        val untouched = createFeed("https://untouched.example.com/rss")
        articleDao.insertAll(
            (1..101).map { articleEntity(feedId = trimmed, link = "https://trimmed.example.com/$it", publishedAt = it.toLong()) } +
                (1..101).map { articleEntity(feedId = untouched, link = "https://untouched.example.com/$it", publishedAt = it.toLong()) },
        )

        articleDao.trimToLimit(feedId = trimmed, maxArticles = 100)

        assertEquals(100, articleDao.countByFeed(trimmed))
        assertEquals(101, articleDao.countByFeed(untouched))
    }

    @Test
    fun `marking an article read records the timestamp`() = runBlocking {
        val feedId = createFeed()
        val id = articleDao.insertAll(
            listOf(articleEntity(feedId = feedId, link = "https://example.com/1", publishedAt = 10)),
        ).single()

        articleDao.markRead(id = id, readAtEpochMillis = 4_242L)

        val stored = articleDao.findById(id)
        assertNotNull(stored)
        assertEquals(4_242L, stored?.lastReadAt)
    }
}
