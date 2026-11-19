package com.kalogsc.feedbil.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class FeedInputValidatorTest {

    @Test
    fun `blank names are rejected`() {
        assertEquals(FeedInputError.BLANK_NAME, FeedInputValidator.nameError(""))
        assertEquals(FeedInputError.BLANK_NAME, FeedInputValidator.nameError("   "))
    }

    @Test
    fun `names are limited to a fixed length after trimming`() {
        val atLimit = "a".repeat(FeedInputValidator.MAX_NAME_LENGTH)

        assertNull(FeedInputValidator.nameError(atLimit))
        assertNull(FeedInputValidator.nameError("  $atLimit  "))
        assertEquals(
            FeedInputError.NAME_TOO_LONG,
            FeedInputValidator.nameError(atLimit + "a"),
        )
    }

    @Test
    fun `http and https urls are accepted`() {
        assertNull(FeedInputValidator.urlError("https://blog.example.com/rss"))
        assertNull(FeedInputValidator.urlError("http://blog.example.com/feed.xml"))
        assertNull(FeedInputValidator.urlError("HTTPS://BLOG.EXAMPLE.COM/RSS"))
        assertNull(FeedInputValidator.urlError("  https://blog.example.com/rss  "))
    }

    @Test
    fun `blank urls are reported as blank`() {
        assertEquals(FeedInputError.BLANK_URL, FeedInputValidator.urlError(""))
        assertEquals(FeedInputError.BLANK_URL, FeedInputValidator.urlError("  "))
    }

    @Test
    fun `urls without a web scheme are rejected`() {
        assertEquals(FeedInputError.INVALID_URL, FeedInputValidator.urlError("blog.example.com/rss"))
        assertEquals(FeedInputError.INVALID_URL, FeedInputValidator.urlError("ftp://blog.example.com/rss"))
        assertEquals(FeedInputError.INVALID_URL, FeedInputValidator.urlError("file:///tmp/feed.xml"))
        assertEquals(FeedInputError.INVALID_URL, FeedInputValidator.urlError("javascript:alert(1)"))
    }

    @Test
    fun `urls without a host are rejected`() {
        assertEquals(FeedInputError.INVALID_URL, FeedInputValidator.urlError("https://"))
        assertEquals(FeedInputError.INVALID_URL, FeedInputValidator.urlError("https:///rss"))
    }

    @Test
    fun `urls containing spaces are rejected`() {
        assertEquals(
            FeedInputError.INVALID_URL,
            FeedInputValidator.urlError("https://blog.example.com/my feed.xml"),
        )
    }
}
