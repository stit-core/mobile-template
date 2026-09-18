import Foundation
import Testing

@testable import AppFoundation

@Suite("AppTimestamp")
struct AppTimestampTests {
  @Test("ISO-8601 без дробной части разбирается")
  func plainInstant() throws {
    let date = try #require(AppTimestamp.parse("2026-01-05T12:00:00Z"))
    #expect(date == Date(timeIntervalSince1970: 1_767_614_400))
  }

  @Test("ISO-8601 с дробной частью разбирается")
  func fractionalInstant() throws {
    let date = try #require(AppTimestamp.parse("2026-01-05T12:00:00.250Z"))
    #expect(abs(date.timeIntervalSince1970 - 1_767_614_400.25) < 0.001)
  }

  @Test("смещение зоны приводится к UTC")
  func offsetInstant() throws {
    let date = try #require(AppTimestamp.parse("2026-01-05T15:00:00+03:00"))
    #expect(date == Date(timeIntervalSince1970: 1_767_614_400))
  }

  @Test("мусор не превращается в дату")
  func rejectsGarbage() {
    #expect(AppTimestamp.parse("") == nil)
    #expect(AppTimestamp.parse("вчера") == nil)
    #expect(AppTimestamp.parse("2026-01-05") == nil)
  }

  @Test("формат и разбор взаимно обратны")
  func roundTrip() throws {
    let moment = Date(timeIntervalSince1970: 1_767_614_400)
    let parsed = try #require(AppTimestamp.parse(AppTimestamp.format(moment)))
    #expect(parsed == moment)
  }
}
