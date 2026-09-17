package tech.stit.mobile.core.observability

enum class LogLevel {
    Debug,
    Info,
    Warn,
    Error,
}

// Приёмник логов даёт платформа (Logcat, os.Logger, Sentry-breadcrumbs в release); android.util.Log и print в коде запрещены.
interface LogSink {
    fun log(level: LogLevel, tag: String, message: String, throwable: Throwable?)
}

object NoopLogSink : LogSink {
    override fun log(level: LogLevel, tag: String, message: String, throwable: Throwable?) = Unit
}

class Logger(private val tag: String, private val sink: LogSink) {
    fun debug(message: String) = sink.log(LogLevel.Debug, tag, message, null)

    fun info(message: String) = sink.log(LogLevel.Info, tag, message, null)

    fun warn(message: String, throwable: Throwable? = null) = sink.log(LogLevel.Warn, tag, message, throwable)

    fun error(message: String, throwable: Throwable? = null) = sink.log(LogLevel.Error, tag, message, throwable)
}
