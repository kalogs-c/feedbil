package com.kalogsc.feedbil.domain

import java.net.URI

/**
 * Returns [value] only when it is an absolute http(s) URL with a host, or
 * `null` otherwise. Feed content is untrusted: this is the single gate used
 * before a URL is opened or rendered as an image or link.
 */
object HttpUrlValidator {

    fun sanitize(value: String?): String? {
        val candidate = value?.trim()?.takeIf { it.isNotEmpty() } ?: return null
        return try {
            val uri = URI(candidate)
            val scheme = uri.scheme?.lowercase()
            if ((scheme == "http" || scheme == "https") && !uri.host.isNullOrEmpty()) {
                candidate
            } else {
                null
            }
        } catch (_: Exception) {
            null
        }
    }
}
