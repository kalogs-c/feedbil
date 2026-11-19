package com.kalogsc.feedbil.ui.common

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant
import java.time.ZoneOffset

class ArticleDateFormatterTest {

    @Test
    fun `formats a timestamp in pt-BR with the supplied zone`() {
        val epochMillis = Instant.parse("2026-11-17T09:00:00Z").toEpochMilli()

        assertEquals("17/11/2026, 09:00", formatDateTime(epochMillis, ZoneOffset.UTC))
    }

    @Test
    fun `uses the supplied zone instead of the instant's zone`() {
        val epochMillis = Instant.parse("2026-11-17T09:00:00Z").toEpochMilli()

        assertEquals("17/11/2026, 10:00", formatDateTime(epochMillis, ZoneOffset.ofHours(1)))
    }
}
