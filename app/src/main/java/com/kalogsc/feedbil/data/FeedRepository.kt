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

    suspend fun delete(id: Long) = feedDao.deleteById(id)

    suspend fun count(): Int = feedDao.count()
}
