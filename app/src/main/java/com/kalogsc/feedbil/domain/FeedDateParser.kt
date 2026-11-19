package com.kalogsc.feedbil.domain

import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.util.Locale

/**
 * Parses the date strings feeds use in RSS and Atom entries.
 *
 * rssparser exposes the raw date text, so the app owns the format handling.
 * Common RFC 1123 / RFC 822 strings and ISO 8601 instants are accepted; anything
 * else returns `null`, letting [ArticleDates] fall back to the observation time.
 */
object FeedDateParser {

    private val rfc1123 = DateTimeFormatter.RFC_1123_DATE_TIME
    private val rfc822WithOffset =
        DateTimeFormatter.ofPattern("EEE, d MMM yyyy HH:mm:ss Z", Locale.ENGLISH)
    private val rfc822WithZone =
        DateTimeFormatter.ofPattern("EEE, d MMM yyyy HH:mm:ss z", Locale.ENGLISH)
    private val rfc822WithoutDay =
        DateTimeFormatter.ofPattern("d MMM yyyy HH:mm:ss Z", Locale.ENGLISH)
    private val rfc822WithoutDayAndOffset =
        DateTimeFormatter.ofPattern("d MMM yyyy HH:mm:ss z", Locale.ENGLISH)

    fun parseEpochMillis(value: String?): Long? {
        val text = value?.trim().orEmpty()
        if (text.isEmpty()) return null
        return parseInstant(text)?.toEpochMilli()
    }

    private fun parseInstant(text: String): Instant? =
        parseOrNull { Instant.parse(text) }
            ?: parseOrNull {
                OffsetDateTime.parse(text, DateTimeFormatter.ISO_OFFSET_DATE_TIME).toInstant()
            }
            ?: parseOrNull { ZonedDateTime.parse(text, rfc1123).toInstant() }
            ?: parseOrNull { ZonedDateTime.parse(text, rfc822WithZone).toInstant() }
            ?: parseOrNull { OffsetDateTime.parse(text, rfc822WithOffset).toInstant() }
            ?: parseOrNull { ZonedDateTime.parse(text, rfc822WithoutDayAndOffset).toInstant() }
            ?: parseOrNull { OffsetDateTime.parse(text, rfc822WithoutDay).toInstant() }

    private inline fun parseOrNull(block: () -> Instant): Instant? = try {
        block()
    } catch (_: DateTimeParseException) {
        null
    } catch (_: IllegalArgumentException) {
        null
    }
}
