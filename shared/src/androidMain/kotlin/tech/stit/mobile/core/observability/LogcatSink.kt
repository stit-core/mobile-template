package tech.stit.mobile.core.observability

import android.util.Log

// Единственное место с android.util.Log: сюда Logger пишет в debug-сборках, в release платформа ставит Sentry-breadcrumbs.
class LogcatSink : LogSink {
    override fun log(level: LogLevel, tag: String, message: String, throwable: Throwable?) {
        when (level) {
            LogLevel.Debug -> Log.d(tag, message, throwable)
            LogLevel.Info -> Log.i(tag, message, throwable)
            LogLevel.Warn -> Log.w(tag, message, throwable)
            LogLevel.Error -> Log.e(tag, message, throwable)
        }
    }
}
