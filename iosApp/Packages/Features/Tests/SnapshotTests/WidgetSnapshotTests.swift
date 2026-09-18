import AppIntents
import Features
import Foundation
import SnapshotTesting
import SwiftUI
import TestSupport
import Testing
import Widgets

// Кнопке строки нужен конкретный интент: настоящий живёт в Core/SystemActions, снапшоту хватает заглушки.
nonisolated private struct TestCompleteIntent: AppIntent {
  static var title: LocalizedStringResource { "test.complete" }

  var todoId: String

  init() {
    todoId = ""
  }

  init(todoId: String) {
    self.todoId = todoId
  }

  func perform() async throws -> some IntentResult { .result() }
}

// Эталон на каждое состояние снимка: расширение других источников данных не имеет.
@Suite("Снапшоты виджетов", .serialized, .snapshots(record: .missing))
struct WidgetSnapshotTests {
  let moment = Date(timeIntervalSince1970: 1_767_614_400)

  @Test(
    "today-todos: все состояния снимка",
    arguments: [
      (WidgetSnapshotState.placeholder, "placeholder"),
      (.populated, "populated"),
      (.empty, "empty"),
      (.signedOut, "signed-out"),
      (.stale, "stale"),
    ]
  )
  func todayTodos(state: WidgetSnapshotState, name: String) {
    let view = TodayTodosWidgetView(entry: entry(state), complete: TestCompleteIntent.init(todoId:))
      .frame(width: 320, height: 160)
    Snapshots.assertScreen(view, named: "widget-today-todos-\(name)")
  }

  private func entry(_ state: WidgetSnapshotState) -> TodayTodosEntry {
    let items =
      state == .placeholder || state == .empty || state == .signedOut
      ? []
      : [
        TodayTodosItem(id: "t-1", title: "Купить молоко", updatedAt: moment, done: false),
        TodayTodosItem(id: "t-2", title: "Позвонить врачу", updatedAt: moment, done: true),
      ]
    return TodayTodosEntry(
      date: moment, state: state, items: items, tapUrl: URL(string: "https://app.dev.example.com/todos"))
  }
}
