public import Foundation

// Снимок виджета пишет shared через FileCache, а читает расширение: путь внутри общего каталога и порог свежести —
// единственный контракт между ними, поэтому заданы одним местом.
nonisolated public enum WidgetSnapshotFile {
  // Требование свежести «в пределах часа» из провайдера снимка: расширение про сеть не знает и судит только по возрасту.
  public static let freshFor: TimeInterval = 3600

  public static func relativePath(slug: String) -> String {
    "cache/widget-\(slug).json"
  }

  public static func isStale(generatedAt: Date, now: Date = .now, freshFor: TimeInterval = Self.freshFor) -> Bool {
    now.timeIntervalSince(generatedAt) > freshFor
  }
}
