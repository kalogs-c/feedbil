package com.kalogsc.feedbil

import android.content.Context
import androidx.room.Room
import com.kalogsc.feedbil.ai.LiteRtArticleSummarizer
import com.kalogsc.feedbil.ai.ModelManager
import com.kalogsc.feedbil.data.ArticleRepository
import com.kalogsc.feedbil.data.FeedRefreshService
import com.kalogsc.feedbil.data.FeedRepository
import com.kalogsc.feedbil.data.local.FeedbilDatabase
import com.kalogsc.feedbil.data.local.MIGRATION_1_2
import com.kalogsc.feedbil.data.remote.FeedFetcher
import com.kalogsc.feedbil.data.remote.FeedHttp
import com.prof18.rssparser.RssParserBuilder
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

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

    /**
     * Model downloads need much longer timeouts than feed requests. The native
     * engine is created lazily by the summarizer, not at app startup.
     */
    private val modelHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(5, TimeUnit.MINUTES)
        .writeTimeout(5, TimeUnit.MINUTES)
        .callTimeout(0, TimeUnit.MILLISECONDS)
        .addInterceptor { chain ->
            val request = chain.request().newBuilder()
                .header("User-Agent", FeedHttp.USER_AGENT)
                .build()
            chain.proceed(request)
        }
        .build()

    val modelManager = ModelManager(context.applicationContext.filesDir, modelHttpClient)

    val articleSummarizer = LiteRtArticleSummarizer(modelManager.modelPath)

    private companion object {
        const val DATABASE_NAME = "feedbil.db"
    }
}
