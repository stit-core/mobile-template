import Testing

@testable import DesignSystemTokens

// Инварианты, а не конкретные значения: Tokens.swift перегенерируется из design/tokens.json (make gen-tokens).
@Suite("Токены дизайна")
struct TokensTests {
  struct TextRole {
    let name: String
    let foreground: ColorToken
    let background: ColorToken
  }

  let colors: [(name: String, token: ColorToken)] = [
    ("color.app.bg", Tokens.Colors.appBg),
    ("color.app.fg", Tokens.Colors.appFg),
    ("color.surface.muted", Tokens.Colors.surfaceMuted),
    ("color.divider", Tokens.Colors.divider),
    ("color.action.bg", Tokens.Colors.actionBg),
    ("color.action.fg", Tokens.Colors.actionFg),
    ("color.danger", Tokens.Colors.danger),
    ("color.success", Tokens.Colors.success),
    ("color.warning", Tokens.Colors.warning),
    ("color.muted", Tokens.Colors.muted),
  ]

  // Текстовая роль на своём фоне: контраст ≥ 4.5:1 в обеих темах (WCAG AA).
  let textRoles: [TextRole] = [
    TextRole(name: "app.fg/app.bg", foreground: Tokens.Colors.appFg, background: Tokens.Colors.appBg),
    TextRole(name: "app.fg/surface.muted", foreground: Tokens.Colors.appFg, background: Tokens.Colors.surfaceMuted),
    TextRole(name: "action.fg/action.bg", foreground: Tokens.Colors.actionFg, background: Tokens.Colors.actionBg),
    TextRole(name: "danger/app.bg", foreground: Tokens.Colors.danger, background: Tokens.Colors.appBg),
    TextRole(name: "success/app.bg", foreground: Tokens.Colors.success, background: Tokens.Colors.appBg),
    TextRole(name: "warning/app.bg", foreground: Tokens.Colors.warning, background: Tokens.Colors.appBg),
    TextRole(name: "muted/app.bg", foreground: Tokens.Colors.muted, background: Tokens.Colors.appBg),
  ]

  @Test("цвета непрозрачны в обеих темах")
  func opaque() {
    for color in colors {
      #expect(ArgbColor.alpha(color.token.light) == 0xFF, "\(color.name) light")
      #expect(ArgbColor.alpha(color.token.dark) == 0xFF, "\(color.name) dark")
    }
  }

  @Test("контраст текстовых ролей ≥ 4.5:1")
  func contrast() {
    for role in textRoles {
      #expect(ArgbColor.contrastRatio(role.foreground.light, role.background.light) >= 4.5, "\(role.name) light")
      #expect(ArgbColor.contrastRatio(role.foreground.dark, role.background.dark) >= 4.5, "\(role.name) dark")
    }
  }

  @Test("light и dark различаются у фонов")
  func themesDiffer() {
    #expect(Tokens.Colors.appBg.light != Tokens.Colors.appBg.dark)
    #expect(Tokens.Colors.surfaceMuted.light != Tokens.Colors.surfaceMuted.dark)
  }

  @Test("отступы положительны и растут")
  func spacing() {
    let scale = [
      Tokens.Dimensions.spaceXs, Tokens.Dimensions.spaceSm, Tokens.Dimensions.spaceMd,
      Tokens.Dimensions.spaceLg, Tokens.Dimensions.spaceXl,
    ]
    #expect(scale.allSatisfy { $0 > 0 })
    #expect(scale == scale.sorted())
    #expect(Set(scale).count == scale.count)
  }

  @Test("радиусы и минимальный touch-target ≥ 44 pt")
  func radiiAndTouchTarget() {
    #expect(Tokens.Dimensions.radiusControl > 0)
    #expect(Tokens.Dimensions.radiusCard >= Tokens.Dimensions.radiusControl)
    #expect(Tokens.Dimensions.touchMin >= 44)
  }

  @Test("типографика растёт от body к heading")
  func typography() {
    #expect(Tokens.Typography.bodySm > 0)
    #expect(Tokens.Typography.bodySm < Tokens.Typography.bodyMd)
    #expect(Tokens.Typography.bodyMd < Tokens.Typography.headingLg)
  }
}
