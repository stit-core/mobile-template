import Foundation
import Testing

@testable import Widgets

@Suite("TodayTodosSnapshotStore")
struct TodayTodosSnapshotStoreTests {
  let published = Date(timeIntervalSince1970: 1_767_614_400)

  @Test("снимка нет — placeholder")
  func missingSnapshot() {
    let store = TodayTodosSnapshotStore(containerUrl: nil)
    #expect(store.entry(now: published).state == .placeholder)
  }

  @Test("populated: строки и адрес тапа приходят из снимка shared")
  func populated() throws {
    let store = try store(json(state: "Populated", generatedAt: "2026-01-05T12:00:00Z"))
    let entry = store.entry(now: published)
    #expect(entry.state == .populated)
    #expect(entry.items.map(\.id) == ["t-1", "t-2"])
    #expect(entry.items.first?.done == false)
    #expect(entry.tapUrl?.absoluteString == "https://app.dev.example.com/todos")
    // Запись датируется снимком, а не чтением: по этой дате расширение заказывает следующее перечитывание.
    #expect(entry.date == published)
  }

  @Test("снимок старше часа читается как stale, данные остаются")
  func stale() throws {
    let store = try store(json(state: "Populated", generatedAt: "2026-01-05T10:00:00Z"))
    let entry = store.entry(now: published)
    #expect(entry.state == .stale)
    #expect(entry.items.count == 2)
  }

  @Test("signed-out не устаревает: возраст тут ни при чём")
  func signedOut() throws {
    let store = try store(json(state: "SignedOut", generatedAt: "2026-01-04T12:00:00Z", items: "[]"))
    #expect(store.entry(now: published).state == .signedOut)
  }

  @Test("пустой свежий снимок — empty")
  func empty() throws {
    let store = try store(json(state: "Empty", generatedAt: "2026-01-05T12:00:00Z", items: "[]"))
    #expect(store.entry(now: published).state == .empty)
  }

  @Test("битый файл не ломает виджет")
  func corrupted() throws {
    let store = try store("{\"state\":")
    #expect(store.entry(now: published).state == .placeholder)
  }

  @Test("дробные секунды в Instant разбираются")
  func fractionalInstant() throws {
    let store = try store(json(state: "Populated", generatedAt: "2026-01-05T12:00:00.123Z"))
    #expect(store.entry(now: published).state == .populated)
  }

  private func json(state: String, generatedAt: String, items: String? = nil) -> String {
    let defaultItems = """
      [{"id":"t-1","title":"Купить молоко","updatedAt":"2026-01-05T11:00:00Z","done":false},
       {"id":"t-2","title":"Позвонить врачу","updatedAt":"2026-01-04T09:30:00Z","done":true}]
      """
    return """
      {"state":"\(state)","items":\(items ?? defaultItems),
       "generatedAt":"\(generatedAt)","tapUrl":"https://app.dev.example.com/todos"}
      """
  }

  private func store(_ json: String) throws -> TodayTodosSnapshotStore {
    let container = FileManager.default.temporaryDirectory
      .appendingPathComponent("widget-tests-\(UUID().uuidString)")
    let file = container.appendingPathComponent("cache/widget-today-todos.json")
    try FileManager.default.createDirectory(
      at: file.deletingLastPathComponent(), withIntermediateDirectories: true)
    try Data(json.utf8).write(to: file)
    return TodayTodosSnapshotStore(containerUrl: container)
  }
}
