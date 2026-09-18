public import AppIntents

// Задача, заведённая голосом или автоматизацией: экрана нет, ответ — подтверждение с заголовком задачи.
nonisolated public struct AddTodoIntent: AppIntent {
  public static var title: LocalizedStringResource { .action("action.add-todo.title") }
  public static var description: IntentDescription { IntentDescription(.action("action.add-todo.description")) }
  public static let openAppWhenRun = false
  // Задача уходит в список пользователя, поэтому на заблокированном экране система сначала просит разблокировку.
  public static var authenticationPolicy: IntentAuthenticationPolicy { .requiresAuthentication }

  @Parameter(
    title: .action("action.add-todo.parameter.text"),
    requestValueDialog: IntentDialog(.action("action.add-todo.ask_text"))
  )
  public var text: String

  public init() {}

  public init(text: String) {
    self.text = text
  }

  public func perform() async throws -> some IntentResult & ProvidesDialog {
    let response = await SystemActions.addTodo(text: text)
    // Текст ответа уже собран из каталога в рантайме — диалог строится из готовой строки, а не из ключа.
    return .result(dialog: IntentDialog(stringLiteral: response.text))
  }
}
