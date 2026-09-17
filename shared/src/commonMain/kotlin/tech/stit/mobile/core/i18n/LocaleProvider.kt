package tech.stit.mobile.core.i18n

// Локаль и часовой пояс пользователя от платформы (Locale.getDefault() / NSLocale) — для регистрации устройства и форматирования.
interface LocaleProvider {
    val languageTag: String
    val timeZone: String
}
