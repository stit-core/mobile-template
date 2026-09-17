package tech.stit.mobile.testing

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import tech.stit.mobile.PlatformDeps
import tech.stit.mobile.core.auth.SecureStore
import tech.stit.mobile.core.i18n.LocaleProvider
import tech.stit.mobile.core.lib.AppInfo
import tech.stit.mobile.core.lib.ConnectivityMonitor
import tech.stit.mobile.core.lib.DispatcherProvider
import tech.stit.mobile.core.lib.Settings
import tech.stit.mobile.core.observability.LogLevel
import tech.stit.mobile.core.observability.LogSink
import tech.stit.mobile.core.observability.TelemetrySink
import tech.stit.mobile.core.push.NotificationPermission
import tech.stit.mobile.core.push.PushProvider
import tech.stit.mobile.core.push.PushTransport
import kotlin.time.Clock
import kotlin.time.Instant

// Фейки платформенной границы: тесты гоняют настоящий стек shared, подменяя только то, что даёт ОС.

class TestDispatchers(dispatcher: CoroutineDispatcher) : DispatcherProvider {
    override val main: CoroutineDispatcher = dispatcher
    override val io: CoroutineDispatcher = dispatcher
    override val default: CoroutineDispatcher = dispatcher
}

class FakeClock(var current: Instant = Instant.parse("2026-09-17T12:00:00Z")) : Clock {
    override fun now(): Instant = current
}

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

class FakePushTransport(override val provider: PushProvider, var currentToken: String?) : PushTransport {
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

class FakeTelemetry : TelemetrySink {
    val breadcrumbs = mutableListOf<String>()
    val userIds = mutableListOf<String?>()

    override fun breadcrumb(category: String, message: String) {
        breadcrumbs += "$category: $message"
    }

    override fun captureError(throwable: Throwable, requestId: String?) = Unit

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
    override val secureStore: FakeSecureStore = FakeSecureStore(),
    override val settings: FakeSettings = FakeSettings(),
    override val connectivity: FakeConnectivity = FakeConnectivity(),
    override val pushTransports: List<PushTransport> = listOf(FakePushTransport(PushProvider.Fcm, "fcm-token-1")),
    override val notificationPermission: FakeNotificationPermission = FakeNotificationPermission(),
    override val telemetry: FakeTelemetry = FakeTelemetry(),
    override val logSink: MemoryLogSink = MemoryLogSink(),
) : PlatformDeps {
    override val locale: LocaleProvider = FakeLocale
    override val appInfo: AppInfo = AppInfo(version = "1.0.0", build = 1_000_000, osVersion = "Android 16", deviceModel = "Pixel 8")
    override val filesDir: String = "/data/app"
}

val StateFlow<Boolean>.asMutable: MutableStateFlow<Boolean> get() = this as MutableStateFlow<Boolean>
