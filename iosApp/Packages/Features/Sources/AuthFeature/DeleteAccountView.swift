import DesignSystem
import SharedBridge
public import SwiftUI

// App Store 5.1.1(v): удаление аккаунта — экран в приложении, достижимый из настроек.
public struct DeleteAccountView: View {
  @Environment(\.theme) private var theme
  private let model: DeleteAccountModel

  public init(model: DeleteAccountModel) {
    self.model = model
  }

  public var body: some View {
    let state = model.state
    VStack(alignment: .leading, spacing: theme.metrics.spaceMd) {
      Text("auth.delete_account.warning", bundle: .module)
        .font(theme.typography.bodyMd)
      Toggle(isOn: Binding(get: { state.confirmed }, set: model.confirmChanged)) {
        Text("auth.delete_account.confirm", bundle: .module)
      }
      .accessibilityIdentifier("delete_account.confirm")
      if let error = state.error {
        ErrorText(messageKey: error.localizationKey)
      }
      Button(role: .destructive) {
        model.delete()
      } label: {
        if state.deleting {
          ProgressView().tint(theme.colors.actionFg)
        } else {
          Text("auth.delete_account.action", bundle: .module)
        }
      }
      .buttonStyle(PrimaryButtonStyle())
      .disabled(!state.canDelete || state.deleting)
      .accessibilityIdentifier("delete_account.action")
      Spacer()
    }
    .padding(theme.metrics.spaceLg)
    .navigationTitle(Text("auth.delete_account.title", bundle: .module))
  }
}
