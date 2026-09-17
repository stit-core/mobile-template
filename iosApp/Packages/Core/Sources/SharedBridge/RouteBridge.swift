import Routing
import Shared

extension AppRoute {
  public init?(_ route: Route) {
    switch route {
    case is RouteHome: self = .home
    case is RouteLogin: self = .login
    case is RouteProfile: self = .profile
    case is RouteSettings: self = .settings
    case is RouteTodoList: self = .todoList
    case let detail as RouteTodoDetail: self = .todoDetail(id: detail.id)
    default: return nil
    }
  }
}
