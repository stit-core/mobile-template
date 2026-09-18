import Testing

@testable import AppFoundation

@Suite("BackgroundTaskIdentifier")
struct BackgroundTaskIdentifierTests {
  let bundleId = "tech.stit.mobile.dev"

  @Test("slug задачи превращается в идентификатор планировщика")
  func qualifies() {
    #expect(
      BackgroundTaskIdentifier.qualified(bundleId: bundleId, taskId: "refresh-todos")
        == "tech.stit.mobile.dev.refresh-todos")
  }

  @Test("идентификатор планировщика возвращает slug задачи")
  func parsesBack() {
    let qualified = BackgroundTaskIdentifier.qualified(bundleId: bundleId, taskId: "refresh-todos")
    #expect(BackgroundTaskIdentifier.taskId(from: qualified, bundleId: bundleId) == "refresh-todos")
  }

  @Test("ключ задачи на запись переживает оборот целиком")
  func keepsRecordKey() {
    let qualified = BackgroundTaskIdentifier.qualified(bundleId: bundleId, taskId: "todo-reminder:42")
    #expect(BackgroundTaskIdentifier.taskId(from: qualified, bundleId: bundleId) == "todo-reminder:42")
  }

  @Test("чужой или пустой префикс не даёт slug")
  func rejectsForeign() {
    #expect(BackgroundTaskIdentifier.taskId(from: "tech.stit.other.refresh-todos", bundleId: bundleId) == nil)
    #expect(BackgroundTaskIdentifier.taskId(from: bundleId, bundleId: bundleId) == nil)
    #expect(BackgroundTaskIdentifier.taskId(from: "\(bundleId).", bundleId: bundleId) == nil)
  }
}
