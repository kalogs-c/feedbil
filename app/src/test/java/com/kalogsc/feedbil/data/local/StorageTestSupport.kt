package com.kalogsc.feedbil.data.local

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.kalogsc.feedbil.data.local.entity.ArticleEntity
import com.kalogsc.feedbil.data.local.entity.FeedEntity

fun testContext(): Context = ApplicationProvider.getApplicationContext()

fun createInMemoryDatabase(context: Context): FeedbilDatabase =
    Room.inMemoryDatabaseBuilder(context, FeedbilDatabase::class.java)
        .allowMainThreadQueries()
        .build()

fun feedEntity(
    url: String = "https://example.com/rss",
    name: String = "Example",
    addedAt: Long = 0L,
): FeedEntity = FeedEntity(name = name, url = url, addedAt = addedAt)

fun articleEntity(
    feedId: Long,
    link: String,
    publishedAt: Long,
    guid: String? = null,
    lastReadAt: Long? = null,
): ArticleEntity = ArticleEntity(
    feedId = feedId,
    guid = guid,
    link = link,
    title = "Title for $link",
    description = "Description for $link",
    contentHtml = null,
    author = null,
    publishedAt = publishedAt,
    lastReadAt = lastReadAt,
)
