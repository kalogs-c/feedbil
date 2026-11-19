package com.kalogsc.feedbil.data

import androidx.room.withTransaction
import com.kalogsc.feedbil.data.local.FeedbilDatabase
import com.kalogsc.feedbil.data.local.entity.toDomain
import com.kalogsc.feedbil.data.local.entity.toEntity
import com.kalogsc.feedbil.domain.RetentionPolicy
import com.kalogsc.feedbil.domain.model.Article

/**
 * Stores articles and enforces retention. Callers never touch the database
 * directly, so the "insert then prune in one transaction" rule cannot be
 * bypassed by accident.
 */
class ArticleRepository(
    private val database: FeedbilDatabase,
    private val clock: () -> Long = System::currentTimeMillis,
) {
    private val articleDao = database.articleDao()

    /**
     * Stores [articles] for [feedId] and applies retention to that feed in the
     * same transaction. Existing rows are left untouched; duplicates are
     * ignored. Returns the number of newly stored articles.
     */
    suspend fun store(feedId: Long, articles: List<Article>): Int {
        require(articles.all { it.feedId == feedId }) {
            "Every article must belong to feed $feedId"
        }
        if (articles.isEmpty()) return 0

        return database.withTransaction {
            val rowIds = articleDao.insertAll(articles.map { it.toEntity() })
            pruneFeed(feedId)
            rowIds.count { it != ROW_ID_REJECTED }
        }
    }

    /**
     * Applies both retention rules to every feed. Called after each refresh and
     * once at app startup so expired articles disappear even when the app was
     * closed for longer than the retention window.
     */
    suspend fun pruneAll() {
        database.withTransaction {
            articleDao.deleteExpiredEverywhere(RetentionPolicy.cutoffEpochMillis(clock()))
            articleDao.feedIdsWithArticles().forEach { feedId ->
                articleDao.trimToLimit(feedId, RetentionPolicy.MAX_ARTICLES_PER_FEED)
            }
        }
    }

    suspend fun getByFeed(feedId: Long): List<Article> =
        articleDao.getByFeed(feedId).map { it.toDomain() }

    suspend fun countByFeed(feedId: Long): Int = articleDao.countByFeed(feedId)

    suspend fun markRead(articleId: Long) = articleDao.markRead(articleId, clock())

    private suspend fun pruneFeed(feedId: Long) {
        articleDao.deleteExpired(feedId, RetentionPolicy.cutoffEpochMillis(clock()))
        articleDao.trimToLimit(feedId, RetentionPolicy.MAX_ARTICLES_PER_FEED)
    }

    private companion object {
        const val ROW_ID_REJECTED = -1L
    }
}
