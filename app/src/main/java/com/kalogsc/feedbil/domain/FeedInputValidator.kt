package com.kalogsc.feedbil.domain

import java.net.URI

/**
 * Reasons a feed form cannot be saved. They are checked before any network
 * request, so obvious mistakes never cost a fetch.
 */
enum class FeedInputError {
    BLANK_NAME,
    NAME_TOO_LONG,
    BLANK_URL,
    INVALID_URL,
}

/**
 * Form-level validation for the user-provided feed name and URL. Network
 * reachability and feed syntax are a separate concern (`FeedFetcher`).
 */
object FeedInputValidator {

    /** Maximum length of a feed name, after trimming. */
    const val MAX_NAME_LENGTH = 80

    fun nameError(name: String): FeedInputError? {
        val trimmed = name.trim()
        return when {
            trimmed.isEmpty() -> FeedInputError.BLANK_NAME
            trimmed.length > MAX_NAME_LENGTH -> FeedInputError.NAME_TOO_LONG
            else -> null
        }
    }

    /**
     * Requires an absolute `http` or `https` URL with a host. Other schemes are
     * rejected because the app can only fetch web feeds, and malformed input is
     * rejected here instead of failing inside the HTTP client.
     */
    fun urlError(url: String): FeedInputError? {
        val trimmed = url.trim()
        if (trimmed.isEmpty()) return FeedInputError.BLANK_URL
        return if (isWebUrl(trimmed)) null else FeedInputError.INVALID_URL
    }

    private fun isWebUrl(candidate: String): Boolean {
        val uri = runCatching { URI(candidate) }.getOrNull() ?: return false
        val scheme = uri.scheme?.lowercase() ?: return false
        return (scheme == "http" || scheme == "https") && !uri.host.isNullOrBlank()
    }
}
