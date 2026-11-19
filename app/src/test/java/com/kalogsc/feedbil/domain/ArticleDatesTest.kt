package com.kalogsc.feedbil.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class ArticleDatesTest {

    @Test
    fun `feed date is kept when present`() {
        val feedDate = 1_700_000_000_000L
        val observedAt = 1_800_000_000_000L

        assertEquals(feedDate, ArticleDates.resolvePublishedAt(feedDate, observedAt))
    }

    @Test
    fun `observation time is used when the feed has no date`() {
        val observedAt = 1_800_000_000_000L

        assertEquals(observedAt, ArticleDates.resolvePublishedAt(null, observedAt))
    }

    @Test
    fun `future feed dates are kept as-is`() {
        val observedAt = 1_800_000_000_000L
        val futureDate = observedAt + 1_000L

        assertEquals(futureDate, ArticleDates.resolvePublishedAt(futureDate, observedAt))
    }
}
