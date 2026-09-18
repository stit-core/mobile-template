public import Foundation

#if canImport(WidgetKit)
public import WidgetKit
#endif

// Зеркало core/widget/WidgetState из shared: enum сериализуется именем, поэтому расширение читает те же пять значений.
nonisolated public enum WidgetSnapshotState: String, Codable, Sendable {
  case placeholder = "Placeholder"
  case populated = "Populated"
  case empty = "Empty"
  case signedOut = "SignedOut"
  case stale = "Stale"
}

nonisolated public struct TodayTodosItem: Codable, Equatable, Identifiable, Sendable {
  // Кнопка строки адресует конкретную задачу — без её id действие вне приложения не выполнить.
  public let id: String
  public let title: String
  public let updatedAt: Date
  public let done: Bool

  public init(id: String, title: String, updatedAt: Date, done: Bool) {
    self.id = id
    self.title = title
    self.updatedAt = updatedAt
    self.done = done
  }
}

// Запись таймлайна виджета: расширение собирает её из снимка App Group и ни репозитория, ни сети не знает.
nonisolated public struct TodayTodosEntry: Equatable, Sendable {
  public static let kind = "today-todos"

  // Момент, которым датирован снимок: по нему расширение решает и что показывать, и когда перечитать запись.
  public let date: Date
  public let state: WidgetSnapshotState
  public let items: [TodayTodosItem]
  public let tapUrl: URL?

  public init(date: Date, state: WidgetSnapshotState, items: [TodayTodosItem] = [], tapUrl: URL? = nil) {
    self.date = date
    self.state = state
    self.items = items
    self.tapUrl = tapUrl
  }

  // До первой публикации снимка в контейнере нет ничего, и системе всё равно нужна запись для превью.
  public static func placeholder(date: Date = .now) -> Self {
    Self(date: date, state: .placeholder)
  }
}

#if canImport(WidgetKit)
extension TodayTodosEntry: TimelineEntry {}
#endif
