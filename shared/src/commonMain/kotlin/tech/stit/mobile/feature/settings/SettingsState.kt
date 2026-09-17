package tech.stit.mobile.feature.settings

import tech.stit.mobile.core.config.AppEnv
import tech.stit.mobile.core.requirements.AppLinks

enum class ThemeMode {
    System,
    Light,
    Dark,
}

enum class AppLanguage(val tag: String?) {
    System(null),
    Ru("ru"),
    En("en"),
}

data class SettingsState(
    val theme: ThemeMode = ThemeMode.System,
    val language: AppLanguage = AppLanguage.System,
    val notificationsEnabled: Boolean = false,
    val version: String = "",
    val build: Long = 0,
    val env: AppEnv = AppEnv.Dev,
    // Правовые документы — по ссылкам из requirements, доступны до логина (05 L-3).
    val links: AppLinks = AppLinks(privacyPolicy = null, accountDeletion = null, support = null),
)
