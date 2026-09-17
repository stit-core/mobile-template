package tech.stit.mobile.android.core.ui

import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import kotlin.time.Instant
import kotlin.time.toJavaInstant

// Даты — платформенным форматтером над kotlin.time.Instant из shared (styling § i18n); локаль и пояс — устройства.
fun Instant.toDateTimeText(): String =
    DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT).withZone(ZoneId.systemDefault()).format(toJavaInstant())

fun Instant.toDateText(): String =
    DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withZone(ZoneId.systemDefault()).format(toJavaInstant())
