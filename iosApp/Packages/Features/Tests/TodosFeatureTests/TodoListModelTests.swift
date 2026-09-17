import SharedBridge
import TestSupport
import Testing

@testable import TodosFeature

@Suite("TodoListModel")
struct TodoListModelTests {
  @Test("populated: после Start список из фикстуры")
  func startLoadsFixture() async throws {
    let container = try await Scenario.container(.populated)
    let model = TodoListModel(presenter: container.todoListPresenter())
    model.start()
    try await waitUntil("todos") { !model.state.todos.isEmpty }
    #expect(!model.state.loading)
  }

  @Test("empty: пустой список без ошибки")
  func emptyScenario() async throws {
    let container = try await Scenario.container(.empty)
    let model = TodoListModel(presenter: container.todoListPresenter())
    model.start()
    try await waitUntil("loaded") { !model.state.loading }
    #expect(model.state.todos.isEmpty)
    #expect(model.state.error == nil)
  }

  @Test("создание добавляет задачу и очищает поле")
  func createAppends() async throws {
    let container = try await Scenario.container(.empty)
    let model = TodoListModel(presenter: container.todoListPresenter())
    model.start()
    try await waitUntil("loaded") { !model.state.loading }
    model.titleChanged("Купить молоко")
    try await waitUntil("canCreate") { model.state.canCreate }
    model.create()
    try await waitUntil("created") { model.state.todos.count == 1 }
    #expect(model.state.newTitle.isEmpty)
  }
}
