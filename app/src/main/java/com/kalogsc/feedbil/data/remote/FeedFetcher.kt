package com.kalogsc.feedbil.data.remote

import com.prof18.rssparser.RssParser
import com.prof18.rssparser.exception.HttpException
import com.prof18.rssparser.exception.RssParsingException
import com.prof18.rssparser.model.RssChannel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
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
        try {
            val channel = fetchChannel(url)
            FeedFetchResult.Success(
                title = channel.title,
                itemCount = channel.items.size,
            )
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
            is IllegalArgumentException -> FeedFetchResult.InvalidUrl
            is IOException -> FeedFetchResult.Network
            else -> FeedFetchResult.Unknown
        }
    }
}
