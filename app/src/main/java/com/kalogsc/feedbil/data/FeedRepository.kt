package com.kalogsc.feedbil.data

import com.kalogsc.feedbil.data.local.FeedDao
import com.kalogsc.feedbil.data.local.entity.toDomain
import com.kalogsc.feedbil.data.local.entity.toEntity
import com.kalogsc.feedbil.domain.model.Feed

class FeedRepository(
    private val feedDao: FeedDao,
    private val clock: () -> Long = System::currentTimeMillis,
) {

    suspend fun add(name: String, url: String): Feed {
        val feed = Feed(name = name, url = url, addedAt = clock())
        return feed.copy(id = feedDao.insert(feed.toEntity()))
    }

    suspend fun getAll(): List<Feed> = feedDao.getAll().map { it.toDomain() }

    suspend fun findById(id: Long): Feed? = feedDao.findById(id)?.toDomain()

    /**
     * Renames a feed and/or changes its URL. Returns `false` when the feed no
     * longer exists. A URL already used by another feed still violates the
     * unique index; callers check [isUrlTaken] first and map the conflict to a
     * user-facing error.
     */
    suspend fun update(id: Long, name: String, url: String): Boolean =
        feedDao.update(id, name, url) > 0

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
