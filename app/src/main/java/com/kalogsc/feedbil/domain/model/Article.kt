package com.kalogsc.feedbil.domain.model

/**
 * An article as supplied by its feed.
 *
 * Fields mirror what the feed provided. Only [link] is required, because it is
 * the URL used to open the article and to build share text; entries without a
 * link are not stored.
 *
 * @property id local row identity; [UNSAVED_ID] until the article is stored.
 * @property feedId the feed this article belongs to. Duplicate detection is
 *   scoped to a feed: the same link may exist in two different feeds.
 * @property guid the publisher-declared entry identifier, when the feed has one.
 * @property link the article URL, used for identity, opening, and sharing.
 * @property publishedAt epoch millis used for ordering and retention. Feeds that
 *   omit a publication date get the observation time instead; see
 *   [com.kalogsc.feedbil.domain.ArticleDates].
 * @property lastReadAt epoch millis when the user last opened the article,
 *   or `null` when it has never been read.
 */
data class Article(
    val id: Long = UNSAVED_ID,
    val feedId: Long,
    val guid: String?,
    val link: String,
    val title: String?,
    val description: String?,
    val contentHtml: String?,
    val author: String?,
    val publishedAt: Long,
    val lastReadAt: Long? = null,
) {
    val isRead: Boolean
        get() = lastReadAt != null

    companion object {
        const val UNSAVED_ID = 0L
    }
}
