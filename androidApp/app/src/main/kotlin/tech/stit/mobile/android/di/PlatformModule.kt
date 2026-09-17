package tech.stit.mobile.android.di

import android.app.Application
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import tech.stit.mobile.PlatformDeps
import tech.stit.mobile.android.AppOptions
import tech.stit.mobile.android.BuildConfig
import tech.stit.mobile.android.core.platform.AndroidPlatformDeps
import tech.stit.mobile.android.core.platform.BiometricUnlock
import tech.stit.mobile.android.core.platform.DefaultBiometricUnlock
import tech.stit.mobile.android.core.platform.NotificationPermissionGate
import tech.stit.mobile.android.core.platform.SentryCrashReporter
import tech.stit.mobile.android.core.platform.SentryLogSink
import tech.stit.mobile.android.core.platform.push.pushTransports
import tech.stit.mobile.core.config.AppConfig
import tech.stit.mobile.core.observability.LogcatSink
import tech.stit.mobile.core.observability.NoopTelemetrySink
import javax.inject.Singleton

// Всё платформенное собирается здесь и уходит в shared одним PlatformDeps; тесты подменяют модуль целиком фейками.
@Module
@InstallIn(SingletonComponent::class)
object PlatformModule {
    @Provides
    @Singleton
    fun platformDeps(
        application: Application,
        options: AppOptions,
        config: AppConfig,
        permission: NotificationPermissionGate,
    ): PlatformDeps = AndroidPlatformDeps(
        context = application,
        pushTransports = pushTransports(application, options.pushProviders, options.rustoreProjectId),
        notificationPermission = permission,
        // Пустой SENTRY_DSN — телеметрия no-op; в release warn+ идут в breadcrumbs, в debug — в Logcat.
        telemetry = if (config.sentryDsn.isBlank()) NoopTelemetrySink else SentryCrashReporter(),
        logSink = if (BuildConfig.DEBUG) LogcatSink() else SentryLogSink(),
    )

    @Provides
    @Singleton
    fun biometricUnlock(application: Application, options: AppOptions): BiometricUnlock =
        DefaultBiometricUnlock(application, enabled = options.biometricUnlock)
}
