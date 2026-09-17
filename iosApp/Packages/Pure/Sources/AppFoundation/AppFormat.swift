public import Foundation

// Форматирование для экранов: платформенные FormatStyle над Date, без DateFormatter в body вью.
nonisolated public enum AppFormat {
  public static func version(_ version: String, build: Int64) -> String {
    "\(version) (\(build))"
  }

  public static func dateTime(_ date: Date, locale: Locale = .current) -> String {
    date.formatted(Date.FormatStyle(date: .abbreviated, time: .shortened, locale: locale))
  }

  public static func date(_ date: Date, locale: Locale = .current) -> String {
    date.formatted(Date.FormatStyle(date: .abbreviated, time: .omitted, locale: locale))
  }
}
