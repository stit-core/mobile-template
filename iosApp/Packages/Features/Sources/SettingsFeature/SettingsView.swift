import AppFoundation
import DesignSystem
import Routing
import SharedBridge
public import SwiftUI
import UIKit

public struct SettingsView: View {
  @Environment(\.theme) private var theme
  @Environment(\.openURL) private var openURL
  @State private var showPushOptIn = false
  private let model: SettingsModel
  private let pushOptIn: PushOptInModel
  private let demoPanel: DemoPanelModel

  public init(model: SettingsModel, pushOptIn: PushOptInModel, demoPanel: DemoPanelModel) {
    self.model = model
    self.pushOptIn = pushOptIn
    self.demoPanel = demoPanel
  }

  public var body: some View {
    let state = model.state
    Form {
      Section(header: Text("settings.appearance", bundle: .module)) {
        Picker(selection: Binding(get: { state.theme }, set: model.setTheme)) {
          Text("settings.theme.system", bundle: .module).tag(ThemeMode.system)
          Text("settings.theme.light", bundle: .module).tag(ThemeMode.light)
          Text("settings.theme.dark", bundle: .module).tag(ThemeMode.dark)
        } label: {
          Text("settings.theme", bundle: .module)
        }
        .accessibilityIdentifier("settings.theme")
        // Язык на iOS переключается системно (per-app language в Настройках); своего переключателя нет.
        Button {
          if let url = URL(string: UIApplication.openSettingsURLString) { openURL(url) }
        } label: {
          Text("settings.language", bundle: .module)
        }
      }
      Section(header: Text("settings.notifications", bundle: .module)) {
        Toggle(isOn: Binding(get: { state.notificationsEnabled }, set: toggleNotifications)) {
          Text("settings.notifications.push", bundle: .module)
        }
        .accessibilityIdentifier("settings.notifications")
      }
      Section(header: Text("settings.account", bundle: .module)) {
        NavigationLink(value: AppRoute.sessions) { Text("settings.sessions", bundle: .module) }
          .accessibilityIdentifier("settings.sessions")
        NavigationLink(value: AppRoute.deleteAccount) { Text("settings.delete_account", bundle: .module) }
          .accessibilityIdentifier("settings.delete_account")
      }
      Section(header: Text("settings.about", bundle: .module)) {
        LabeledContent {
          Text(AppFormat.version(state.version, build: state.build))
        } label: {
          Text("settings.version", bundle: .module)
        }
        LabeledContent {
          Text(state.env.name.lowercased())
        } label: {
          Text("settings.environment", bundle: .module)
        }
        if let policy = state.links.privacyPolicy.flatMap(URL.init(string:)) {
          Link(destination: policy) { Text("settings.privacy_policy", bundle: .module) }
        }
        if let support = state.links.support.flatMap(URL.init(string:)) {
          Link(destination: support) { Text("settings.support", bundle: .module) }
        }
      }
      if demoPanel.state.available {
        DemoPanelSection(model: demoPanel)
      }
    }
    .navigationTitle(Text("settings.title", bundle: .module))
    .task {
      model.start()
      pushOptIn.start()
      demoPanel.start()
    }
    .sheet(isPresented: $showPushOptIn) {
      PushOptInView(model: pushOptIn)
    }
    .accessibilityIdentifier("settings.screen")
  }

  // Включение уведомлений идёт через pre-prompt (PushOptInView): системный диалог только после объяснения ценности.
  private func toggleNotifications(_ enabled: Bool) {
    if enabled, pushOptIn.state.status == .notAsked {
      showPushOptIn = true
    } else {
      model.setNotifications(enabled)
    }
  }
}
