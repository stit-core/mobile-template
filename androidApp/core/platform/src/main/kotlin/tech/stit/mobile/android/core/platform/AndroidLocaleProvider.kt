package tech.stit.mobile.android.core.platform

import tech.stit.mobile.core.i18n.LocaleProvider
import java.util.Locale
import java.util.TimeZone

// Locale.getDefault() уже учитывает per-app locale (AppCompatDelegate.setApplicationLocales).
object AndroidLocaleProvider : LocaleProvider {
    override val languageTag: String get() = Locale.getDefault().toLanguageTag()
    override val timeZone: String get() = TimeZone.getDefault().id
}
