package com.kalogsc.feedbil.data.local

import android.content.Context
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class FeedStorageTest {

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

    @Test
    fun `feed is read back with its user-provided name and url`() = runBlocking {
        val id = feedDao.insert(feedEntity(name = "Kotlin Blog", url = "https://blog.example.com/rss"))

        val stored = feedDao.findById(id)

        assertEquals("Kotlin Blog", stored?.name)
        assertEquals("https://blog.example.com/rss", stored?.url)
    }

    @Test
    fun `feeds are returned in subscription order`() = runBlocking {
        feedDao.insert(feedEntity(url = "https://b.example.com/rss", name = "B", addedAt = 200))
        feedDao.insert(feedEntity(url = "https://a.example.com/rss", name = "A", addedAt = 100))

        assertEquals(listOf("A", "B"), feedDao.getAll().map { it.name })
    }

    @Test(expected = android.database.sqlite.SQLiteConstraintException::class)
    fun `the same feed url cannot be stored twice`(): Unit = runBlocking {
        feedDao.insert(feedEntity(url = "https://example.com/rss"))
        feedDao.insert(feedEntity(url = "https://example.com/rss"))
    }

    @Test
    fun `a successful refresh records its time`() = runBlocking {
        val id = feedDao.insert(feedEntity())

        assertNull(feedDao.findById(id)?.lastFetchedAt)
        feedDao.markFetched(id = id, fetchedAtEpochMillis = 4_242L)

        assertEquals(4_242L, feedDao.findById(id)?.lastFetchedAt)
    }

    @Test
    fun `deleting a feed deletes its articles but keeps other feeds' articles`() = runBlocking {
        val removedFeed = feedDao.insert(feedEntity(url = "https://removed.example.com/rss"))
        val keptFeed = feedDao.insert(feedEntity(url = "https://kept.example.com/rss"))
        articleDao.insertAll(
            listOf(
                articleEntity(feedId = removedFeed, link = "https://removed.example.com/1", publishedAt = 1_000),
                articleEntity(feedId = keptFeed, link = "https://kept.example.com/1", publishedAt = 1_000),
            ),
        )

        feedDao.deleteById(removedFeed)

        assertNull(feedDao.findById(removedFeed))
        assertEquals(0, articleDao.countByFeed(removedFeed))
        assertEquals(1, articleDao.countByFeed(keptFeed))
    }
}
