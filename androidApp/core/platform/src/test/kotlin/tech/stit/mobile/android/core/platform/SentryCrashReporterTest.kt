package tech.stit.mobile.android.core.platform

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.sentry.Sentry
import org.junit.Test
import org.junit.runner.RunWith
import tech.stit.mobile.core.config.AppConfig
import tech.stit.mobile.core.lib.AppInfo
import tech.stit.mobile.core.lib.Store
import tech.stit.mobile.core.observability.LogLevel
import kotlin.test.assertFalse

@RunWith(AndroidJUnit4::class)
class SentryCrashReporterTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val appInfo = AppInfo("tech.stit.mobile", "1.0.0", 1_000_000, "Android 16", "Pixel 8", Store.Direct)

    @Test
    fun `пустой DSN — SDK выключен, вызовы безопасны`() {
        val config = AppConfig.parse("dev", "https://api.dev.invalid", "real", "bearer", "app.dev.invalid", "stit", "")
        SentryCrashReporter.init(context, config, appInfo)
        assertFalse(Sentry.isEnabled())

        val reporter = SentryCrashReporter()
        reporter.breadcrumb("nav", "Route.TodoList")
        reporter.captureError(IllegalStateException("boom"), requestId = "req-1")
        reporter.setUserId("u-1")
        reporter.setUserId(null)
        val sink = SentryLogSink()
        LogLevel.entries.forEach { sink.log(it, "Test", "message", null) }
    }
}
