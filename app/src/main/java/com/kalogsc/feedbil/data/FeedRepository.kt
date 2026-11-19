package com.kalogsc.feedbil.data

import android.database.sqlite.SQLiteConstraintException
import com.kalogsc.feedbil.data.local.FeedDao
import com.kalogsc.feedbil.data.local.entity.toDomain
import com.kalogsc.feedbil.data.local.entity.toEntity
import com.kalogsc.feedbil.domain.model.Feed
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class FeedRepository(
    private val feedDao: FeedDao,
    private val clock: () -> Long = System::currentTimeMillis,
) {

    suspend fun add(name: String, url: String): Feed {
        val feed = Feed(name = name, url = url, addedAt = clock())
        val id = try {
            feedDao.insert(feed.toEntity())
        } catch (conflict: SQLiteConstraintException) {
            throw DuplicateFeedUrlException(url, conflict)
        }
        return feed.copy(id = id)
    }

    suspend fun getAll(): List<Feed> = feedDao.getAll().map { it.toDomain() }

    /** Emits the stored feeds again whenever one is added, edited, or removed. */
    fun observeAll(): Flow<List<Feed>> = feedDao.observeAll().map { feeds ->
        feeds.map { it.toDomain() }
    }

    suspend fun findById(id: Long): Feed? = feedDao.findById(id)?.toDomain()

    /**
     * Renames a feed and/or changes its URL. Returns `false` when the feed no
     * longer exists. A URL already used by another feed throws
     * [DuplicateFeedUrlException]; callers check [isUrlTaken] first so the
     * common case never reaches the constraint.
     */
    suspend fun update(id: Long, name: String, url: String): Boolean = try {
        feedDao.update(id, name, url) > 0
    } catch (conflict: SQLiteConstraintException) {
        throw DuplicateFeedUrlException(url, conflict)
    }

    /**
     * True when [url] belongs to a stored feed, ignoring [exceptFeedId] so an
     * edited feed does not conflict with itself. Unsaved ids are `0`, while
     * stored ids start at `1`, so the default excludes nothing.
     */
    suspend fun isUrlTaken(url: String, exceptFeedId: Long = Feed.UNSAVED_ID): Boolean =
        feedDao.countByUrl(url, exceptFeedId) > 0

    suspend fun delete(id: Long) = feedDao.deleteById(id)

    suspend fun count(): Int = feedDao.count()
}
