import AppFoundation
import Foundation
import Synchronization

// Чем кончилось действие — в понятиях фичи, без Shared: тип интента виден расширению виджетов, а статический
// Shared.framework внутрь расширения не линкуется. Правило по-прежнему живёт в shared, здесь только исход.
nonisolated public enum SystemActionOutcome: Equatable, Sendable {
  case done(title: String)
  case alreadyDone(title: String)
  case added(title: String)
  case signedOut
  case failed
}

// Мост к контейнеру: реализацию поверх SharedContainer даёт Core/Platform, регистрирует её composition root.
nonisolated public protocol SystemActionRunner: Sendable {
  func completeTodo(id: String) async -> SystemActionOutcome
  func addTodo(text: String) async -> SystemActionOutcome
}

// Ответ канала пользователю: экрана у действия нет, поэтому итог показывает сам канал — диалогом интента или уведомлением.
nonisolated public struct SystemActionResponse: Equatable, Sendable {
  public let title: String
  public let text: String
  public let succeeded: Bool
}

// Интент и кнопку уведомления поднимает система, а не приложение: исполнитель отдаётся реестром, который заполняет
// composition root (аналог Hilt EntryPoint на Android). Пустой реестр — не падение, а честное «откройте приложение».
nonisolated public enum SystemActions {
  private static let registry = Mutex<(any SystemActionRunner)?>(nil)

  public static func bind(runner: any SystemActionRunner) {
    registry.withLock { $0 = runner }
  }

  public static func completeTodo(id: String) async -> SystemActionResponse {
    let title = text("action.complete-todo.title")
    guard let runner = current() else { return unavailable(title: title) }
    switch await runner.completeTodo(id: id) {
    case .done(let todo):
      return SystemActionResponse(title: title, text: text("action.complete-todo.done", todo), succeeded: true)
    case .alreadyDone(let todo):
      return SystemActionResponse(title: title, text: text("action.complete-todo.already_done", todo), succeeded: true)
    case .signedOut:
      return SystemActionResponse(title: title, text: text("action.complete-todo.signed_out"), succeeded: false)
    default:
      return SystemActionResponse(title: title, text: text("action.complete-todo.failed"), succeeded: false)
    }
  }

  public static func addTodo(text input: String) async -> SystemActionResponse {
    let title = text("action.add-todo.title")
    guard let runner = current() else { return unavailable(title: title) }
    switch await runner.addTodo(text: input) {
    case .added(let todo):
      return SystemActionResponse(title: title, text: text("action.add-todo.added", todo), succeeded: true)
    case .signedOut:
      return SystemActionResponse(title: title, text: text("action.add-todo.signed_out"), succeeded: false)
    default:
      return SystemActionResponse(title: title, text: text("action.add-todo.failed"), succeeded: false)
    }
  }

  private static func current() -> (any SystemActionRunner)? {
    registry.withLock { $0 }
  }

  // Система подняла действие мимо приложения (реестр пуст) — обещать выполнение нечем.
  private static func unavailable(title: String) -> SystemActionResponse {
    SystemActionResponse(title: title, text: text("action.unavailable"), succeeded: false)
  }

  private static func text(_ key: String, _ arguments: String...) -> String {
    AppStrings.text(key, bundle: .module, arguments: arguments)
  }
}
