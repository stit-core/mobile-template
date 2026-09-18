package tech.stit.mobile.android.core.testing

import kotlinx.coroutines.flow.MutableStateFlow
import tech.stit.mobile.PlatformDeps
import tech.stit.mobile.core.auth.SecureStore
import tech.stit.mobile.core.background.BackgroundScheduler
import tech.stit.mobile.core.background.BackgroundTask
import tech.stit.mobile.core.config.ApiMode
import tech.stit.mobile.core.config.AppConfig
import tech.stit.mobile.core.config.AppEnv
import tech.stit.mobile.core.config.AuthMode
import tech.stit.mobile.core.i18n.LocaleProvider
import tech.stit.mobile.core.lib.AppInfo
import tech.stit.mobile.core.lib.ConnectivityMonitor
import tech.stit.mobile.core.lib.Settings
import tech.stit.mobile.core.lib.Store
import tech.stit.mobile.core.observability.LogLevel
import tech.stit.mobile.core.observability.LogSink
import tech.stit.mobile.core.observability.TelemetrySink
import tech.stit.mobile.core.push.NotificationPermission
import tech.stit.mobile.core.push.PushProvider
import tech.stit.mobile.core.push.PushTransport

// Фейки платформенной границы для Android-тестов: настоящий стек shared на MockTransport, подменяется только то, что даёт ОС.

fun demoConfig(): AppConfig = AppConfig(
    env = AppEnv.Demo,
    apiBaseUrl = "https://api.demo.invalid",
    apiMode = ApiMode.Mock,
    authMode = AuthMode.Bearer,
    deepLinkHost = "app.demo.invalid",
    deepLinkScheme = "stit",
    sentryDsn = "",
)

class FakeSecureStore : SecureStore {
    val values = mutableMapOf<String, String>()

    override suspend fun read(key: String): String? = values[key]

    override suspend fun write(key: String, value: String) {
        values[key] = value
    }

    override suspend fun remove(key: String) {
        values.remove(key)
    }
}

class FakeSettings : Settings {
    val values = mutableMapOf<String, String>()

    override suspend fun getString(key: String): String? = values[key]

    override suspend fun putString(key: String, value: String) {
        values[key] = value
    }

    override suspend fun remove(key: String) {
        values.remove(key)
    }
}

class FakeConnectivity(initial: Boolean = true) : ConnectivityMonitor {
    override val online: MutableStateFlow<Boolean> = MutableStateFlow(initial)
}

class FakePushTransport(override val provider: PushProvider = PushProvider.Fcm, var currentToken: String? = "fcm-token") : PushTransport {
    override suspend fun token(): String? = currentToken
}

class FakeNotificationPermission(var granted: Boolean = false, var answer: Boolean = true) : NotificationPermission {
    var requests = 0

    override suspend fun isGranted(): Boolean = granted

    override suspend fun request(): Boolean {
        requests++
        granted = answer
        return granted
    }
}

class FakeBackgroundScheduler : BackgroundScheduler {
    val scheduled = mutableListOf<BackgroundTask>()
    val cancelled = mutableListOf<String>()

    override fun schedule(task: BackgroundTask) {
        scheduled += task
    }

    override fun cancel(id: String) {
        cancelled += id
    }
}

class FakeTelemetry : TelemetrySink {
    val breadcrumbs = mutableListOf<String>()
    val errors = mutableListOf<Throwable>()
    val userIds = mutableListOf<String?>()

    override fun breadcrumb(category: String, message: String) {
        breadcrumbs += "$category: $message"
    }

    override fun captureError(throwable: Throwable, requestId: String?) {
        errors += throwable
    }

    override fun setUserId(id: String?) {
        userIds += id
    }
}

class MemoryLogSink : LogSink {
    val lines = mutableListOf<String>()

    override fun log(level: LogLevel, tag: String, message: String, throwable: Throwable?) {
        lines += "$level $tag: $message"
    }
}

object FakeLocale : LocaleProvider {
    override val languageTag: String = "ru-RU"
    override val timeZone: String = "Europe/Moscow"
}

class FakePlatformDeps(
    override val filesDir: String,
    override val secureStore: FakeSecureStore = FakeSecureStore(),
    override val settings: FakeSettings = FakeSettings(),
    override val connectivity: FakeConnectivity = FakeConnectivity(),
    override val pushTransports: List<PushTransport> = listOf(FakePushTransport()),
    override val notificationPermission: FakeNotificationPermission = FakeNotificationPermission(),
    override val background: FakeBackgroundScheduler = FakeBackgroundScheduler(),
    override val telemetry: FakeTelemetry = FakeTelemetry(),
    override val logSink: MemoryLogSink = MemoryLogSink(),
) : PlatformDeps {
    override val locale: LocaleProvider = FakeLocale
    override val appInfo: AppInfo = AppInfo(
        appId = "tech.stit.mobile.demo",
        version = "1.0.0",
        build = 1_000_000,
        osVersion = "Android 16",
        deviceModel = "Pixel 8",
        store = Store.Direct,
    )
}
