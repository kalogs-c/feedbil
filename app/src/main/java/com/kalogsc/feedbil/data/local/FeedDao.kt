package com.kalogsc.feedbil.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.kalogsc.feedbil.data.local.entity.FeedEntity

@Dao
interface FeedDao {

    @Insert
    suspend fun insert(feed: FeedEntity): Long

    @Query("SELECT * FROM feeds ORDER BY addedAt ASC, id ASC")
    suspend fun getAll(): List<FeedEntity>

    @Query("SELECT * FROM feeds WHERE id = :id")
    suspend fun findById(id: Long): FeedEntity?

    @Query("DELETE FROM feeds WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT COUNT(*) FROM feeds")
    suspend fun count(): Int
}
