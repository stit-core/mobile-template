import Foundation

// Контраст по WCAG 2.x над ARGB-значениями токенов: проверяется тестами и доступен рантайму для a11y.
nonisolated public enum ArgbColor {
  public static func alpha(_ argb: UInt32) -> UInt8 { UInt8((argb >> 24) & 0xFF) }
  public static func red(_ argb: UInt32) -> UInt8 { UInt8((argb >> 16) & 0xFF) }
  public static func green(_ argb: UInt32) -> UInt8 { UInt8((argb >> 8) & 0xFF) }
  public static func blue(_ argb: UInt32) -> UInt8 { UInt8(argb & 0xFF) }

  public static func relativeLuminance(_ argb: UInt32) -> Double {
    0.2126 * linear(red(argb)) + 0.7152 * linear(green(argb)) + 0.0722 * linear(blue(argb))
  }

  public static func contrastRatio(_ first: UInt32, _ second: UInt32) -> Double {
    let lighter = max(relativeLuminance(first), relativeLuminance(second))
    let darker = min(relativeLuminance(first), relativeLuminance(second))
    return (lighter + 0.05) / (darker + 0.05)
  }

  private static func linear(_ channel: UInt8) -> Double {
    let value = Double(channel) / 255
    return value <= 0.03928 ? value / 12.92 : pow((value + 0.055) / 1.055, 2.4)
  }
}
