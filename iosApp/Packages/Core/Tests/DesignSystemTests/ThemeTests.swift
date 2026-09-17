import DesignSystemTokens
import SwiftUI
import Testing
import UIKit

@testable import DesignSystem

@Suite("Theme")
struct ThemeTests {
  @Test("Color(token) даёт разные цвета для light и dark trait collection")
  func colorFollowsTraits() {
    let token = ColorToken(light: 0xFFFF_FFFF, dark: 0xFF00_0000)
    let color = UIColor(Color(token))
    let light = color.resolvedColor(with: UITraitCollection(userInterfaceStyle: .light))
    let dark = color.resolvedColor(with: UITraitCollection(userInterfaceStyle: .dark))
    #expect(light != dark)
  }

  @Test("метрики повторяют токены")
  func metricsMirrorTokens() {
    let metrics = Theme.standard.metrics
    #expect(metrics.spaceMd == CGFloat(Tokens.Dimensions.spaceMd))
    #expect(metrics.touchMin >= 44)
  }
}
