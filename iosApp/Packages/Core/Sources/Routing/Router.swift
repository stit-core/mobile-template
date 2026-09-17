public import Observation

// Единственный владелец навигационного состояния: у каждого таба свой стек, NavigationStack таба биндится через subscript.
@Observable
public final class Router {
  public var tab: AppTab = .todos
  public var paths: [AppTab: [AppRoute]] = [:]

  public init() {}

  public subscript(tab: AppTab) -> [AppRoute] {
    get { paths[tab, default: []] }
    set { paths[tab] = newValue }
  }

  // Deep link / pending route из ShellState: таб и его стек меняются одним присваиванием — NavigationStack не любит правку пути посреди анимации.
  public func open(_ route: AppRoute) {
    guard let target = route.tab else {
      self[tab] = []
      return
    }
    tab = target
    switch route {
    case .todoDetail, .sessions, .deleteAccount: self[target] = [route]
    case .home, .login, .profile, .settings, .todoList: self[target] = []
    }
  }

  public func push(_ route: AppRoute) {
    self[tab].append(route)
  }

  public func pop() {
    _ = self[tab].popLast()
  }

  public func popToRoot() {
    self[tab] = []
  }
}
