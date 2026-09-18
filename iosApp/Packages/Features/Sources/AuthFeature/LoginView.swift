import DesignSystem
import SharedBridge
public import SwiftUI
import UIKit

public struct LoginView: View {
  @Environment(\.theme) private var theme
  private let model: LoginModel

  public init(model: LoginModel) {
    self.model = model
  }

  public var body: some View {
    let state = model.state
    VStack(spacing: theme.metrics.spaceMd) {
      Text("auth.login.title", bundle: .module)
        .font(theme.typography.headingLg)
      TextField(String(localized: "auth.login.email", bundle: .module), text: binding(state.email, model.emailChanged))
        .textContentType(.username)
        .keyboardType(.emailAddress)
        .textInputAutocapitalization(.never)
        .accessibilityIdentifier("login.email")
      SecureField(
        String(localized: "auth.login.password", bundle: .module), text: binding(state.password, model.passwordChanged)
      )
      .textContentType(.password)
      .accessibilityIdentifier("login.password")
      if let error = state.error {
        ErrorText(messageKey: error.localizationKey)
      }
      Button {
        model.submit()
      } label: {
        if state.submitting {
          ProgressView().tint(theme.colors.actionFg)
        } else {
          Text("auth.login.submit", bundle: .module)
        }
      }
      .buttonStyle(PrimaryButtonStyle())
      .disabled(!state.canSubmit || state.submitting)
      .accessibilityIdentifier("login.submit")
      // Вход для ревьюера магазина: показывается, только когда бэкенд разрешил флагом demo_login.
      if state.demoAvailable {
        Button {
          model.enterDemo()
        } label: {
          Text("auth.login.demo", bundle: .module)
        }
        .disabled(state.submitting)
        .accessibilityIdentifier("login.demo")
      }
    }
    .task { model.start() }
    .textFieldStyle(.roundedBorder)
    .padding(theme.metrics.spaceLg)
    .frame(maxWidth: .infinity, maxHeight: .infinity)
    .background(theme.colors.appBg)
  }

  private func binding(_ value: String, _ update: @escaping (String) -> Void) -> Binding<String> {
    Binding(get: { value }, set: update)
  }
}
