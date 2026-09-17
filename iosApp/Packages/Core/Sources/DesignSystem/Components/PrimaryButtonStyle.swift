public import SwiftUI

// Единственная кастомная кнопка: системные бары и контролы не перекрашиваются (Liquid Glass — системный).
public struct PrimaryButtonStyle: ButtonStyle {
  @Environment(\.theme) private var theme
  @Environment(\.isEnabled) private var isEnabled

  public init() {}

  public func makeBody(configuration: Configuration) -> some View {
    configuration.label
      .font(theme.typography.bodyMd.weight(.semibold))
      .foregroundStyle(theme.colors.actionFg)
      .frame(maxWidth: .infinity, minHeight: theme.metrics.touchMin)
      .background(theme.colors.actionBg.opacity(isEnabled ? 1 : 0.4))
      .clipShape(RoundedRectangle(cornerRadius: theme.metrics.radiusControl, style: .continuous))
      .opacity(configuration.isPressed ? 0.8 : 1)
  }
}

extension View {
  public func card() -> some View {
    modifier(CardModifier())
  }
}

private struct CardModifier: ViewModifier {
  @Environment(\.theme) private var theme

  func body(content: Content) -> some View {
    content
      .padding(theme.metrics.spaceMd)
      .background(theme.colors.surfaceMuted)
      .clipShape(RoundedRectangle(cornerRadius: theme.metrics.radiusCard, style: .continuous))
  }
}
