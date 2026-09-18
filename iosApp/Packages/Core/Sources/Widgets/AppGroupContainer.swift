public import Foundation

// Единственный общий каталог приложения и расширения; идентификатор приходит из xcconfig APP_GROUP_ID через Info.plist,
// чтобы у dev/staging/prod/demo были разные контейнеры.
nonisolated public enum AppGroupContainer {
  public static let infoKey = "APP_GROUP_ID"

  public static func identifier(bundle: Bundle = .main) -> String? {
    guard let raw = bundle.object(forInfoDictionaryKey: infoKey) as? String else { return nil }
    let trimmed = raw.trimmingCharacters(in: .whitespacesAndNewlines)
    return trimmed.isEmpty ? nil : trimmed
  }

  public static func url(bundle: Bundle = .main, fileManager: FileManager = .default) -> URL? {
    guard let identifier = identifier(bundle: bundle) else { return nil }
    return fileManager.containerURL(forSecurityApplicationGroupIdentifier: identifier)
  }
}
