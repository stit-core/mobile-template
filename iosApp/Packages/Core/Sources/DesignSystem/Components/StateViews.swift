public import SwiftUI

public struct LoadingStateView: View {
  public init() {}

  public var body: some View {
    ProgressView()
      .controlSize(.large)
      .frame(maxWidth: .infinity, maxHeight: .infinity)
      .accessibilityIdentifier("state.loading")
  }
}

public struct EmptyStateView: View {
  @Environment(\.theme) private var theme
  private let title: Text
  private let systemImage: String

  // Заголовок приходит уже локализованным Text(…, bundle:) — ключ живёт в каталоге фичи, не DesignSystem.
  public init(_ title: Text, systemImage: String = "tray") {
    self.title = title
    self.systemImage = systemImage
  }

  public var body: some View {
    ContentUnavailableView {
      Label {
        title
      } icon: {
        Image(systemName: systemImage)
      }
    }
    .foregroundStyle(theme.colors.muted)
    .accessibilityIdentifier("state.empty")
  }
}

public struct ErrorStateView: View {
  @Environment(\.theme) private var theme
  private let messageKey: String
  private let onRetry: (() -> Void)?

  // messageKey — MessageKey.value презентера (error.<code>); строки лежат в каталоге DesignSystem.
  public init(messageKey: String, onRetry: (() -> Void)? = nil) {
    self.messageKey = messageKey
    self.onRetry = onRetry
  }

  public var body: some View {
    VStack(spacing: theme.metrics.spaceMd) {
      Image(systemName: "exclamationmark.triangle")
        .font(theme.typography.headingLg)
        .foregroundStyle(theme.colors.danger)
      Text(LocalizedStringKey(messageKey), bundle: .module)
        .font(theme.typography.bodyMd)
        .multilineTextAlignment(.center)
      if let onRetry {
        Button(action: onRetry) { Text("common.retry", bundle: .module) }
          .buttonStyle(PrimaryButtonStyle())
          .accessibilityIdentifier("state.error.retry")
      }
    }
    .padding(theme.metrics.spaceLg)
    .frame(maxWidth: .infinity, maxHeight: .infinity)
    .accessibilityIdentifier("state.error")
  }
}

// Инлайн-текст ошибки формы; ключ — MessageKey.value презентера, строки — в каталоге DesignSystem.
public struct ErrorText: View {
  @Environment(\.theme) private var theme
  private let messageKey: String

  public init(messageKey: String) {
    self.messageKey = messageKey
  }

  public var body: some View {
    Text(LocalizedStringKey(messageKey), bundle: .module)
      .font(theme.typography.bodySm)
      .foregroundStyle(theme.colors.danger)
      .accessibilityIdentifier("error.text")
  }
}

// Приложение идёт на фикстурах по флагу demo_login: пользователь видит, что данные ненастоящие, и выходит
// одним нажатием — иначе demo-режим не отличить от сломанного бэкенда.
public struct DemoBanner: View {
  @Environment(\.theme) private var theme
  private let onExit: () -> Void

  public init(onExit: @escaping () -> Void) {
    self.onExit = onExit
  }

  public var body: some View {
    HStack {
      Label {
        Text("common.demo", bundle: .module)
      } icon: {
        Image(systemName: "theatermasks")
      }
      Spacer()
      Button(action: onExit) { Text("common.demo.exit", bundle: .module) }
        .accessibilityIdentifier("banner.demo.exit")
    }
    .font(theme.typography.bodySm)
    .foregroundStyle(theme.colors.appFg)
    .padding(.horizontal, theme.metrics.spaceMd)
    .padding(.vertical, theme.metrics.spaceSm)
    .frame(maxWidth: .infinity)
    .background(theme.colors.warning.opacity(0.35))
    .accessibilityIdentifier("banner.demo")
  }
}

public struct OfflineBanner: View {
  @Environment(\.theme) private var theme

  public init() {}

  public var body: some View {
    Label {
      Text("common.offline", bundle: .module)
    } icon: {
      Image(systemName: "wifi.slash")
    }
    .font(theme.typography.bodySm)
    .foregroundStyle(theme.colors.appFg)
    .padding(.vertical, theme.metrics.spaceSm)
    .frame(maxWidth: .infinity)
    .background(theme.colors.warning.opacity(0.25))
    .accessibilityIdentifier("banner.offline")
  }
}
