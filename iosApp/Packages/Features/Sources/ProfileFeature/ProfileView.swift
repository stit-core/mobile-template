import DesignSystem
import SharedBridge
public import SwiftUI

public struct ProfileView: View {
  @Environment(\.theme) private var theme
  @State private var showPasswordSheet = false
  private let model: ProfileModel

  public init(model: ProfileModel) {
    self.model = model
  }

  public var body: some View {
    let state = model.state
    Group {
      if state.loading, state.user == nil {
        LoadingStateView()
      } else if let error = state.error, state.user == nil {
        ErrorStateView(messageKey: error.localizationKey, onRetry: model.refresh)
      } else {
        form(state)
      }
    }
    .navigationTitle(Text("profile.title", bundle: .module))
    .task { model.start() }
    .sheet(isPresented: $showPasswordSheet) {
      ChangePasswordSheet(model: model)
    }
    .accessibilityIdentifier("profile.screen")
  }

  private func form(_ state: ProfileState) -> some View {
    Form {
      Section {
        if let user = state.user {
          LabeledContent {
            Text(user.email)
          } label: {
            Text("profile.email", bundle: .module)
          }
        }
        TextField(
          String(localized: "profile.name", bundle: .module),
          text: Binding(get: { state.draftName }, set: model.nameChanged)
        )
        .accessibilityIdentifier("profile.name")
        if let error = state.error {
          ErrorText(messageKey: error.localizationKey)
        }
        Button(action: model.saveName) { Text("profile.save", bundle: .module) }
          .disabled(!state.canSaveName || state.saving)
          .accessibilityIdentifier("profile.save")
      }
      Section {
        Button {
          showPasswordSheet = true
        } label: {
          Text("profile.change_password", bundle: .module)
        }
        if state.passwordChanged {
          Text("profile.password_changed", bundle: .module).foregroundStyle(theme.colors.success)
        }
      }
      Section {
        Button(role: .destructive, action: model.logout) { Text("profile.logout", bundle: .module) }
          .accessibilityIdentifier("profile.logout")
      }
    }
    .refreshable { model.refresh() }
  }
}

private struct ChangePasswordSheet: View {
  @Environment(\.dismiss) private var dismiss
  @State private var oldPassword = ""
  @State private var newPassword = ""
  let model: ProfileModel

  var body: some View {
    NavigationStack {
      Form {
        SecureField(String(localized: "profile.old_password", bundle: .module), text: $oldPassword)
        SecureField(String(localized: "profile.new_password", bundle: .module), text: $newPassword)
      }
      .navigationTitle(Text("profile.change_password", bundle: .module))
      .toolbar {
        ToolbarItem(placement: .confirmationAction) {
          Button {
            model.changePassword(old: oldPassword, new: newPassword)
            dismiss()
          } label: {
            Text("profile.save", bundle: .module)
          }
          .disabled(oldPassword.isEmpty || newPassword.isEmpty)
        }
      }
    }
  }
}
