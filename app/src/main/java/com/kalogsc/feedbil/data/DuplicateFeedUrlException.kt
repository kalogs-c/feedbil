package com.kalogsc.feedbil.data

/**
 * A write was rejected because another feed already uses the URL. Feed URLs are
 * unique at the storage level, so this can only happen on a race after the
 * pre-check; the form still shows the same "already in your list" message.
 */
class DuplicateFeedUrlException(url: String, cause: Throwable? = null) :
    Exception("A feed with URL $url is already stored", cause)
