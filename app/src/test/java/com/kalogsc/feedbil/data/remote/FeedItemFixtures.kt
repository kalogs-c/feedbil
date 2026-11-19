package com.kalogsc.feedbil.data.remote

import com.prof18.rssparser.model.RssItem
import java.time.Instant

/** Builds parser items without touching XML, for deterministic mapping tests. */
fun rssItem(
    guid: String? = null,
    title: String? = null,
    author: String? = null,
    link: String? = null,
    pubDate: String? = null,
    description: String? = null,
    content: String? = null,
): RssItem = RssItem(
    guid = guid,
    title = title,
    author = author,
    link = link,
    pubDate = pubDate,
    description = description,
    content = content,
    image = null,
    audio = null,
    video = null,
    sourceName = null,
    sourceUrl = null,
    categories = emptyList(),
    itunesItemData = null,
    commentsUrl = null,
    youtubeItemData = null,
    rawEnclosure = null,
)

val samplePublishedAt: Long = Instant.parse("2026-11-17T09:00:00Z").toEpochMilli()
