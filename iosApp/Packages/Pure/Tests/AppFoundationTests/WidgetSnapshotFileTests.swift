import Foundation
import Testing

@testable import AppFoundation

@Suite("WidgetSnapshotFile")
struct WidgetSnapshotFileTests {
  let published = Date(timeIntervalSince1970: 1_767_614_400)

  @Test("путь совпадает с именем кеша в shared")
  func path() {
    #expect(WidgetSnapshotFile.relativePath(slug: "today-todos") == "cache/widget-today-todos.json")
  }

  @Test("свежий снимок не устарел")
  func fresh() {
    #expect(!WidgetSnapshotFile.isStale(generatedAt: published, now: published.addingTimeInterval(59 * 60)))
  }

  @Test("снимок старше порога устарел")
  func stale() {
    #expect(WidgetSnapshotFile.isStale(generatedAt: published, now: published.addingTimeInterval(61 * 60)))
  }

  @Test("на самой границе снимок ещё свеж")
  func border() {
    #expect(
      !WidgetSnapshotFile.isStale(
        generatedAt: published, now: published.addingTimeInterval(WidgetSnapshotFile.freshFor)))
  }

  @Test("часы устройства могли уйти назад — снимок из будущего не устарел")
  func clockSkew() {
    #expect(!WidgetSnapshotFile.isStale(generatedAt: published, now: published.addingTimeInterval(-3600)))
  }
}
