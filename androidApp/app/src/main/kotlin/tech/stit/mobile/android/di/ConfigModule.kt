package tech.stit.mobile.android.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import tech.stit.mobile.android.AppOptions
import tech.stit.mobile.android.BuildConfig
import tech.stit.mobile.core.config.AppConfig
import javax.inject.Singleton

// Единственное место чтения BuildConfig: сырые строки config/env → AppConfig shared и AppOptions платформы.
@Module
@InstallIn(SingletonComponent::class)
object ConfigModule {
    @Provides
    @Singleton
    fun appConfig(): AppConfig = AppConfig.parse(
        env = BuildConfig.APP_ENV,
        apiBaseUrl = BuildConfig.API_BASE_URL,
        apiMode = BuildConfig.API_MODE,
        authMode = BuildConfig.AUTH_MODE,
        deepLinkHost = BuildConfig.DEEP_LINK_HOST,
        deepLinkScheme = BuildConfig.DEEP_LINK_SCHEME,
        sentryDsn = BuildConfig.SENTRY_DSN,
    )

    @Provides
    fun appOptions(): AppOptions = AppOptions(
        pushProviders = BuildConfig.PUSH_PROVIDERS,
        rustoreProjectId = BuildConfig.RUSTORE_PROJECT_ID,
        biometricUnlock = BuildConfig.AUTH_BIOMETRIC_UNLOCK.toBoolean(),
    )
}
