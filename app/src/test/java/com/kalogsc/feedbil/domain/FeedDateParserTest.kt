package com.kalogsc.feedbil.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Instant

class FeedDateParserTest {

    private val expected = Instant.parse("2026-11-17T09:00:00Z").toEpochMilli()

    @Test
    fun `parses an RFC 1123 feed date`() {
        assertEquals(expected, FeedDateParser.parseEpochMillis("Tue, 17 Nov 2026 09:00:00 GMT"))
    }

    @Test
    fun `parses an RFC 822 feed date with a numeric offset`() {
        assertEquals(expected, FeedDateParser.parseEpochMillis("Tue, 17 Nov 2026 09:00:00 +0000"))
    }

    @Test
    fun `parses an RFC 822 feed date without a weekday`() {
        assertEquals(expected, FeedDateParser.parseEpochMillis("17 Nov 2026 09:00:00 +0000"))
    }

    @Test
    fun `parses an ISO instant`() {
        assertEquals(expected, FeedDateParser.parseEpochMillis("2026-11-17T09:00:00Z"))
    }

    @Test
    fun `parses an ISO offset date`() {
        assertEquals(expected, FeedDateParser.parseEpochMillis("2026-11-17T10:00:00+01:00"))
    }

    @Test
    fun `trims surrounding whitespace`() {
        assertEquals(expected, FeedDateParser.parseEpochMillis("  2026-11-17T09:00:00Z  "))
    }

    @Test
    fun `a missing date is null`() {
        assertNull(FeedDateParser.parseEpochMillis(null))
        assertNull(FeedDateParser.parseEpochMillis(""))
        assertNull(FeedDateParser.parseEpochMillis("   "))
    }

    @Test
    fun `an unparseable date is null`() {
        assertNull(FeedDateParser.parseEpochMillis("last Tuesday"))
        assertNull(FeedDateParser.parseEpochMillis("2026-13-45T99:99:99Z"))
    }
}
