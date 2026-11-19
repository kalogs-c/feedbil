package com.kalogsc.feedbil

import android.content.Context
import androidx.room.Room
import com.kalogsc.feedbil.data.ArticleRepository
import com.kalogsc.feedbil.data.FeedRefreshService
import com.kalogsc.feedbil.data.FeedRepository
import com.kalogsc.feedbil.data.local.FeedbilDatabase
import com.kalogsc.feedbil.data.local.MIGRATION_1_2
import com.kalogsc.feedbil.data.remote.FeedFetcher
import com.kalogsc.feedbil.data.remote.FeedHttp
import com.prof18.rssparser.RssParserBuilder

/**
 * Manual dependency container (see `DECISIONS.md`). One instance is created by
 * [FeedbilApplication] and used for the lifetime of the process.
 */
class AppContainer(
    context: Context,
    clock: () -> Long = System::currentTimeMillis,
) {
    private val database: FeedbilDatabase = Room
        .databaseBuilder(context.applicationContext, FeedbilDatabase::class.java, DATABASE_NAME)
        .addMigrations(MIGRATION_1_2)
        .build()

    val feedRepository = FeedRepository(database.feedDao(), clock)

    val articleRepository = ArticleRepository(database, clock)

    private val httpClient = FeedHttp.newClient()

    val feedFetcher = FeedFetcher(RssParserBuilder(callFactory = httpClient).build())

    /**
     * Trigger-agnostic refresh unit. The article list calls it today; a future
     * background worker can call the same instance without touching UI code.
     */
    val feedRefreshService = FeedRefreshService(
        feedRepository = feedRepository,
        articleRepository = articleRepository,
        fetchFeed = feedFetcher::fetch,
        clock = clock,
    )

    private companion object {
        const val DATABASE_NAME = "feedbil.db"
    }
}
