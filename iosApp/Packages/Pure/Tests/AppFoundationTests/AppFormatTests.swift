import Foundation
import Testing

@testable import AppFoundation

@Suite("AppFormat")
struct AppFormatTests {
  let locale = Locale(identifier: "en_US")
  let moment = Date(timeIntervalSince1970: 1_767_614_400)

  @Test("версия с номером сборки")
  func version() {
    #expect(AppFormat.version("1.4.2", build: 1_004_002) == "1.4.2 (1004002)")
  }

  @Test("дата и время содержат год и не пусты")
  func dateTime() {
    let text = AppFormat.dateTime(moment, locale: locale)
    #expect(text.contains("2026"))
    #expect(text.count > AppFormat.date(moment, locale: locale).count)
  }

  @Test("дата без времени")
  func dateOnly() {
    let text = AppFormat.date(moment, locale: locale)
    #expect(text.contains("2026"))
    #expect(!text.contains(":"))
  }
}
