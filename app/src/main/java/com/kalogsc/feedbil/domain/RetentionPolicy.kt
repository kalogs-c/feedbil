package com.kalogsc.feedbil.domain

import java.time.Duration

/**
 * Article retention rules, enforced in storage rather than only in the UI.
 *
 * Both rules apply per feed: an article is expired when it is older than
 * [MAX_AGE_DAYS], and a feed never keeps more than [MAX_ARTICLES_PER_FEED]
 * articles even when they are all recent.
 */
object RetentionPolicy {
    /** Maximum age of a retained article, in days. */
    const val MAX_AGE_DAYS = 7L

    /** Maximum number of retained articles per feed. */
    const val MAX_ARTICLES_PER_FEED = 100

    /**
     * Publication-time cutoff for [nowEpochMillis]. Articles with a
     * `publishedAt` strictly before the cutoff are expired; an article exactly
     * [MAX_AGE_DAYS] old is still retained.
     */
    fun cutoffEpochMillis(nowEpochMillis: Long): Long =
        nowEpochMillis - Duration.ofDays(MAX_AGE_DAYS).toMillis()
}
