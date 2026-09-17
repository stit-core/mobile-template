package tech.stit.mobile.feature.settings

sealed class SettingsIntent {
    data object Start : SettingsIntent()

    data class SetTheme(val theme: ThemeMode) : SettingsIntent()

    data class SetLanguage(val language: AppLanguage) : SettingsIntent()

    data class SetNotifications(val enabled: Boolean) : SettingsIntent()
}
