package com.kalogsc.feedbil.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.kalogsc.feedbil.data.local.entity.ArticleEntity

@Dao
interface ArticleDao {

    /**
     * Inserts the batch, skipping rows that collide with the unique
     * `(feedId, link)` or `(feedId, guid)` indexes. Returns the generated row
     * ids, with `-1` for every skipped duplicate.
     */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(articles: List<ArticleEntity>): List<Long>

    @Query("SELECT * FROM articles WHERE feedId = :feedId ORDER BY publishedAt DESC, id DESC")
    suspend fun getByFeed(feedId: Long): List<ArticleEntity>

    @Query("SELECT * FROM articles WHERE id = :id")
    suspend fun findById(id: Long): ArticleEntity?

    @Query("SELECT COUNT(*) FROM articles WHERE feedId = :feedId")
    suspend fun countByFeed(feedId: Long): Int

    @Query("SELECT DISTINCT feedId FROM articles")
    suspend fun feedIdsWithArticles(): List<Long>

    @Query("DELETE FROM articles WHERE feedId = :feedId AND publishedAt < :cutoffEpochMillis")
    suspend fun deleteExpired(feedId: Long, cutoffEpochMillis: Long): Int

    @Query("DELETE FROM articles WHERE publishedAt < :cutoffEpochMillis")
    suspend fun deleteExpiredEverywhere(cutoffEpochMillis: Long): Int

    /**
     * Deletes everything beyond the newest [maxArticles] of a feed, keeping
     * unread articles in preference to read ones. An undated feed that keeps
     * publishing cannot push unread items out with items already read.
     */
    @Query(
        """
        DELETE FROM articles
        WHERE feedId = :feedId
          AND id NOT IN (
              SELECT id
              FROM articles
              WHERE feedId = :feedId
              ORDER BY CASE WHEN lastReadAt IS NULL THEN 0 ELSE 1 END ASC,
                       publishedAt DESC,
                       id DESC
              LIMIT :maxArticles
          )
        """,
    )
    suspend fun trimToLimit(feedId: Long, maxArticles: Int): Int

    @Query("UPDATE articles SET lastReadAt = :readAtEpochMillis WHERE id = :id")
    suspend fun markRead(id: Long, readAtEpochMillis: Long)
}
