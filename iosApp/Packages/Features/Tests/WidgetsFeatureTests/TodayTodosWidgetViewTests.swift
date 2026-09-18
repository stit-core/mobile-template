import AppIntents
import Foundation
import SwiftUI
import Testing
import UIKit
import Widgets

@testable import WidgetsFeature

// Кнопке строки нужен конкретный интент: настоящий живёт в Core/SystemActions, здесь проверяется только вью.
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

@Suite("TodayTodosWidgetView")
struct TodayTodosWidgetViewTests {
  let moment = Date(timeIntervalSince1970: 1_767_614_400)

  @Test(
    "каждое состояние снимка рисуется",
    arguments: [WidgetSnapshotState.placeholder, .populated, .empty, .signedOut, .stale]
  )
  func everyState(state: WidgetSnapshotState) {
    let host = UIHostingController(
      rootView: TodayTodosWidgetView(entry: entry(state), complete: TestCompleteIntent.init(todoId:)))
    host.loadViewIfNeeded()
    #expect(host.view != nil)
  }

  @Test("до первой публикации снимка виджет показывает placeholder без строк")
  func placeholder() {
    let entry = TodayTodosEntry.placeholder(date: moment)
    #expect(entry.state == .placeholder)
    #expect(entry.items.isEmpty)
    #expect(entry.tapUrl == nil)
  }

  private func entry(_ state: WidgetSnapshotState) -> TodayTodosEntry {
    TodayTodosEntry(
      date: moment,
      state: state,
      items: [
        TodayTodosItem(id: "t-1", title: "Купить молоко", updatedAt: moment, done: false),
        TodayTodosItem(id: "t-2", title: "Позвонить врачу", updatedAt: moment, done: true),
      ],
      tapUrl: URL(string: "https://app.dev.example.com/todos")
    )
  }
}
