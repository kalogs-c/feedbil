package com.kalogsc.feedbil.domain.model

/**
 * One row of the combined article list: the article and the current name of
 * the feed it came from. The name is read from storage on every query, so
 * renaming a feed updates the list without rewriting articles.
 */
data class ArticleListItem(
    val article: Article,
    val feedName: String,
)
