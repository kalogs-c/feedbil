package com.kalogsc.feedbil.ui.common

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val appLocale = Locale.forLanguageTag("pt-BR")
private val shortDateTime = DateTimeFormatter.ofPattern("dd/MM/yyyy, HH:mm", appLocale)

/**
 * Fixed pt-BR formatting for article and feed timestamps. The app's strings are
 * pt-BR regardless of the device locale, so dates are too. The instant is shown
 * in the device time zone.
 */
fun formatDateTime(epochMillis: Long, zoneId: ZoneId = ZoneId.systemDefault()): String =
    shortDateTime.withZone(zoneId).format(Instant.ofEpochMilli(epochMillis))
