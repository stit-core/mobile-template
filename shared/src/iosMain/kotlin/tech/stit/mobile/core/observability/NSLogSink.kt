package tech.stit.mobile.core.observability

import platform.Foundation.NSLog

// Debug-сборки iOS: NSLog из Foundation; в release платформа подставляет os.Logger / Sentry-breadcrumbs из Swift.
class NSLogSink : LogSink {
    override fun log(level: LogLevel, tag: String, message: String, throwable: Throwable?) {
        NSLog("%@", "[${level.name.uppercase()}] $tag: $message${throwable?.let { " — $it" }.orEmpty()}")
    }
}
