package tech.stit.mobile.android.core.platform

import android.content.Context
import io.sentry.Breadcrumb
import io.sentry.Sentry
import io.sentry.SentryLevel
import io.sentry.SentryOptions
import io.sentry.android.core.SentryAndroid
import io.sentry.protocol.User
import tech.stit.mobile.core.config.AppConfig
import tech.stit.mobile.core.lib.AppInfo
import tech.stit.mobile.core.observability.LogLevel
import tech.stit.mobile.core.observability.LogSink
import tech.stit.mobile.core.observability.TelemetrySink

// TelemetrySink над sentry-android (07 §7): пустой SENTRY_DSN — SDK не инициализируется, все вызовы становятся no-op.
class SentryCrashReporter : TelemetrySink {
    override fun breadcrumb(category: String, message: String) {
        Sentry.addBreadcrumb(Breadcrumb(message).also { it.category = category })
    }

    override fun captureError(throwable: Throwable, requestId: String?) {
        Sentry.captureException(throwable) { scope -> requestId?.let { scope.setTag("request_id", it) } }
    }

    override fun setUserId(id: String?) {
        Sentry.setUser(id?.let { User().also { user -> user.id = it } })
    }

    companion object {
        fun init(context: Context, config: AppConfig, appInfo: AppInfo) {
            if (config.sentryDsn.isBlank()) return
            SentryAndroid.init(context) { options ->
                options.dsn = config.sentryDsn
                options.environment = config.env.name.lowercase()
                // Один release-идентификатор на обеих платформах: общий дашборд GlitchTip.
                options.release = "${appInfo.appId}@${appInfo.versionWithBuild}"
                options.dist = appInfo.build.toString()
                options.isSendDefaultPii = false
                options.isAttachScreenshot = false
                options.beforeSend = SentryOptions.BeforeSendCallback { event, _ ->
                    event.request?.let { request ->
                        request.headers = request.headers?.filterKeys { it !in SENSITIVE_HEADERS }
                        request.data = null
                    }
                    event
                }
            }
        }

        private val SENSITIVE_HEADERS = setOf("Authorization", "Cookie", "Set-Cookie")
    }
}

// В release Logcat недоступен: предупреждения и ошибки становятся breadcrumbs — контекст крэша без PII.
class SentryLogSink : LogSink {
    override fun log(level: LogLevel, tag: String, message: String, throwable: Throwable?) {
        if (level < LogLevel.Warn) return
        Sentry.addBreadcrumb(
            Breadcrumb("$tag: $message").also {
                it.category = "log"
                it.level = if (level == LogLevel.Error) SentryLevel.ERROR else SentryLevel.WARNING
            },
        )
    }
}
