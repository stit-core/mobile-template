import AppFoundation
import DesignSystem
import SharedBridge
public import SwiftUI

// Блокирующие оверлеи requirements (update_required / maintenance) и мягкая рекомендация обновиться.
public struct RequirementsOverlayView: View {
  @Environment(\.theme) private var theme
  @Environment(\.openURL) private var openURL
  private let model: RequirementsModel

  public init(model: RequirementsModel) {
    self.model = model
  }

  public var body: some View {
    let state = model.state
    if state.blocking {
      blocking(state)
    } else if state.showRecommendation {
      recommendation(state)
    }
  }

  private func blocking(_ state: RequirementsState) -> some View {
    VStack(spacing: theme.metrics.spaceMd) {
      Image(systemName: state.status == .maintenance ? "wrench.and.screwdriver" : "arrow.down.circle")
        .font(theme.typography.headingLg)
        .foregroundStyle(theme.colors.warning)
      Text(state.status == .maintenance ? "shell.maintenance.title" : "shell.update_required.title", bundle: .module)
        .font(theme.typography.headingLg)
      Text(state.status == .maintenance ? "shell.maintenance.body" : "shell.update_required.body", bundle: .module)
        .font(theme.typography.bodyMd)
        .multilineTextAlignment(.center)
      if let until = state.requirements.maintenanceUntil {
        Text(AppFormat.dateTime(until.date)).font(theme.typography.bodySm).foregroundStyle(theme.colors.muted)
      }
      if state.status == .updateRequired, let store = state.requirements.storeUrl.flatMap(URL.init(string:)) {
        Button {
          openURL(store)
        } label: {
          Text("shell.update_required.action", bundle: .module)
        }
        .buttonStyle(PrimaryButtonStyle())
        .accessibilityIdentifier("requirements.update")
      }
      if state.status == .maintenance {
        Button(action: model.retry) { Text("common.retry", bundle: .module) }
          .buttonStyle(PrimaryButtonStyle())
          .accessibilityIdentifier("requirements.retry")
      }
    }
    .padding(theme.metrics.spaceLg)
    .frame(maxWidth: .infinity, maxHeight: .infinity)
    .background(theme.colors.appBg)
    .accessibilityIdentifier("requirements.blocking")
  }

  private func recommendation(_ state: RequirementsState) -> some View {
    HStack(spacing: theme.metrics.spaceSm) {
      Text("shell.update_recommended.title", bundle: .module).font(theme.typography.bodySm)
      Spacer()
      if let store = state.requirements.storeUrl.flatMap(URL.init(string:)) {
        Button {
          openURL(store)
        } label: {
          Text("shell.update_recommended.action", bundle: .module)
        }
      }
      Button(action: model.dismissRecommendation) { Text("shell.update_recommended.dismiss", bundle: .module) }
        .accessibilityIdentifier("requirements.dismiss")
    }
    .padding(theme.metrics.spaceMd)
    .card()
    .padding(theme.metrics.spaceMd)
    .accessibilityIdentifier("requirements.recommendation")
  }
}
