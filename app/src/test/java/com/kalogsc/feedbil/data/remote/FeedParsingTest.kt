package com.kalogsc.feedbil.data.remote

import com.prof18.rssparser.RssParser
import com.prof18.rssparser.RssParserBuilder
import com.prof18.rssparser.exception.RssParsingException
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Runs the real parser against local fixtures, so the dependency's behavior on
 * Android is covered without touching the network.
 */
@RunWith(RobolectricTestRunner::class)
class FeedParsingTest {

    private val parser: RssParser = RssParserBuilder().build()

    @Test
    fun `parses an RSS 2_0 feed`() = runBlocking {
        val channel = parser.parse(fixture("feeds/valid_rss.xml"))

        assertEquals("Example Blog", channel.title)
        assertEquals(2, channel.items.size)
        assertEquals("https://blog.example.com/first", channel.items.first().link)
        assertEquals("post-1", channel.items.first().guid)
    }

    @Test
    fun `parses an Atom feed`() = runBlocking {
        val channel = parser.parse(fixture("feeds/valid_atom.xml"))

        assertEquals("Atom Example", channel.title)
        assertEquals(1, channel.items.size)
        assertEquals("https://atom.example.com/post", channel.items.first().link)
    }

    @Test
    fun `a well-formed website is not accepted as a feed`() {
        assertThrows(IllegalArgumentException::class.java) {
            runBlocking { parser.parse(fixture("feeds/website_page.html")) }
        }
    }

    @Test
    fun `malformed xml is reported as a parsing failure`() {
        assertThrows(RssParsingException::class.java) {
            runBlocking { parser.parse(fixture("feeds/malformed.xml")) }
        }
    }

    private fun fixture(path: String): String =
        checkNotNull(javaClass.classLoader?.getResourceAsStream(path)) {
            "Missing test fixture: $path"
        }.bufferedReader().use { it.readText() }
}
