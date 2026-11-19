package com.kalogsc.feedbil.domain

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Duration

class RetentionPolicyTest {

    @Test
    fun `cutoff is seven days before now`() {
        val now = 1_800_000_000_000L

        assertEquals(now - Duration.ofDays(7).toMillis(), RetentionPolicy.cutoffEpochMillis(now))
    }

    @Test
    fun `cutoff does not depend on the current clock`() {
        val now = 12_345_678_901L

        assertEquals(
            RetentionPolicy.cutoffEpochMillis(now),
            RetentionPolicy.cutoffEpochMillis(now),
        )
    }

    @Test
    fun `policy constants match the product rules`() {
        assertEquals(7L, RetentionPolicy.MAX_AGE_DAYS)
        assertEquals(100, RetentionPolicy.MAX_ARTICLES_PER_FEED)
    }
}
