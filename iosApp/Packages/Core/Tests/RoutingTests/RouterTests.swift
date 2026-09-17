import Testing

@testable import Routing

@Suite("Router")
struct RouterTests {
  @Test("deep link на деталь: таб todos, стек settings не тронут")
  func opensDetail() {
    let router = Router()
    router.tab = .settings
    router.push(.sessions)
    router.open(.todoDetail(id: "7"))
    #expect(router.tab == .todos)
    #expect(router[.todos] == [.todoDetail(id: "7")])
    #expect(router[.settings] == [.sessions])
  }

  @Test("корневой маршрут переключает таб и очищает только его стек")
  func opensRoot() {
    let router = Router()
    router.push(.todoDetail(id: "1"))
    router.tab = .settings
    router.push(.deleteAccount)
    router.open(.settings)
    #expect(router.tab == .settings)
    #expect(router[.settings].isEmpty)
    #expect(router[.todos] == [.todoDetail(id: "1")])
    router.open(.home)
    #expect(router.tab == .todos)
    #expect(router[.todos].isEmpty)
  }

  @Test("login не трогает таб, только очищает его стек")
  func loginKeepsTab() {
    let router = Router()
    router.tab = .settings
    router.push(.sessions)
    router.open(.login)
    #expect(router.tab == .settings)
    #expect(router[.settings].isEmpty)
  }

  @Test("экраны настроек открываются стеком в табе settings")
  func opensSettingsScreens() {
    let router = Router()
    router.open(.deleteAccount)
    #expect(router.tab == .settings)
    #expect(router[.settings] == [.deleteAccount])
  }

  @Test("push / pop / popToRoot действуют только на текущий таб")
  func stackOperations() {
    let router = Router()
    router.push(.todoDetail(id: "1"))
    router.push(.todoDetail(id: "2"))
    #expect(router[.todos].count == 2)
    router.pop()
    #expect(router[.todos] == [.todoDetail(id: "1")])
    router.tab = .settings
    router.push(.sessions)
    router.popToRoot()
    router.pop()
    #expect(router[.settings].isEmpty)
    #expect(router[.todos] == [.todoDetail(id: "1")])
  }

  @Test("стек таба без истории пуст")
  func emptyByDefault() {
    #expect(Router()[.profile].isEmpty)
  }
}

@Suite("AppRoute")
struct AppRouteTests {
  @Test(
    "маршрут → таб",
    arguments: [
      (AppRoute.home, AppTab?.some(.todos)), (.todoList, .todos), (.todoDetail(id: "x"), .todos),
      (.profile, .profile), (.settings, .settings), (.sessions, .settings), (.deleteAccount, .settings), (.login, nil),
    ])
  func tabMapping(route: AppRoute, tab: AppTab?) {
    #expect(route.tab == tab)
  }

  @Test("равенство учитывает идентификатор")
  func equality() {
    #expect(AppRoute.todoDetail(id: "1") != AppRoute.todoDetail(id: "2"))
    #expect(AppRoute.todoDetail(id: "1") == AppRoute.todoDetail(id: "1"))
  }
}
