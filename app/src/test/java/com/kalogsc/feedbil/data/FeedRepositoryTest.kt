package com.kalogsc.feedbil.data

import com.kalogsc.feedbil.data.local.FeedbilDatabase
import com.kalogsc.feedbil.data.local.createInMemoryDatabase
import com.kalogsc.feedbil.data.local.testContext
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class FeedRepositoryTest {

    private lateinit var database: FeedbilDatabase
    private lateinit var repository: FeedRepository

    @Before
    fun setUp() {
        database = createInMemoryDatabase(testContext())
        repository = FeedRepository(database.feedDao()) { ADDED_AT }
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun `add stores the feed with the current time`() = runBlocking {
        val feed = repository.add("Kotlin Blog", "https://blog.example.com/rss")

        assertEquals(ADDED_AT, feed.addedAt)
        assertEquals(listOf(feed), repository.getAll())
    }

    @Test
    fun `update changes the name and the url`() = runBlocking {
        val feed = repository.add("Old", "https://old.example.com/rss")

        assertTrue(repository.update(feed.id, "New", "https://new.example.com/rss"))

        assertEquals(
            listOf("New" to "https://new.example.com/rss"),
            repository.getAll().map { it.name to it.url },
        )
    }

    @Test
    fun `update reports a feed that no longer exists`() = runBlocking {
        assertFalse(repository.update(id = 99L, name = "New", url = "https://new.example.com/rss"))
    }

    @Test
    fun `a url used by another feed is taken`() = runBlocking {
        repository.add("Example", "https://example.com/rss")

        assertTrue(repository.isUrlTaken("https://example.com/rss"))
        assertFalse(repository.isUrlTaken("https://other.example.com/rss"))
    }

    @Test
    fun `renaming a feed does not conflict with its own url`() = runBlocking {
        val feed = repository.add("Example", "https://example.com/rss")

        assertFalse(repository.isUrlTaken("https://example.com/rss", exceptFeedId = feed.id))
    }

    private companion object {
        const val ADDED_AT = 1_000L
    }
}
