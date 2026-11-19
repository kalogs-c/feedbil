package com.kalogsc.feedbil.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.kalogsc.feedbil.data.local.entity.FeedEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FeedDao {

    @Insert
    suspend fun insert(feed: FeedEntity): Long

    @Query("SELECT * FROM feeds ORDER BY addedAt ASC, id ASC")
    suspend fun getAll(): List<FeedEntity>

    /** Same order as [getAll], but re-emitted whenever the feeds table changes. */
    @Query("SELECT * FROM feeds ORDER BY addedAt ASC, id ASC")
    fun observeAll(): Flow<List<FeedEntity>>

    @Query("SELECT * FROM feeds WHERE id = :id")
    suspend fun findById(id: Long): FeedEntity?

    @Query("UPDATE feeds SET name = :name, url = :url WHERE id = :id")
    suspend fun update(id: Long, name: String, url: String): Int

    @Query("SELECT COUNT(*) FROM feeds WHERE url = :url AND id != :excludedId")
    suspend fun countByUrl(url: String, excludedId: Long): Int

    @Query("DELETE FROM feeds WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT COUNT(*) FROM feeds")
    suspend fun count(): Int
}
