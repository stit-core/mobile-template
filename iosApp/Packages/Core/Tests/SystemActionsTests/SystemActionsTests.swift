import Testing

@testable import SystemActions

nonisolated private struct FakeRunner: SystemActionRunner {
  let outcome: SystemActionOutcome

  func completeTodo(id: String) async -> SystemActionOutcome { outcome }
  func addTodo(text: String) async -> SystemActionOutcome { outcome }
}

// Реестр исполнителя — общий для процесса, поэтому suite не параллелится.
@Suite("SystemActions", .serialized)
struct SystemActionsTests {
  @Test("отметка подтверждается заголовком задачи из каталога строк")
  func done() async {
    SystemActions.bind(runner: FakeRunner(outcome: .done(title: "Купить молоко")))
    let response = await SystemActions.completeTodo(id: "t-1")
    #expect(response.succeeded)
    #expect(response.text.contains("Купить молоко"))
  }

  @Test("повторное нажатие не открывает задачу заново и остаётся успехом")
  func alreadyDone() async {
    SystemActions.bind(runner: FakeRunner(outcome: .alreadyDone(title: "Купить молоко")))
    #expect(await SystemActions.completeTodo(id: "t-1").succeeded)
  }

  @Test("без сессии канал отвечает «войдите», а не выполняет действие")
  func signedOut() async {
    SystemActions.bind(runner: FakeRunner(outcome: .signedOut))
    let response = await SystemActions.addTodo(text: "Купить молоко")
    #expect(!response.succeeded)
    #expect(response.text != "action.add-todo.signed_out")
  }

  @Test("ошибка не обещает выполнения")
  func failed() async {
    SystemActions.bind(runner: FakeRunner(outcome: .failed))
    let response = await SystemActions.addTodo(text: "Купить молоко")
    #expect(!response.succeeded)
    #expect(!response.title.isEmpty)
  }

  @Test("оба действия исполняются без открытия приложения")
  func headless() {
    #expect(!CompleteTodoIntent.openAppWhenRun)
    #expect(!AddTodoIntent.openAppWhenRun)
  }

  @Test("отметка задачи не попадает в каталог Shortcuts: её показывают виджет и уведомление")
  func notDiscoverable() {
    #expect(!CompleteTodoIntent.isDiscoverable)
  }
}
