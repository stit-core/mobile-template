public import Foundation

// Снимки из shared несут kotlin.time.Instant, а он сериализуется ISO-8601 с необязательной дробной частью:
// один формат разбирает не все значения, поэтому проверяются оба.
nonisolated public enum AppTimestamp {
  public static func parse(_ text: String) -> Date? {
    let formatter = ISO8601DateFormatter()
    formatter.formatOptions = [.withInternetDateTime, .withFractionalSeconds]
    if let date = formatter.date(from: text) { return date }
    formatter.formatOptions = [.withInternetDateTime]
    return formatter.date(from: text)
  }

  public static func format(_ date: Date) -> String {
    let formatter = ISO8601DateFormatter()
    formatter.formatOptions = [.withInternetDateTime, .withFractionalSeconds]
    return formatter.string(from: date)
  }
}
