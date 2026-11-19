package com.kalogsc.feedbil.data

import android.database.sqlite.SQLiteException
import com.kalogsc.feedbil.data.remote.FeedArticleMapper
import com.kalogsc.feedbil.data.remote.FeedFetchResult
import com.kalogsc.feedbil.domain.model.Feed
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext

/** Outcome of refreshing every stored feed once. */
data class RefreshSummary(
    val feedCount: Int,
    val newArticleCount: Int,
    val failures: List<FeedRefreshFailure>,
) {
    val hasFailures: Boolean get() = failures.isNotEmpty()
}

/** One feed that could not be refreshed, kept for the UI's details list. */
data class FeedRefreshFailure(
    val feedId: Long,
    val feedName: String,
    val reason: FeedRefreshError,
)

sealed interface FeedRefreshError {
    data class Fetch(val result: FeedFetchResult) : FeedRefreshError
    data object Storage : FeedRefreshError
    data object Unknown : FeedRefreshError
}

private data class FeedRefreshOutcome(
    val newArticleCount: Int,
    val failure: FeedRefreshFailure?,
)

/**
 * Fetches every stored feed, stores new articles, applies retention, and records
 * the successful fetch time. This unit has no Android UI dependencies: the
 * article list calls it today, and a future background worker can call the same
 * method without changing storage or refresh code.
 *
 * A feed's failure is returned as a value and never stops other feeds. A process
 * level mutex prevents two triggers (for example, the automatic launch refresh
 * and a manual tap) from overlapping.
 */
class FeedRefreshService(
    private val feedRepository: FeedRepository,
    private val articleRepository: ArticleRepository,
    private val fetchFeed: suspend (String) -> FeedFetchResult,
    private val clock: () -> Long = System::currentTimeMillis,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val maxConcurrentFetches: Int = 4,
) {
    private val refreshMutex = Mutex()

    suspend fun refreshAll(): RefreshSummary = refreshMutex.withLock {
        val feeds = feedRepository.getAll()
        if (feeds.isEmpty()) {
            return@withLock RefreshSummary(
                feedCount = 0,
                newArticleCount = 0,
                failures = emptyList(),
            )
        }

        val semaphore = Semaphore(maxConcurrentFetches.coerceAtLeast(1))
        val outcomes = coroutineScope {
            feeds.map { feed ->
                async { semaphore.withPermit { refreshOne(feed) } }
            }.awaitAll()
        }

        RefreshSummary(
            feedCount = feeds.size,
            newArticleCount = outcomes.sumOf { it.newArticleCount },
            failures = outcomes.mapNotNull { it.failure },
        )
    }

    private suspend fun refreshOne(feed: Feed): FeedRefreshOutcome = withContext(ioDispatcher) {
        try {
            when (val result = fetchFeed(feed.url)) {
                is FeedFetchResult.Success -> {
                    val observedAt = clock()
                    val articles = FeedArticleMapper.map(feed.id, result.items, observedAt)
                    val newArticles = articleRepository.store(feed.id, articles)
                    markFetched(feed.id, observedAt)
                    FeedRefreshOutcome(newArticleCount = newArticles, failure = null)
                }

                else -> FeedRefreshOutcome(
                    newArticleCount = 0,
                    failure = FeedRefreshFailure(
                        feedId = feed.id,
                        feedName = feed.name,
                        reason = FeedRefreshError.Fetch(result),
                    ),
                )
            }
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (error: Exception) {
            FeedRefreshOutcome(
                newArticleCount = 0,
                failure = FeedRefreshFailure(
                    feedId = feed.id,
                    feedName = feed.name,
                    reason = storageOrUnknown(error),
                ),
            )
        }
    }

    private suspend fun markFetched(feedId: Long, fetchedAtEpochMillis: Long) {
        try {
            feedRepository.markFetched(feedId, fetchedAtEpochMillis)
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (_: Exception) {
            // The articles are stored; a missing timestamp is harmless and the
            // next successful refresh will fill it in.
        }
    }

    private fun storageOrUnknown(error: Exception): FeedRefreshError =
        if (error is SQLiteException) FeedRefreshError.Storage else FeedRefreshError.Unknown
}
