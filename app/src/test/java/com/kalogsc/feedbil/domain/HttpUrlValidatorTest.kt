package com.kalogsc.feedbil.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class HttpUrlValidatorTest {

    @Test
    fun `accepts absolute http and https urls`() {
        assertEquals("https://example.com/a?b=1", HttpUrlValidator.sanitize(" https://example.com/a?b=1 "))
        assertEquals("http://example.com", HttpUrlValidator.sanitize("http://example.com"))
        assertEquals("HTTPS://example.com", HttpUrlValidator.sanitize("HTTPS://example.com"))
    }

    @Test
    fun `rejects non http schemes and relative urls`() {
        listOf(
            "javascript:alert(1)",
            "file:///sdcard/a.jpg",
            "data:image/png;base64,abc",
            "//example.com/a.jpg",
            "/local/path",
            "https:",
            "not a url",
            "https://example.com/a b",
            "https://example.com/a\u0000b",
            "",
            null,
        ).forEach { assertNull(HttpUrlValidator.sanitize(it)) }
    }
}
