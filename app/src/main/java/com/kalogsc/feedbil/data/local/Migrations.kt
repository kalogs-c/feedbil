package com.kalogsc.feedbil.data.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Feeds now remember when they were last fetched successfully. Existing feeds
 * start with `NULL`, which means "never fetched"; the first refresh fills it.
 */
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE feeds ADD COLUMN lastFetchedAt INTEGER")
    }
}
