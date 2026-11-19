package com.kalogsc.feedbil.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "feeds",
    indices = [Index(value = ["url"], unique = true)],
)
data class FeedEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val name: String,
    val url: String,
    val addedAt: Long,
)

/**
 * Stored article. Duplicate detection is enforced by two unique indexes:
 * `(feedId, link)` and `(feedId, guid)`. SQLite treats `NULL` values as distinct
 * in unique indexes, so any number of articles without a guid can coexist within
 * the same feed.
 */
@Entity(
    tableName = "articles",
    foreignKeys = [
        ForeignKey(
            entity = FeedEntity::class,
            parentColumns = ["id"],
            childColumns = ["feedId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["feedId", "link"], unique = true),
        Index(value = ["feedId", "guid"], unique = true),
        Index(value = ["feedId", "publishedAt"]),
    ],
)
data class ArticleEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val feedId: Long,
    val guid: String?,
    val link: String,
    val title: String?,
    val description: String?,
    val contentHtml: String?,
    val author: String?,
    val publishedAt: Long,
    val lastReadAt: Long?,
)
