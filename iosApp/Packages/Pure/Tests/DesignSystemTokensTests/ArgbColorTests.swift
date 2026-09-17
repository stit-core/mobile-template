import Testing

@testable import DesignSystemTokens

@Suite("ArgbColor")
struct ArgbColorTests {
  @Test("каналы извлекаются по маске")
  func channels() {
    let argb: UInt32 = 0x80FF_1020
    #expect(ArgbColor.alpha(argb) == 0x80)
    #expect(ArgbColor.red(argb) == 0xFF)
    #expect(ArgbColor.green(argb) == 0x10)
    #expect(ArgbColor.blue(argb) == 0x20)
  }

  @Test("яркость: чёрный 0, белый 1")
  func luminanceBounds() {
    #expect(ArgbColor.relativeLuminance(0xFF00_0000) == 0)
    #expect(abs(ArgbColor.relativeLuminance(0xFFFF_FFFF) - 1) < 0.0001)
  }

  @Test("контраст чёрного и белого — 21:1, симметричен")
  func contrastExtremes() {
    #expect(abs(ArgbColor.contrastRatio(0xFF00_0000, 0xFFFF_FFFF) - 21) < 0.01)
    #expect(ArgbColor.contrastRatio(0xFFFF_FFFF, 0xFF00_0000) == ArgbColor.contrastRatio(0xFF00_0000, 0xFFFF_FFFF))
  }

  @Test("одинаковые цвета — контраст 1")
  func contrastSame() {
    #expect(ArgbColor.contrastRatio(0xFF12_3456, 0xFF12_3456) == 1)
  }

  @Test("серый посередине — тёмная ветка sRGB-кривой")
  func midGray() {
    let luminance = ArgbColor.relativeLuminance(0xFF80_8080)
    #expect(luminance > 0.2 && luminance < 0.25)
  }
}
