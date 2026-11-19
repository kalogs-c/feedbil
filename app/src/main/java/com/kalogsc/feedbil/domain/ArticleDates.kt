package com.kalogsc.feedbil.domain

/**
 * Decides which timestamp is stored as an article's publication date.
 *
 * Ordering and the 7-day retention rule need a value for every article, but not
 * every feed supplies a usable date. When the feed date is missing or cannot be
 * parsed, the observation time is used instead so the article still expires and
 * ranks consistently. This is a deliberate exception to not inventing feed data;
 * see `DECISIONS.md`.
 */
object ArticleDates {
    /**
     * @param feedDateEpochMillis date parsed from the feed, or `null` when the
     *   feed omitted it or supplied an unparseable value.
     * @param observedAtEpochMillis when the entry was seen and stored.
     */
    fun resolvePublishedAt(feedDateEpochMillis: Long?, observedAtEpochMillis: Long): Long =
        feedDateEpochMillis ?: observedAtEpochMillis
}
