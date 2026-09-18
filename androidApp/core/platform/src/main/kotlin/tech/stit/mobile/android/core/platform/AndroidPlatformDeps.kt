package tech.stit.mobile.android.core.platform

import android.content.Context
import tech.stit.mobile.PlatformDeps
import tech.stit.mobile.android.core.platform.background.AndroidBackgroundScheduler
import tech.stit.mobile.core.auth.KeystoreSecureStore
import tech.stit.mobile.core.auth.SecureStore
import tech.stit.mobile.core.background.BackgroundScheduler
import tech.stit.mobile.core.i18n.LocaleProvider
import tech.stit.mobile.core.lib.AppInfo
import tech.stit.mobile.core.lib.ConnectivityMonitor
import tech.stit.mobile.core.lib.DataStoreSettings
import tech.stit.mobile.core.lib.Settings
import tech.stit.mobile.core.observability.LogSink
import tech.stit.mobile.core.observability.TelemetrySink
import tech.stit.mobile.core.push.NotificationPermission
import tech.stit.mobile.core.push.PushTransport

// Единственная Android-реализация PlatformDeps: один экземпляр на процесс (DataStore не терпит второго на тот же файл).
class AndroidPlatformDeps(
    context: Context,
    override val pushTransports: List<PushTransport>,
    override val notificationPermission: NotificationPermission,
    override val telemetry: TelemetrySink,
    override val logSink: LogSink,
) : PlatformDeps {
    override val appInfo: AppInfo = context.androidAppInfo()
    override val secureStore: SecureStore = KeystoreSecureStore(context)
    override val settings: Settings = DataStoreSettings(context)
    override val filesDir: String = context.filesDir.absolutePath
    override val background: BackgroundScheduler = AndroidBackgroundScheduler(context)
    override val locale: LocaleProvider = AndroidLocaleProvider
    override val connectivity: ConnectivityMonitor = AndroidConnectivityMonitor(context)
}
