public import Foundation
import SharedBridge

// Keychain переживает удаление приложения: refresh-токен прошлой установки стирается по флагу первого запуска.
nonisolated public struct FirstLaunchReset {
  static let flagKey = "app.first_launch_done"
  private let defaults: UserDefaults

  public init(defaults: UserDefaults = .standard) {
    self.defaults = defaults
  }

  public func run(secureStore: any SecureStore) async throws {
    guard !defaults.bool(forKey: Self.flagKey) else { return }
    try await secureStore.remove(key: SessionStore.companion.REFRESH_TOKEN_KEY)
    defaults.set(true, forKey: Self.flagKey)
  }
}
