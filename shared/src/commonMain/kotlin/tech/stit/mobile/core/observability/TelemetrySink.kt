package tech.stit.mobile.core.observability

// Шов observability (07 §7): платформа подключает Sentry, пустой DSN = NoopTelemetrySink. Тел запросов и PII здесь нет.
interface TelemetrySink {
    fun breadcrumb(category: String, message: String)

    fun captureError(throwable: Throwable, requestId: String?)

    fun setUserId(id: String?)
}

object NoopTelemetrySink : TelemetrySink {
    override fun breadcrumb(category: String, message: String) = Unit

    override fun captureError(throwable: Throwable, requestId: String?) = Unit

    override fun setUserId(id: String?) = Unit
}
