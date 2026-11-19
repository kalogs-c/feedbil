package com.kalogsc.feedbil.data.remote

import com.prof18.rssparser.exception.HttpException
import com.prof18.rssparser.exception.RssParsingException
import com.prof18.rssparser.model.RssChannel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import java.io.IOException
import java.io.InterruptedIOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.net.ssl.SSLHandshakeException

class FeedFetcherTest {

    private val url = "https://example.com/rss"

    @Test
    fun `successful fetch reports the channel title and item count`() = runTest {
        val fetcher = FeedFetcher {
            RssChannel(
                title = "Example Blog",
                link = null,
                description = null,
                image = null,
                lastBuildDate = null,
                updatePeriod = null,
                items = emptyList(),
                itunesChannelData = null,
                youtubeChannelData = null,
            )
        }

        assertEquals(FeedFetchResult.Success("Example Blog", emptyList()), fetcher.fetch(url))
    }

    @Test
    fun `http errors keep their status code`() = runTest {
        val fetcher = FeedFetcher { throw HttpException(code = 404, message = "Not Found") }

        assertEquals(FeedFetchResult.HttpError(404), fetcher.fetch(url))
    }

    @Test
    fun `unparseable content is not a feed`() = runTest {
        val fetcher = FeedFetcher {
            throw RssParsingException(message = "invalid xml", cause = null)
        }

        assertEquals(FeedFetchResult.NotAFeed, fetcher.fetch(url))
    }

    @Test
    fun `socket and call timeouts are reported as timeouts`() {
        assertEquals(
            FeedFetchResult.Timeout,
            FeedFetcher.failureResult(SocketTimeoutException("read timed out")),
        )
        assertEquals(
            FeedFetchResult.Timeout,
            FeedFetcher.failureResult(InterruptedIOException("timeout")),
        )
    }

    @Test
    fun `connection, dns, and tls failures are network errors`() {
        assertEquals(
            FeedFetchResult.Network,
            FeedFetcher.failureResult(UnknownHostException("example.com")),
        )
        assertEquals(
            FeedFetchResult.Network,
            FeedFetcher.failureResult(SSLHandshakeException("bad certificate")),
        )
        assertEquals(
            FeedFetchResult.Network,
            FeedFetcher.failureResult(IOException("connection reset")),
        )
    }

    @Test
    fun `an unusable url is reported separately`() {
        assertEquals(
            FeedFetchResult.InvalidUrl,
            FeedFetcher.failureResult(IllegalArgumentException("invalid url")),
        )
    }

    @Test
    fun `anything else is unknown instead of crashing`() {
        assertEquals(
            FeedFetchResult.Unknown,
            FeedFetcher.failureResult(IllegalStateException("unexpected")),
        )
    }

    @Test
    fun `cancellation is not converted into a result`() {
        val fetcher = FeedFetcher { throw CancellationException("cancelled") }

        assertThrows(CancellationException::class.java) {
            runBlocking { fetcher.fetch(url) }
        }
    }
}
