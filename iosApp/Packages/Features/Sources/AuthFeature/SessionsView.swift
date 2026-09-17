import AppFoundation
import DesignSystem
import SharedBridge
public import SwiftUI

public struct SessionsView: View {
  @Environment(\.theme) private var theme
  private let model: SessionsModel

  public init(model: SessionsModel) {
    self.model = model
  }

  public var body: some View {
    let state = model.state
    Group {
      if state.loading, state.sessions.isEmpty {
        LoadingStateView()
      } else if let error = state.error, state.sessions.isEmpty {
        ErrorStateView(messageKey: error.localizationKey, onRetry: model.refresh)
      } else if state.sessions.isEmpty {
        EmptyStateView(Text("auth.sessions.empty", bundle: .module), systemImage: "person.badge.key")
      } else {
        list(state)
      }
    }
    .navigationTitle(Text("auth.sessions.title", bundle: .module))
    .task { model.refresh() }
    .accessibilityIdentifier("sessions.screen")
  }

  private func list(_ state: SessionsState) -> some View {
    List {
      Section {
        ForEach(state.sessions, id: \.jti) { session in
          SessionRow(session: session)
            .swipeActions {
              if !session.current {
                Button(role: .destructive) {
                  model.revoke(jti: session.jti)
                } label: {
                  Text("auth.sessions.revoke", bundle: .module)
                }
              }
            }
        }
      } footer: {
        Button(role: .destructive, action: model.revokeAll) { Text("auth.sessions.revoke_all", bundle: .module) }
          .accessibilityIdentifier("sessions.revoke_all")
      }
      if !state.devices.isEmpty {
        Section(header: Text("auth.sessions.devices", bundle: .module)) {
          ForEach(state.devices, id: \.id) { device in
            HStack {
              Text(device.deviceModel ?? device.platform)
              Spacer()
              Text(device.provider).font(theme.typography.bodySm).foregroundStyle(theme.colors.muted)
            }
            .swipeActions {
              Button(role: .destructive) {
                model.unregister(deviceId: device.id)
              } label: {
                Text("auth.sessions.unregister", bundle: .module)
              }
            }
          }
        }
      }
    }
    .refreshable { model.refresh() }
  }
}

private struct SessionRow: View {
  @Environment(\.theme) private var theme
  let session: Session

  var body: some View {
    VStack(alignment: .leading, spacing: theme.metrics.spaceXs) {
      HStack {
        Text(session.deviceName ?? session.client ?? session.jti).font(theme.typography.bodyMd)
        if session.current {
          Text("auth.sessions.current", bundle: .module)
            .font(theme.typography.bodySm)
            .foregroundStyle(theme.colors.success)
        }
      }
      Text(AppFormat.dateTime((session.lastUsedAt ?? session.issuedAt).date))
        .font(theme.typography.bodySm)
        .foregroundStyle(theme.colors.muted)
    }
  }
}
