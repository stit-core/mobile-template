public import AppIntents
import Foundation

// Кнопка «Готово» виджета и уведомления: приложение не открывается, решение принимает CompleteTodoAction в shared.
nonisolated public struct CompleteTodoIntent: AppIntent {
  public static var title: LocalizedStringResource { .action("action.complete-todo.title") }
  public static let openAppWhenRun = false
  // Действие адресует конкретную задачу и в каталоге Shortcuts само по себе бесполезно: его показывают виджет и уведомление.
  public static let isDiscoverable = false
  // Отметка меняет данные пользователя, поэтому на заблокированном экране система сначала просит разблокировку.
  public static var authenticationPolicy: IntentAuthenticationPolicy { .requiresAuthentication }

  @Parameter(title: .action("action.complete-todo.parameter.todo"))
  public var todoId: String

  public init() {}

  public init(todoId: String) {
    self.todoId = todoId
  }

  public func perform() async throws -> some IntentResult & ProvidesDialog {
    let response = await SystemActions.completeTodo(id: todoId)
    // Текст ответа уже собран из каталога в рантайме — диалог строится из готовой строки, а не из ключа.
    return .result(dialog: IntentDialog(stringLiteral: response.text))
  }
}

extension LocalizedStringResource {
  // Каталог строк таргета лежит в бандле пакета: LocalizedStringResource ищет строки только по URL бандла.
  static func action(_ key: String) -> Self {
    Self(String.LocalizationValue(stringLiteral: key), bundle: .atURL(Bundle.module.bundleURL))
  }
}
