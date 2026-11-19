package com.kalogsc.feedbil.data

import com.kalogsc.feedbil.data.local.FeedbilDatabase
import com.kalogsc.feedbil.data.local.articleEntity
import com.kalogsc.feedbil.data.local.createInMemoryDatabase
import com.kalogsc.feedbil.data.local.testContext
import com.kalogsc.feedbil.data.remote.FeedFetchResult
import com.kalogsc.feedbil.data.remote.rssItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.time.Duration

@RunWith(RobolectricTestRunner::class)
class FeedRefreshServiceTest {

    private lateinit var database: FeedbilDatabase
    private lateinit var feedRepository: FeedRepository
    private lateinit var articleRepository: ArticleRepository
    private var currentTime = 1_900_000_000_000L
    private var fetchOutcome: suspend (String) -> FeedFetchResult = {
        error("No fetch outcome configured")
    }

    @Before
    fun setUp() {
        database = createInMemoryDatabase(testContext())
        feedRepository = FeedRepository(database.feedDao()) { currentTime }
        articleRepository = ArticleRepository(database) { currentTime }
    }

    @After
    fun tearDown() {
        database.close()
    }

    private fun service(maxConcurrentFetches: Int = 4): FeedRefreshService =
        FeedRefreshService(
            feedRepository = feedRepository,
            articleRepository = articleRepository,
            fetchFeed = { url -> fetchOutcome(url) },
            clock = { currentTime },
            ioDispatcher = Dispatchers.Unconfined,
            maxConcurrentFetches = maxConcurrentFetches,
        )

    @Test
    fun `successful refresh stores new articles and records the fetch time`() = runBlocking {
        val feed = feedRepository.add("Example", "https://example.com/rss")
        fetchOutcome = {
            FeedFetchResult.Success(
                items = listOf(
                    rssItem(link = "https://example.com/one", title = "One"),
                    rssItem(link = "https://example.com/two", title = "Two"),
                ),
            )
        }

        val summary = service().refreshAll()

        assertEquals(1, summary.feedCount)
        assertEquals(2, summary.newArticleCount)
        assertTrue(summary.failures.isEmpty())
        assertEquals(2, articleRepository.countByFeed(feed.id))
        assertEquals(currentTime, feedRepository.findById(feed.id)?.lastFetchedAt)
    }

    @Test
    fun `a failed feed does not block the others`() = runBlocking {
        val good = feedRepository.add("Good", "https://good.example.com/rss")
        val bad = feedRepository.add("Bad", "https://bad.example.com/rss")
        fetchOutcome = { url ->
            if (url == good.url) {
                FeedFetchResult.Success(
                    items = listOf(rssItem(link = "https://good.example.com/post")),
                )
            } else {
                FeedFetchResult.Network
            }
        }

        val summary = service().refreshAll()

        assertEquals(2, summary.feedCount)
        assertEquals(1, summary.newArticleCount)
        assertEquals(listOf(bad.id), summary.failures.map { it.feedId })
        assertEquals(FeedRefreshError.Fetch(FeedFetchResult.Network), summary.failures.single().reason)
        assertEquals(1, articleRepository.countByFeed(good.id))
        assertEquals(0, articleRepository.countByFeed(bad.id))
        assertEquals(currentTime, feedRepository.findById(good.id)?.lastFetchedAt)
        assertNull(feedRepository.findById(bad.id)?.lastFetchedAt)
    }

    @Test
    fun `refreshing twice does not duplicate entries`() = runBlocking {
        val feed = feedRepository.add("Example", "https://example.com/rss")
        fetchOutcome = {
            FeedFetchResult.Success(
                items = listOf(rssItem(link = "https://example.com/one")),
            )
        }
        val service = service()

        val first = service.refreshAll()
        val second = service.refreshAll()

        assertEquals(1, first.newArticleCount)
        assertEquals(0, second.newArticleCount)
        assertEquals(1, articleRepository.countByFeed(feed.id))
    }

    @Test
    fun `retention is applied after a refresh`() = runBlocking {
        val feed = feedRepository.add("Example", "https://example.com/rss")
        fetchOutcome = {
            FeedFetchResult.Success(
                items = (1..105).map { index ->
                    rssItem(link = "https://example.com/$index")
                },
            )
        }

        val summary = service().refreshAll()

        assertEquals(105, summary.newArticleCount)
        assertEquals(100, articleRepository.countByFeed(feed.id))
    }

    @Test
    fun `retention is applied when a refresh stores no articles`() = runBlocking {
        val feed = feedRepository.add("Example", "https://example.com/rss")
        database.articleDao().insertAll(
            listOf(
                articleEntity(
                    feedId = feed.id,
                    link = "https://example.com/old",
                    publishedAt = currentTime - Duration.ofDays(8).toMillis(),
                ),
                articleEntity(
                    feedId = feed.id,
                    link = "https://example.com/recent",
                    publishedAt = currentTime,
                ),
            ),
        )
        fetchOutcome = { FeedFetchResult.Success(items = emptyList()) }

        val summary = service().refreshAll()

        assertEquals(0, summary.newArticleCount)
        assertEquals(listOf("https://example.com/recent"), articleRepository.getByFeed(feed.id).map { it.link })
        assertEquals(currentTime, feedRepository.findById(feed.id)?.lastFetchedAt)
    }

    @Test
    fun `entries without a usable link are skipped but the feed still counts as refreshed`() =
        runBlocking {
            val feed = feedRepository.add("Example", "https://example.com/rss")
            fetchOutcome = {
                FeedFetchResult.Success(
                    items = listOf(
                        rssItem(title = "No link"),
                        rssItem(title = "Blank link", link = "   "),
                    ),
                )
            }

            val summary = service().refreshAll()

            assertEquals(0, summary.newArticleCount)
            assertEquals(0, articleRepository.countByFeed(feed.id))
            assertEquals(currentTime, feedRepository.findById(feed.id)?.lastFetchedAt)
        }

    @Test
    fun `the concurrency limit is respected`() = runBlocking {
        (1..5).forEach { index ->
            feedRepository.add("Feed $index", "https://feed$index.example.com/rss")
        }
        var active = 0
        var maxActive = 0
        fetchOutcome = { url ->
            active += 1
            maxActive = maxOf(maxActive, active)
            delay(20)
            active -= 1
            FeedFetchResult.Success(
                items = listOf(rssItem(link = "$url/post")),
            )
        }

        service(maxConcurrentFetches = 2).refreshAll()

        assertTrue("Expected at most 2 concurrent fetches, saw $maxActive", maxActive <= 2)
    }

    @Test
    fun `overlapping refresh calls are serialized`() = runBlocking {
        feedRepository.add("Example", "https://example.com/rss")
        var calls = 0
        fetchOutcome = {
            calls += 1
            delay(20)
            FeedFetchResult.Success(items = emptyList())
        }
        val service = service()

        val first = async { service.refreshAll() }
        val second = async { service.refreshAll() }
        awaitAll(first, second)

        assertEquals(2, calls)
    }

    @Test
    fun `no feeds produces an empty summary`() = runBlocking {
        val summary = service().refreshAll()

        assertEquals(0, summary.feedCount)
        assertEquals(0, summary.newArticleCount)
        assertTrue(summary.failures.isEmpty())
    }
}
