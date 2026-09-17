import Synchronization
import Testing

@testable import AppFoundation

nonisolated final class RecordingLogSink: AppLogSink {
  struct Entry: Equatable, Sendable {
    let level: AppLogLevel
    let category: String
    let message: String
  }

  private let storage = Mutex<[Entry]>([])

  var entries: [Entry] { storage.withLock { $0 } }

  func log(_ level: AppLogLevel, category: String, message: String) {
    storage.withLock { $0.append(Entry(level: level, category: category, message: message)) }
  }
}

@Suite("AppLogger")
struct AppLoggerTests {
  @Test("уровни ниже порога не доходят до sink")
  func filtersByLevel() {
    let sink = RecordingLogSink()
    let logger = AppLogger(category: "net", sink: sink, minimumLevel: .info)
    logger.debug("скрыто")
    logger.info("видно")
    logger.warning("видно")
    logger.error("видно")
    #expect(sink.entries.map(\.level) == [.info, .warning, .error])
    #expect(sink.entries.allSatisfy { $0.category == "net" })
    #expect(sink.entries.map(\.message) == ["видно", "видно", "видно"])
  }

  @Test("порядок уровней")
  func levelOrder() {
    #expect(AppLogLevel.debug < .info)
    #expect(AppLogLevel.info < .warning)
    #expect(AppLogLevel.warning < .error)
  }

  @Test("OSLogSink принимает все уровни без падений")
  func osLogSink() {
    let sink = OSLogSink(subsystem: "tech.stit.mobile.tests")
    for level in [AppLogLevel.debug, .info, .warning, .error] {
      sink.log(level, category: "tests", message: "ok")
    }
    let logger = AppLogger(category: "tests", sink: sink)
    logger.debug("default sink")
  }
}
