public import SharedBridge
import SystemActions

// Единственное место, где системные действия встречаются с контейнером: правило остаётся в shared, канал — в SystemActions,
// а статический Shared.framework не уезжает в расширение виджетов вместе с типом интента.
nonisolated public struct SharedSystemActionRunner: SystemActionRunner {
  private let container: SharedContainer

  public init(container: SharedContainer) {
    self.container = container
  }

  // Composition root регистрирует исполнителя, не зная про таргет интентов.
  public static func bind(container: SharedContainer) {
    SystemActions.bind(runner: Self(container: container))
  }

  public func completeTodo(id: String) async -> SystemActionOutcome {
    switch try? await container.completeTodoAction().run(todoId: id) {
    case let done as CompleteTodoResultDone:
      return .done(title: done.title)
    case let already as CompleteTodoResultAlreadyDone:
      return .alreadyDone(title: already.title)
    case is CompleteTodoResultSignedOut:
      return .signedOut
    default:
      return .failed
    }
  }

  public func addTodo(text: String) async -> SystemActionOutcome {
    switch try? await container.addTodoAction().run(text: text) {
    case let added as AddTodoResultAdded:
      return .added(title: added.title)
    case is AddTodoResultSignedOut:
      return .signedOut
    default:
      return .failed
    }
  }
}
