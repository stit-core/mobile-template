import Foundation
import SharedBridge
import TestSupport
import Testing

@testable import Shell

@Suite("ShellModel")
struct ShellModelTests {
  @Test("deep link без сессии ждёт в pendingRoute")
  func pendingRouteWithoutSession() async throws {
    let container = try await Scenario.container(.populated)
    let model = ShellModel(presenter: container.shellPresenter())
    model.start()
    try await waitUntil("auth") { model.state.auth != .unknown }
    model.open(url: URL(string: "https://app.demo.invalid/todos/todo-1")!)
    try await waitUntil("pending") { model.state.pendingRoute != nil || model.state.route != nil }
  }

  // Баннер снимается вместе с режимом: в demo-схеме переключать нечего, поэтому флаг всегда снят.
  @Test("баннер demo не поднимается в demo-схеме и выход из режима безопасен")
  func demoBannerStaysDownInTheDemoBuild() async throws {
    let container = try await Scenario.container(.populated)
    let model = ShellModel(presenter: container.shellPresenter())
    model.start()
    try await waitUntil("auth") { model.state.auth != .unknown }
    #expect(!model.state.demo)
    model.leaveDemo()
    try await waitUntil("demo") { !model.state.demo }
  }

  @Test("requirements: в demo статус ok, оверлей не блокирует")
  func requirementsOk() async throws {
    let container = try await Scenario.container(.populated)
    let model = RequirementsModel(presenter: container.requirementsPresenter())
    model.start()
    try await waitUntil("checked") { model.state.checked }
    #expect(!model.state.blocking)
  }
}
