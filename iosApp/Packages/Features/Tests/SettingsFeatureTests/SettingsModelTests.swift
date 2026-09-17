import SharedBridge
import TestSupport
import Testing

@testable import SettingsFeature

@Suite("SettingsModel")
struct SettingsModelTests {
  @Test("смена темы доходит до состояния")
  func setTheme() async throws {
    let container = try await Scenario.container(.populated)
    let model = SettingsModel(presenter: container.settingsPresenter())
    model.start()
    model.setTheme(.dark)
    try await waitUntil("theme") { model.state.theme == .dark }
  }

  @Test("demo-панель доступна в mock-режиме")
  func demoPanelAvailable() async throws {
    let container = try await Scenario.container(.populated)
    let model = DemoPanelModel(presenter: container.demoPanelPresenter())
    model.start()
    try await waitUntil("available") { model.state.available }
    #expect(model.state.scenarios.count == 4)
  }
}
