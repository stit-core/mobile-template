package tech.stit.mobile.core.config

import kotlin.enums.EnumEntries

// Значения BuildConfig/Info.plist приходят строками из config/env/*.properties; одно место разбора для всех enum конфига.
internal fun <T : Enum<T>> parseEnum(raw: String, entries: EnumEntries<T>, what: String): T =
    entries.firstOrNull { it.name.equals(raw, ignoreCase = true) }
        ?: throw IllegalArgumentException("неизвестн$what '$raw'; допустимы: ${entries.joinToString { it.name.lowercase() }}")
