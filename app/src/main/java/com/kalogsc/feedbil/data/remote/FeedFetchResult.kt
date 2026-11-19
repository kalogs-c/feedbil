package com.kalogsc.feedbil.data.remote

/**
 * Outcome of fetching and parsing one feed URL. Failures are values, not
 * exceptions, so the UI can map each one to a clear message.
 */
sealed interface FeedFetchResult {

    /** The URL served a feed the parser understood. */
    data class Success(
        val title: String?,
        val itemCount: Int,
    ) : FeedFetchResult

    /** The URL is syntactically unusable; the form should reject it earlier. */
    data object InvalidUrl : FeedFetchResult

    /** The server answered with a non-success status. */
    data class HttpError(val code: Int) : FeedFetchResult

    /** The response was not RSS/Atom/RDF (for example an HTML page). */
    data object NotAFeed : FeedFetchResult

    /** The request did not finish in time. */
    data object Timeout : FeedFetchResult

    /** Host unreachable, DNS failure, TLS failure, or connection dropped. */
    data object Network : FeedFetchResult

    /** Anything else; kept so a bug never crashes the screen. */
    data object Unknown : FeedFetchResult
}
