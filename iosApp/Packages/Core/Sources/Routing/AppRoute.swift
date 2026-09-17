public enum AppTab: String, CaseIterable, Hashable, Sendable {
  case todos
  case profile
  case settings
}

// Зеркало Shared.Route как enum: исчерпывающий switch в SwiftUI; маппинг из Kotlin — в SharedBridge.
public enum AppRoute: Hashable, Sendable {
  case home
  case login
  case profile
  case settings
  case todoList
  case todoDetail(id: String)
  // Экраны без deep link: достижимы только из настроек.
  case sessions
  case deleteAccount

  public var tab: AppTab? {
    switch self {
    case .home, .todoList, .todoDetail: .todos
    case .profile: .profile
    case .settings, .sessions, .deleteAccount: .settings
    case .login: nil
    }
  }
}
