import SharedBridge
import TestSupport
import Testing

@testable import AuthFeature

// Модель — тонкий адаптер: тесты проверяют проводку интентов к презентеру на MockTransport (сценарии mock/).
@Suite("LoginModel")
struct LoginModelTests {
  @Test("пустая форма не отправляется, заполненная — да")
  func canSubmitFollowsInput() async throws {
    let container = try await Scenario.container(.populated)
    let model = LoginModel(presenter: container.loginPresenter())
    #expect(!model.state.canSubmit)
    model.emailChanged("user@example.com")
    model.passwordChanged("secret")
    try await waitUntil("canSubmit") { model.state.canSubmit }
    #expect(model.state.email == "user@example.com")
  }

  @Test("submit в demo-режиме завершается signedIn")
  func submitSignsIn() async throws {
    let container = try await Scenario.container(.populated)
    let model = LoginModel(presenter: container.loginPresenter())
    model.emailChanged("user@example.com")
    model.passwordChanged("secret")
    try await waitUntil("canSubmit") { model.state.canSubmit }
    model.submit()
    try await waitUntil("signedIn") { model.state.signedIn }
    #expect(model.state.error == nil)
  }

  // Вход в demo открывает флаг requirements.demo_login обычной сборки; сама demo-схема его не показывает.
  @Test("в demo-схеме вход в demo не предлагается и ничего не переключает")
  func demoEntryHiddenInTheDemoBuild() async throws {
    let container = try await Scenario.container(.populated)
    let model = LoginModel(presenter: container.loginPresenter())
    model.start()
    #expect(!model.state.demoAvailable)
    model.enterDemo()
    try await waitUntil("idle") { !model.state.submitting }
    #expect(!model.state.demoAvailable)
  }

  @Test("сценарий error даёт MessageKey ошибки")
  func errorScenarioShowsError() async throws {
    let container = try await Scenario.container(.error)
    let model = LoginModel(presenter: container.loginPresenter())
    model.emailChanged("user@example.com")
    model.passwordChanged("secret")
    try await waitUntil("canSubmit") { model.state.canSubmit }
    model.submit()
    try await waitUntil("error") { model.state.error != nil }
    #expect(model.state.error?.value.hasPrefix("error.") == true)
  }
}
