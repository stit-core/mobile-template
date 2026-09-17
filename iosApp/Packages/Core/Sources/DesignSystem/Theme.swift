import DesignSystemTokens
public import SwiftUI
import UIKit

// Единственное место, где ARGB-токены становятся SwiftUI.Color; фичи читают Theme из окружения.
public struct Theme: Sendable {
  public struct Colors: Sendable {
    public let appBg = Color(Tokens.Colors.appBg)
    public let appFg = Color(Tokens.Colors.appFg)
    public let surfaceMuted = Color(Tokens.Colors.surfaceMuted)
    public let divider = Color(Tokens.Colors.divider)
    public let actionBg = Color(Tokens.Colors.actionBg)
    public let actionFg = Color(Tokens.Colors.actionFg)
    public let danger = Color(Tokens.Colors.danger)
    public let success = Color(Tokens.Colors.success)
    public let warning = Color(Tokens.Colors.warning)
    public let muted = Color(Tokens.Colors.muted)
  }

  public struct Metrics: Sendable {
    public let spaceXs = CGFloat(Tokens.Dimensions.spaceXs)
    public let spaceSm = CGFloat(Tokens.Dimensions.spaceSm)
    public let spaceMd = CGFloat(Tokens.Dimensions.spaceMd)
    public let spaceLg = CGFloat(Tokens.Dimensions.spaceLg)
    public let spaceXl = CGFloat(Tokens.Dimensions.spaceXl)
    public let radiusControl = CGFloat(Tokens.Dimensions.radiusControl)
    public let radiusCard = CGFloat(Tokens.Dimensions.radiusCard)
    public let touchMin = CGFloat(Tokens.Dimensions.touchMin)
  }

  // Системные текстовые стили дают Dynamic Type; pt из токенов — ориентир паритета с Android.
  public struct Typography: Sendable {
    public let bodySm = Font.subheadline
    public let bodyMd = Font.body
    public let headingLg = Font.title2.weight(.semibold)
  }

  public let colors = Colors()
  public let metrics = Metrics()
  public let typography = Typography()

  public static let standard = Self()
}

extension EnvironmentValues {
  @Entry public var theme: Theme = .standard
}

extension Color {
  // Тема выбирается системой по trait collection: одна пара light/dark на токен.
  public init(_ token: ColorToken) {
    self.init(
      uiColor: UIColor { traits in
        UIColor(argb: traits.userInterfaceStyle == .dark ? token.dark : token.light)
      })
  }
}

extension UIColor {
  fileprivate convenience init(argb: UInt32) {
    self.init(
      red: CGFloat(ArgbColor.red(argb)) / 255,
      green: CGFloat(ArgbColor.green(argb)) / 255,
      blue: CGFloat(ArgbColor.blue(argb)) / 255,
      alpha: CGFloat(ArgbColor.alpha(argb)) / 255
    )
  }
}
