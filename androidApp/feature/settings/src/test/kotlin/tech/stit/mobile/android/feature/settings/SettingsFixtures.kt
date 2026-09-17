package tech.stit.mobile.android.feature.settings

import tech.stit.mobile.core.config.AppEnv
import tech.stit.mobile.core.requirements.AppLinks
import tech.stit.mobile.feature.settings.SettingsState

// Ссылки — как в mock/fixtures/populated/requirements.json.
internal val demoSettings = SettingsState(
    version = "1.0.0",
    build = 1_000_000,
    env = AppEnv.Demo,
    links = AppLinks(
        privacyPolicy = "https://app.example.com/privacy",
        accountDeletion = "https://app.example.com/account/delete",
        support = "https://app.example.com/support",
    ),
)
