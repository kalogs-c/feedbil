package com.kalogsc.feedbil.data.local

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File

/**
 * Builds a real version 1 database, inserts data, and opens it with Room plus
 * the hand-written migration. Room validates the resulting schema.
 */
@RunWith(RobolectricTestRunner::class)
class DatabaseMigrationTest {

    private companion object {
        const val DATABASE_NAME = "migration-test.db"
    }

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = testContext()
        databaseFile().delete()
        databaseFile().parentFile?.mkdirs()
    }

    @After
    fun tearDown() {
        databaseFile().delete()
    }

    private fun databaseFile(): File = context.getDatabasePath(DATABASE_NAME)

    @Test
    fun `version 1 data survives the migration and gains a null fetch time`() = runBlocking {
        val legacy = SQLiteDatabase.openOrCreateDatabase(databaseFile(), null)
        legacy.execSQL(
            "CREATE TABLE IF NOT EXISTS feeds (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "name TEXT NOT NULL, url TEXT NOT NULL, addedAt INTEGER NOT NULL)",
        )
        legacy.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_feeds_url ON feeds (url)")
        legacy.execSQL(
            "CREATE TABLE IF NOT EXISTS articles (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "feedId INTEGER NOT NULL, guid TEXT, link TEXT NOT NULL, title TEXT, " +
                "description TEXT, contentHtml TEXT, author TEXT, publishedAt INTEGER NOT NULL, " +
                "lastReadAt INTEGER, " +
                "FOREIGN KEY(feedId) REFERENCES feeds(id) ON UPDATE NO ACTION ON DELETE CASCADE)",
        )
        legacy.execSQL(
            "CREATE UNIQUE INDEX IF NOT EXISTS index_articles_feedId_link ON articles (feedId, link)",
        )
        legacy.execSQL(
            "CREATE UNIQUE INDEX IF NOT EXISTS index_articles_feedId_guid ON articles (feedId, guid)",
        )
        legacy.execSQL(
            "CREATE INDEX IF NOT EXISTS index_articles_feedId_publishedAt " +
                "ON articles (feedId, publishedAt)",
        )
        legacy.execSQL(
            "INSERT INTO feeds (id, name, url, addedAt) " +
                "VALUES (1, 'Kotlin Blog', 'https://blog.example.com/rss', 1000)",
        )
        legacy.execSQL(
            "INSERT INTO articles " +
                "(id, feedId, guid, link, title, description, contentHtml, author, publishedAt, lastReadAt) " +
                "VALUES (1, 1, NULL, 'https://blog.example.com/post', 'Post', NULL, NULL, NULL, 2000, NULL)",
        )
        legacy.version = 1
        legacy.close()

        val migrated = Room.databaseBuilder(context, FeedbilDatabase::class.java, DATABASE_NAME)
            .addMigrations(MIGRATION_1_2)
            .allowMainThreadQueries()
            .build()
        try {
            val feed = migrated.feedDao().findById(1)
            assertEquals("Kotlin Blog", feed?.name)
            assertNull(feed?.lastFetchedAt)
            assertEquals(1, migrated.articleDao().countByFeed(1))
            assertEquals("https://blog.example.com/post", migrated.articleDao().getByFeed(1).single().link)

            migrated.feedDao().markFetched(id = 1, fetchedAtEpochMillis = 5_000)
            assertEquals(5_000L, migrated.feedDao().findById(1)?.lastFetchedAt)
        } finally {
            migrated.close()
        }
    }
}
