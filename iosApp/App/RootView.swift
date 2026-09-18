import DesignSystem
import Features
import Routing
import SharedBridge
import SwiftUI

// Shell: auth-gate → табы; маршрут из ShellState исполняется роутером и подтверждается RouteConsumed.
struct RootView: View {
  @Environment(AppEnvironment.self) private var env
  @Environment(\.scenePhase) private var scenePhase

  var body: some View {
    ZStack {
      VStack(spacing: 0) {
        // Баннер живёт над содержимым оболочки, а не внутри экрана: demo-режим переживает любую навигацию.
        if env.shell.state.demo {
          DemoBanner(onExit: env.shell.leaveDemo)
        }
        switch env.shell.state.auth {
        case .unknown:
          LoadingStateView()
        case .signedOut:
          Screen(env.loginModel) { LoginView(model: $0) }
        default:
          tabs()
        }
      }
      RequirementsOverlayView(model: env.requirements)
    }
    .preferredColorScheme(colorScheme(env.settings.state.theme))
    .task { await env.start() }
    .onChange(of: env.shell.state.route) { _, route in
      guard let route else { return }
      if let appRoute = AppRoute(route) { env.router.open(appRoute) }
      env.shell.routeConsumed()
    }
    .onChange(of: scenePhase) { _, phase in
      if phase == .active { env.requirements.foreground() }
      // Уход с экрана — момент, когда снимок виджета обязан догнать данные: сессия уже известна, а виджет вот-вот покажут.
      if phase == .background { env.publishWidgets() }
    }
  }

  private func tabs() -> some View {
    @Bindable var router = env.router
    return TabView(selection: $router.tab) {
      Tab(
        value: AppTab.todos,
        content: {
          NavigationStack(path: path(.todos)) {
            Screen(env.todoListModel) { TodoListView(model: $0) }
              .navigationDestination(for: AppRoute.self, destination: destination)
          }
        },
        label: {
          Label {
            Text("shell.tab.todos")
          } icon: {
            Image(systemName: "checklist")
          }
        }
      )
      Tab(
        value: AppTab.profile,
        content: { NavigationStack { Screen(env.profileModel) { ProfileView(model: $0) } } },
        label: {
          Label {
            Text("shell.tab.profile")
          } icon: {
            Image(systemName: "person")
          }
        }
      )
      Tab(
        value: AppTab.settings,
        content: {
          NavigationStack(path: path(.settings)) {
            Screen(
              { SettingsScreenModels(env: env) },
              content: { models in
                SettingsView(model: env.settings, pushOptIn: models.pushOptIn, demoPanel: models.demoPanel)
              }
            )
            .navigationDestination(for: AppRoute.self, destination: destination)
          }
        },
        label: {
          Label {
            Text("shell.tab.settings")
          } icon: {
            Image(systemName: "gearshape")
          }
        }
      )
    }
  }

  // NavigationStack каждого таба владеет только своим стеком: пуш в todos не проявится в settings.
  private func path(_ tab: AppTab) -> Binding<[AppRoute]> {
    Binding(get: { env.router[tab] }, set: { env.router[tab] = $0 })
  }

  @ViewBuilder
  private func destination(_ route: AppRoute) -> some View {
    switch route {
    case .todoDetail(let id):
      Screen({ env.todoDetailModel(id: id) }, content: { TodoDetailView(model: $0) })
    case .sessions:
      Screen(env.sessionsModel) { SessionsView(model: $0) }
    case .deleteAccount:
      Screen(env.deleteAccountModel) { DeleteAccountView(model: $0) }
    case .home, .login, .profile, .settings, .todoList:
      EmptyView()
    }
  }

  private func colorScheme(_ theme: ThemeMode) -> ColorScheme? {
    switch theme {
    case .light: .light
    case .dark: .dark
    default: nil
    }
  }
}

// Модели экрана Settings, которые живут вместе с ним, а не с приложением.
final class SettingsScreenModels {
  let pushOptIn: PushOptInModel
  let demoPanel: DemoPanelModel

  init(env: AppEnvironment) {
    pushOptIn = env.pushOptInModel()
    demoPanel = env.demoPanelModel()
  }
}

// Модель экрана создаётся один раз при появлении экрана и уходит вместе с ним (deinit закрывает презентер).
struct Screen<Model: AnyObject, Content: View>: View {
  @State private var model: Model?
  private let make: () -> Model
  private let content: (Model) -> Content

  init(_ make: @escaping () -> Model, @ViewBuilder content: @escaping (Model) -> Content) {
    self.make = make
    self.content = content
  }

  var body: some View {
    Group {
      if let model {
        content(model)
      } else {
        LoadingStateView()
      }
    }
    .task { if model == nil { model = make() } }
  }
}
