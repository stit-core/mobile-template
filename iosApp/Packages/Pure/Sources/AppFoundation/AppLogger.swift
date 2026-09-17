#if canImport(os)
import os
#else
import Foundation
#endif

// Утилиты Pure без UI помечены nonisolated: их зовут из deinit и callback'ов Kotlin вне главного актора.
nonisolated public enum AppLogLevel: Int, Comparable, Sendable {
  case debug
  case info
  case warning
  case error

  public static func < (lhs: Self, rhs: Self) -> Bool { lhs.rawValue < rhs.rawValue }
}

nonisolated public protocol AppLogSink: Sendable {
  func log(_ level: AppLogLevel, category: String, message: String)
}

// os.Logger на Apple-платформах; на Linux (тесты Pure) — stderr, чтобы модуль собирался без os.
nonisolated public struct OSLogSink: AppLogSink {
  private let subsystem: String

  public init(subsystem: String) {
    self.subsystem = subsystem
  }

  public func log(_ level: AppLogLevel, category: String, message: String) {
    #if canImport(os)
    let logger = Logger(subsystem: subsystem, category: category)
    switch level {
    case .debug: logger.debug("\(message, privacy: .public)")
    case .info: logger.info("\(message, privacy: .public)")
    case .warning: logger.warning("\(message, privacy: .public)")
    case .error: logger.error("\(message, privacy: .public)")
    }
    #else
    let line = "[\(subsystem)] \(level) \(category): \(message)\n"
    FileHandle.standardError.write(Data(line.utf8))
    #endif
  }
}

nonisolated public struct AppLogger: Sendable {
  public let category: String
  private let sink: any AppLogSink
  private let minimumLevel: AppLogLevel

  public init(
    category: String,
    sink: any AppLogSink = OSLogSink(subsystem: "tech.stit.mobile"),
    minimumLevel: AppLogLevel = .debug
  ) {
    self.category = category
    self.sink = sink
    self.minimumLevel = minimumLevel
  }

  public func debug(_ message: String) { log(.debug, message) }
  public func info(_ message: String) { log(.info, message) }
  public func warning(_ message: String) { log(.warning, message) }
  public func error(_ message: String) { log(.error, message) }

  private func log(_ level: AppLogLevel, _ message: String) {
    guard level >= minimumLevel else { return }
    sink.log(level, category: category, message: message)
  }
}
