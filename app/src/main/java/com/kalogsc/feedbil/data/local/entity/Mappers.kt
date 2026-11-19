package com.kalogsc.feedbil.data.local.entity

import com.kalogsc.feedbil.domain.model.Article
import com.kalogsc.feedbil.domain.model.Feed

fun FeedEntity.toDomain(): Feed = Feed(
    id = id,
    name = name,
    url = url,
    addedAt = addedAt,
)

fun Feed.toEntity(): FeedEntity = FeedEntity(
    id = id,
    name = name,
    url = url,
    addedAt = addedAt,
)

fun ArticleEntity.toDomain(): Article = Article(
    id = id,
    feedId = feedId,
    guid = guid,
    link = link,
    title = title,
    description = description,
    contentHtml = contentHtml,
    author = author,
    publishedAt = publishedAt,
    lastReadAt = lastReadAt,
)

fun Article.toEntity(): ArticleEntity = ArticleEntity(
    id = id,
    feedId = feedId,
    guid = guid,
    link = link,
    title = title,
    description = description,
    contentHtml = contentHtml,
    author = author,
    publishedAt = publishedAt,
    lastReadAt = lastReadAt,
)
