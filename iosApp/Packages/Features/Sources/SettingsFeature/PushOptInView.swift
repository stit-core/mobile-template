import DesignSystem
import SharedBridge
public import SwiftUI

public struct PushOptInView: View {
  @Environment(\.theme) private var theme
  @Environment(\.dismiss) private var dismiss
  private let model: PushOptInModel

  public init(model: PushOptInModel) {
    self.model = model
  }

  public var body: some View {
    let state = model.state
    VStack(spacing: theme.metrics.spaceMd) {
      Image(systemName: "bell.badge").font(theme.typography.headingLg).foregroundStyle(theme.colors.actionBg)
      Text("settings.push.title", bundle: .module).font(theme.typography.headingLg)
      Text("settings.push.body", bundle: .module).font(theme.typography.bodyMd).multilineTextAlignment(.center)
      if let error = state.error { ErrorText(messageKey: error.localizationKey) }
      Button(action: model.accept) { Text("settings.push.accept", bundle: .module) }
        .buttonStyle(PrimaryButtonStyle())
        .disabled(state.busy)
        .accessibilityIdentifier("push.accept")
      Button(action: model.decline) { Text("settings.push.decline", bundle: .module) }
        .disabled(state.busy)
        .accessibilityIdentifier("push.decline")
    }
    .padding(theme.metrics.spaceLg)
    .onChange(of: state.status) { _, status in
      if status != .notAsked { dismiss() }
    }
  }
}
