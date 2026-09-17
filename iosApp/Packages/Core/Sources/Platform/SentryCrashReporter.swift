import Sentry
import SharedBridge

// Пустой DSN — SDK не стартует (dev/demo); PII не уходит: sendDefaultPii = false, auth-заголовки режутся в beforeSend.
nonisolated public final class SentryCrashReporter: TelemetrySink {
  public init(dsn: String, environment: String, release: String) {
    guard !dsn.isEmpty else { return }
    SentrySDK.start { options in
      options.dsn = dsn
      options.environment = environment
      options.releaseName = release
      options.sendDefaultPii = false
      options.attachScreenshot = false
      options.beforeSend = { event in
        event.request?.headers?.removeValue(forKey: "Authorization")
        event.request?.headers?.removeValue(forKey: "Cookie")
        return event
      }
    }
  }

  public func breadcrumb(category: String, message: String) {
    guard SentrySDK.isEnabled else { return }
    let crumb = Breadcrumb(level: .info, category: category)
    crumb.message = message
    SentrySDK.addBreadcrumb(crumb)
  }

  public func captureError(throwable: KotlinThrowable, requestId: String?) {
    guard SentrySDK.isEnabled else { return }
    SentrySDK.capture(error: throwable.asError()) { scope in
      if let requestId { scope.setTag(value: requestId, key: "request_id") }
    }
  }

  public func setUserId(id: String?) {
    guard SentrySDK.isEnabled else { return }
    SentrySDK.setUser(id.map { User(userId: $0) })
  }
}
