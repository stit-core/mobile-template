import AppFoundation
public import Foundation

// Снимок, который положил TodayTodosWidgetProvider из shared; поля повторяют TodayTodosWidgetSnapshot байт в байт.
private struct TodayTodosSnapshot: Decodable {
  let state: WidgetSnapshotState
  let items: [TodayTodosItem]
  let generatedAt: Date
  let tapUrl: String
}

// Расширение читает только файл из общего каталога: ни сети, ни токена, ни Shared внутри appex нет.
nonisolated public struct TodayTodosSnapshotStore: Sendable {
  private let containerUrl: URL?

  public init(containerUrl: URL?) {
    self.containerUrl = containerUrl
  }

  public init(bundle: Bundle = .main, fileManager: FileManager = .default) {
    self.init(containerUrl: AppGroupContainer.url(bundle: bundle, fileManager: fileManager))
  }

  public func entry(now: Date = .now) -> TodayTodosEntry {
    guard let snapshot = read() else { return .placeholder(date: now) }
    return TodayTodosEntry(
      date: snapshot.generatedAt,
      state: state(of: snapshot, now: now),
      items: snapshot.items,
      tapUrl: URL(string: snapshot.tapUrl)
    )
  }

  // Приложение публикует Populated/Empty/SignedOut; возраст снимка расширение оценивает само — обновить его ему нечем.
  private func state(of snapshot: TodayTodosSnapshot, now: Date) -> WidgetSnapshotState {
    guard snapshot.state == .populated || snapshot.state == .empty else { return snapshot.state }
    return WidgetSnapshotFile.isStale(generatedAt: snapshot.generatedAt, now: now) ? .stale : snapshot.state
  }

  private func read() -> TodayTodosSnapshot? {
    guard let url = containerUrl?.appendingPathComponent(WidgetSnapshotFile.relativePath(slug: TodayTodosEntry.kind)),
      let data = try? Data(contentsOf: url)
    else { return nil }
    let decoder = JSONDecoder()
    decoder.dateDecodingStrategy = .custom { decoder in
      let text = try decoder.singleValueContainer().decode(String.self)
      guard let date = AppTimestamp.parse(text) else {
        throw DecodingError.dataCorrupted(
          .init(codingPath: decoder.codingPath, debugDescription: "не ISO-8601: \(text)"))
      }
      return date
    }
    // Битый или чужой снимок — не повод рисовать пустоту как данные: виджет вернётся в placeholder.
    return try? decoder.decode(TodayTodosSnapshot.self, from: data)
  }
}
