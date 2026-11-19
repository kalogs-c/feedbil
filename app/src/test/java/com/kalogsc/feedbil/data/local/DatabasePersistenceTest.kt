package com.kalogsc.feedbil.data.local

import android.content.Context
import androidx.room.Room
import com.kalogsc.feedbil.data.ArticleRepository
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File

/** Verifies that data survives closing and reopening the database file. */
@RunWith(RobolectricTestRunner::class)
class DatabasePersistenceTest {

    private companion object {
        const val DATABASE_NAME = "persistence-test.db"
    }

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = testContext()
        databaseFile().delete()
    }

    @After
    fun tearDown() {
        databaseFile().delete()
    }

    private fun databaseFile(): File = context.getDatabasePath(DATABASE_NAME)

    private fun openDatabase(): FeedbilDatabase =
        Room.databaseBuilder(context, FeedbilDatabase::class.java, DATABASE_NAME)
            .allowMainThreadQueries()
            .build()

    @Test
    fun `feeds and articles are still present after reopening the database`() = runBlocking {
        val first = openDatabase()
        val feed = first.feedDao().insert(feedEntity(name = "Kotlin Blog"))
        first.articleDao().insertAll(
            listOf(articleEntity(feedId = feed, link = "https://example.com/1", publishedAt = 10)),
        )
        first.close()

        assertTrue(databaseFile().exists())

        val reopened = openDatabase()
        try {
            val storedFeed = reopened.feedDao().findById(feed)
            assertEquals("Kotlin Blog", storedFeed?.name)
            assertEquals(1, reopened.articleDao().countByFeed(feed))
            assertEquals("https://example.com/1", reopened.articleDao().getByFeed(feed).single().link)
        } finally {
            reopened.close()
        }
    }

    @Test
    fun `retention applies to data stored before the app was restarted`() = runBlocking {
        val now = 1_800_000_000_000L
        val first = openDatabase()
        val feed = first.feedDao().insert(feedEntity())
        first.articleDao().insertAll(
            listOf(articleEntity(feedId = feed, link = "https://example.com/old", publishedAt = now - 8L * 24 * 60 * 60 * 1000)),
        )
        first.close()

        val reopened = openDatabase()
        try {
            ArticleRepository(reopened) { now }.pruneAll()
            assertEquals(0, reopened.articleDao().countByFeed(feed))
        } finally {
            reopened.close()
        }
    }
}
