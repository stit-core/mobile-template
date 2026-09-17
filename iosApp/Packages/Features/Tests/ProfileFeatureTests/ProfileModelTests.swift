import SharedBridge
import TestSupport
import Testing

@testable import ProfileFeature

@Suite("ProfileModel")
struct ProfileModelTests {
  @Test("authenticated: профиль загружается, черновик имени редактируется")
  func loadsUser() async throws {
    let container = try await Scenario.container(.authenticated)
    let model = ProfileModel(presenter: container.profilePresenter())
    model.start()
    try await waitUntil("user") { model.state.user != nil }
    model.nameChanged("Новое имя")
    try await waitUntil("canSaveName") { model.state.canSaveName }
  }
}
