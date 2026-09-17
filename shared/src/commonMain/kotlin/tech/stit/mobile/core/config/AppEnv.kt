package tech.stit.mobile.core.config

// Среда сборки приходит строкой из BuildConfig.APP_ENV (Android) и Info.plist (iOS) — см. config/env/*.properties.
enum class AppEnv {
    Dev,
    Staging,
    Prod,
    Demo,
    ;

    companion object {
        fun parse(raw: String): AppEnv = parseEnum(raw, entries, "ая среда")
    }
}
