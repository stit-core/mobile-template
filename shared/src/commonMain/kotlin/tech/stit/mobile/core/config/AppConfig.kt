package tech.stit.mobile.core.config

// Ключи = config/env/<flavor>.properties; собирается платформой из BuildConfig / Info.plist, runtime-значения — из requirements.
data class AppConfig(
    val env: AppEnv,
    val apiBaseUrl: String,
    val apiMode: ApiMode,
    val authMode: AuthMode,
    val deepLinkHost: String,
    val deepLinkScheme: String,
    val sentryDsn: String,
) {
    companion object {
        // Платформы отдают сырые строки: одна точка разбора вместо parse каждого enum на каждой платформе.
        fun parse(
            env: String,
            apiBaseUrl: String,
            apiMode: String,
            authMode: String,
            deepLinkHost: String,
            deepLinkScheme: String,
            sentryDsn: String,
        ): AppConfig = AppConfig(
            env = AppEnv.parse(env),
            apiBaseUrl = apiBaseUrl.trimEnd('/'),
            apiMode = ApiMode.parse(apiMode),
            authMode = AuthMode.parse(authMode),
            deepLinkHost = deepLinkHost,
            deepLinkScheme = deepLinkScheme,
            sentryDsn = sentryDsn,
        )
    }
}
