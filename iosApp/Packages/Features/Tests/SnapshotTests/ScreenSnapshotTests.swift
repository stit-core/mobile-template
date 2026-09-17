import Features
import SharedBridge
import SnapshotTesting
import TestSupport
import Testing

// Эталоны — __Snapshots__/ рядом; запись — make snapshot-record на симуляторе из iosApp/.test-simulator.
@Suite("Снапшоты экранов", .serialized, .snapshots(record: .missing))
struct ScreenSnapshotTests {
  @Test("login: пустая форма")
  func loginEmpty() async throws {
    let container = try await Scenario.container(.populated)
    Snapshots.assertScreen(LoginView(model: LoginModel(presenter: container.loginPresenter())), named: "login-empty")
  }

  @Test("todos: populated")
  func todosPopulated() async throws {
    let container = try await Scenario.container(.populated)
    let model = TodoListModel(presenter: container.todoListPresenter())
    model.start()
    try await waitUntil("todos") { !model.state.todos.isEmpty }
    Snapshots.assertScreen(TodoListView(model: model), named: "todos-populated")
  }

  @Test("todos: empty")
  func todosEmpty() async throws {
    let container = try await Scenario.container(.empty)
    let model = TodoListModel(presenter: container.todoListPresenter())
    model.start()
    try await waitUntil("loaded") { !model.state.loading }
    Snapshots.assertScreen(TodoListView(model: model), named: "todos-empty")
  }

  @Test("settings: populated")
  func settings() async throws {
    let container = try await Scenario.container(.populated)
    let view = SettingsView(
      model: SettingsModel(presenter: container.settingsPresenter()),
      pushOptIn: PushOptInModel(presenter: container.pushOptInPresenter()),
      demoPanel: DemoPanelModel(presenter: container.demoPanelPresenter())
    )
    Snapshots.assertScreen(view, named: "settings")
  }

  @Test("delete account: до подтверждения")
  func deleteAccount() async throws {
    let container = try await Scenario.container(.authenticated)
    Snapshots.assertScreen(
      DeleteAccountView(model: DeleteAccountModel(presenter: container.deleteAccountPresenter())),
      named: "delete-account")
  }
}
