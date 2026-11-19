package com.kalogsc.feedbil.domain.model

/**
 * A feed the user subscribed to.
 *
 * @property id local row identity; [UNSAVED_ID] until the feed is stored.
 * @property name user-provided display name.
 * @property url the RSS URL exactly as the user entered it; unique per app.
 * @property addedAt epoch millis when the feed was added, used for stable
 *   subscription ordering.
 * @property lastFetchedAt epoch millis of the last successful refresh, or
 *   `null` when the feed has never been refreshed successfully.
 */
data class Feed(
    val id: Long = UNSAVED_ID,
    val name: String,
    val url: String,
    val addedAt: Long,
    val lastFetchedAt: Long? = null,
) {
    companion object {
        const val UNSAVED_ID = 0L
    }
}
