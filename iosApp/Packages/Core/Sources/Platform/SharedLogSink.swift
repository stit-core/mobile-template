import AppFoundation
import SharedBridge

// Логи Shared идут в тот же os.Logger, что и Swift-код: одна лента в Console.app.
nonisolated public final class SharedLogSink: LogSink {
  private let subsystem: String

  public init(subsystem: String) {
    self.subsystem = subsystem
  }

  public func log(level: LogLevel, tag: String, message: String, throwable: KotlinThrowable?) {
    let logger = AppLogger(category: tag, sink: OSLogSink(subsystem: subsystem))
    let text = throwable.map { "\(message) — \($0.message ?? String(describing: $0))" } ?? message
    switch level {
    case .debug: logger.debug(text)
    case .info: logger.info(text)
    case .warn: logger.warning(text)
    case .error: logger.error(text)
    default: logger.info(text)
    }
  }
}
