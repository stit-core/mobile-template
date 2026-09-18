package tech.stit.mobile

import tech.stit.mobile.core.auth.SecureStore
import tech.stit.mobile.core.background.BackgroundScheduler
import tech.stit.mobile.core.i18n.LocaleProvider
import tech.stit.mobile.core.lib.AppInfo
import tech.stit.mobile.core.lib.ConnectivityMonitor
import tech.stit.mobile.core.lib.Settings
import tech.stit.mobile.core.observability.LogSink
import tech.stit.mobile.core.observability.TelemetrySink
import tech.stit.mobile.core.push.NotificationPermission
import tech.stit.mobile.core.push.PushTransport

// Всё платформенное входит через этот интерфейс (Android: core:platform, iOS: AppEnvironment.swift) — commonMain без platform API.
interface PlatformDeps {
    val appInfo: AppInfo
    val secureStore: SecureStore
    val settings: Settings

    // Абсолютный путь каталога для FileCache: Context.filesDir / NSDocumentDirectory.
    val filesDir: String
    val pushTransports: List<PushTransport>
    val notificationPermission: NotificationPermission
    val background: BackgroundScheduler
    val telemetry: TelemetrySink
    val logSink: LogSink
    val locale: LocaleProvider
    val connectivity: ConnectivityMonitor
}
