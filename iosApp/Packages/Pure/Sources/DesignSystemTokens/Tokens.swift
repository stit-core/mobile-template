// Сгенерировано из design/tokens.json командой make gen-tokens — руками не правится, дрейф ловит tokens-check.
nonisolated public struct ColorToken: Sendable {
  public let light: UInt32
  public let dark: UInt32

  public init(light: UInt32, dark: UInt32) {
    self.light = light
    self.dark = dark
  }
}

nonisolated public enum Tokens {
  nonisolated public enum Colors {
    public static let appBg = ColorToken(light: 0xFFFFFFFF, dark: 0xFF121212)
    public static let appFg = ColorToken(light: 0xFF1A1A1A, dark: 0xFFECECEC)
    public static let surfaceMuted = ColorToken(light: 0xFFF2F2F2, dark: 0xFF1E1E1E)
    public static let divider = ColorToken(light: 0xFFE0E0E0, dark: 0xFF2E2E2E)
    public static let actionBg = ColorToken(light: 0xFF1B5E20, dark: 0xFF81C784)
    public static let actionFg = ColorToken(light: 0xFFFFFFFF, dark: 0xFF0B2E10)
    public static let danger = ColorToken(light: 0xFFB3261E, dark: 0xFFF2B8B5)
    public static let success = ColorToken(light: 0xFF1B7F3B, dark: 0xFF6ED08C)
    public static let warning = ColorToken(light: 0xFF8A5200, dark: 0xFFFFC46B)
    public static let muted = ColorToken(light: 0xFF6B6B6B, dark: 0xFF9A9A9A)
  }

  nonisolated public enum Dimensions {
    public static let spaceXs: Double = 4
    public static let spaceSm: Double = 8
    public static let spaceMd: Double = 16
    public static let spaceLg: Double = 24
    public static let spaceXl: Double = 32
    public static let radiusControl: Double = 12
    public static let radiusCard: Double = 16
    public static let touchMin: Double = 48
  }

  nonisolated public enum Typography {
    public static let bodySm: Double = 14
    public static let bodyMd: Double = 16
    public static let headingLg: Double = 22
  }
}
