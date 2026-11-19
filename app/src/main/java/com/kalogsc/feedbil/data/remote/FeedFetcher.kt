package com.kalogsc.feedbil.data.remote

import com.prof18.rssparser.RssParser
import com.prof18.rssparser.exception.HttpException
import com.prof18.rssparser.exception.RssParsingException
import com.prof18.rssparser.model.RssChannel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import java.io.IOException
import java.io.InterruptedIOException

/**
 * Downloads and parses a single feed. Everything runs off the main thread and
 * failures come back as [FeedFetchResult] values instead of exceptions, so the
 * UI decides how to present them.
 *
 * The channel source is a constructor parameter so tests can substitute it;
 * production code uses the [RssParser] constructor.
 */
class FeedFetcher(
    private val fetchChannel: suspend (url: String) -> RssChannel,
) {

    constructor(parser: RssParser) : this(parser::getRssChannel)

    suspend fun fetch(url: String): FeedFetchResult = withContext(Dispatchers.IO) {
        // Use the same URL parser as the HTTP client. Without this, rssparser's
        // "not a feed" IllegalArgumentException would look like a malformed URL.
        if (url.toHttpUrlOrNull() == null) {
            return@withContext FeedFetchResult.InvalidUrl
        }
        try {
            // rssparser dispatches its own parsing to Dispatchers.Default; this
            // IO context keeps the surrounding fetch and error mapping off the main thread.
            val channel = fetchChannel(url)
            FeedFetchResult.Success(items = channel.items)
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (error: Throwable) {
            failureResult(error)
        }
    }

    companion object {
        /**
         * Maps a failed fetch to a user-facing category. DNS, TLS, and
         * connection failures are all [IOException]s and share one message.
         */
        fun failureResult(error: Throwable): FeedFetchResult = when (error) {
            is HttpException -> FeedFetchResult.HttpError(error.code)
            is RssParsingException -> FeedFetchResult.NotAFeed
            is InterruptedIOException -> FeedFetchResult.Timeout
            // fetch() already validated the URL, so an IllegalArgumentException
            // here is rssparser rejecting the response body as not a feed.
            is IllegalArgumentException -> FeedFetchResult.NotAFeed
            is IOException -> FeedFetchResult.Network
            else -> FeedFetchResult.Unknown
        }
    }
}
