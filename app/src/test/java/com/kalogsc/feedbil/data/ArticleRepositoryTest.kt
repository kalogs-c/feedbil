package com.kalogsc.feedbil.data

import android.content.Context
import com.kalogsc.feedbil.data.local.ArticleDao
import com.kalogsc.feedbil.data.local.FeedDao
import com.kalogsc.feedbil.data.local.FeedbilDatabase
import com.kalogsc.feedbil.data.local.articleEntity
import com.kalogsc.feedbil.data.local.createInMemoryDatabase
import com.kalogsc.feedbil.data.local.feedEntity
import com.kalogsc.feedbil.data.local.testContext
import com.kalogsc.feedbil.domain.model.Article
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.time.Duration

@RunWith(RobolectricTestRunner::class)
class ArticleRepositoryTest {

    private val now = 1_800_000_000_000L
    private var currentTime = now

    private lateinit var context: Context
    private lateinit var database: FeedbilDatabase
    private lateinit var feedDao: FeedDao
    private lateinit var articleDao: ArticleDao
    private lateinit var repository: ArticleRepository

    @Before
    fun setUp() {
        context = testContext()
        database = createInMemoryDatabase(context)
        feedDao = database.feedDao()
        articleDao = database.articleDao()
        repository = ArticleRepository(database) { currentTime }
    }

    @After
    fun tearDown() {
        database.close()
    }

    private suspend fun createFeed(url: String = "https://example.com/rss"): Long =
        feedDao.insert(feedEntity(url = url))

    private fun article(
        feedId: Long,
        link: String,
        publishedAt: Long = now,
        lastReadAt: Long? = null,
    ): Article = Article(
        feedId = feedId,
        guid = null,
        link = link,
        title = "Title",
        description = "Description",
        contentHtml = null,
        author = null,
        publishedAt = publishedAt,
        lastReadAt = lastReadAt,
    )

    @Test
    fun `storing more than a hundred articles keeps the newest hundred`() = runBlocking {
        val feedId = createFeed()
        val minute = Duration.ofMinutes(1).toMillis()

        val inserted = repository.store(
            feedId,
            (1..105).map { index ->
                article(
                    feedId = feedId,
                    link = "https://example.com/$index",
                    publishedAt = now - (105 - index) * minute,
                )
            },
        )

        assertEquals(105, inserted)
        assertEquals(100, repository.countByFeed(feedId))
        val stored = repository.getByFeed(feedId)
        assertEquals(now, stored.first().publishedAt)
        assertEquals(now - 99 * minute, stored.last().publishedAt)
    }

    @Test
    fun `articles expire seven days after their stored publication date`() = runBlocking {
        val feedId = createFeed()
        currentTime = now - Duration.ofDays(8).toMillis()
        repository.store(
            feedId,
            listOf(article(feedId = feedId, link = "https://example.com/old", publishedAt = currentTime)),
        )
        assertEquals(1, repository.countByFeed(feedId))

        currentTime = now
        repository.store(
            feedId,
            listOf(article(feedId = feedId, link = "https://example.com/new", publishedAt = currentTime)),
        )

        assertEquals(listOf("https://example.com/new"), repository.getByFeed(feedId).map { it.link })
    }

    @Test
    fun `storing the same entries twice stores nothing the second time`() = runBlocking {
        val feedId = createFeed()
        val articles = (1..3).map { article(feedId = feedId, link = "https://example.com/$it") }

        assertEquals(3, repository.store(feedId, articles))
        assertEquals(0, repository.store(feedId, articles))
        assertEquals(3, repository.countByFeed(feedId))
    }

    @Test
    fun `storing an empty batch is a no-op`() = runBlocking {
        val feedId = createFeed()

        assertEquals(0, repository.store(feedId, emptyList()))
        assertEquals(0, repository.countByFeed(feedId))
    }

    @Test
    fun `storing an empty batch still applies retention`() = runBlocking {
        val feedId = createFeed()
        val expired = now - Duration.ofDays(8).toMillis()
        articleDao.insertAll(
            listOf(
                articleEntity(feedId = feedId, link = "https://example.com/old", publishedAt = expired),
                articleEntity(feedId = feedId, link = "https://example.com/new", publishedAt = now),
            ),
        )

        assertEquals(0, repository.store(feedId, emptyList()))

        assertEquals(listOf("https://example.com/new"), repository.getByFeed(feedId).map { it.link })
    }

    @Test
    fun `storing articles from another feed is rejected`() = runBlocking {
        val feedId = createFeed()

        try {
            repository.store(feedId, listOf(article(feedId = feedId + 1, link = "https://example.com/1")))
            fail("Expected IllegalArgumentException")
        } catch (expected: IllegalArgumentException) {
            assertEquals(0, repository.countByFeed(feedId))
        }
    }

    @Test
    fun `pruneAll drops expired articles from every feed and keeps recent ones`() = runBlocking {
        val first = createFeed("https://first.example.com/rss")
        val second = createFeed("https://second.example.com/rss")
        val expired = now - Duration.ofDays(8).toMillis()
        articleDao.insertAll(
            listOf(
                articleEntity(feedId = first, link = "https://first.example.com/old", publishedAt = expired),
                articleEntity(feedId = first, link = "https://first.example.com/new", publishedAt = now),
                articleEntity(feedId = second, link = "https://second.example.com/old", publishedAt = expired),
            ),
        )

        repository.pruneAll()

        assertEquals(listOf("https://first.example.com/new"), repository.getByFeed(first).map { it.link })
        assertTrue(repository.getByFeed(second).isEmpty())
    }

    @Test
    fun `marking an article read exposes it as read`() = runBlocking {
        val feedId = createFeed()
        repository.store(feedId, listOf(article(feedId = feedId, link = "https://example.com/1")))
        val stored = repository.getByFeed(feedId).single()
        assertFalse(stored.isRead)

        repository.markRead(stored.id)

        val read = repository.getByFeed(feedId).single()
        assertTrue(read.isRead)
        assertEquals(now, read.lastReadAt)
    }
}
