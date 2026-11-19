package com.kalogsc.feedbil.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.kalogsc.feedbil.data.local.entity.ArticleEntity
import com.kalogsc.feedbil.data.local.entity.FeedEntity

/**
 * Local-only database. There is intentionally no destructive fallback: a schema
 * change must ship a migration, and exported schemas live in `app/schemas`.
 */
@Database(
    entities = [FeedEntity::class, ArticleEntity::class],
    version = 1,
    exportSchema = true,
)
abstract class FeedbilDatabase : RoomDatabase() {
    abstract fun feedDao(): FeedDao

    abstract fun articleDao(): ArticleDao
}
