public import Foundation

nonisolated public enum AppConfigError: Error, Equatable, Sendable {
  case missingKey(String)
  case emptyValue(String)
  case unknownEnv(String)
}

// Те же ключи, что в config/env/*.properties и Config/*.xcconfig: одна таблица на обе платформы.
nonisolated public struct AppConfig: Equatable, Sendable {
  public enum Key: String, CaseIterable, Sendable {
    case appEnv = "APP_ENV"
    case apiBaseUrl = "API_BASE_URL"
    case apiMode = "API_MODE"
    case authMode = "AUTH_MODE"
    case deepLinkHost = "DEEP_LINK_HOST"
    case deepLinkScheme = "DEEP_LINK_SCHEME"
    case sentryDsn = "SENTRY_DSN"
    case pushProviders = "PUSH_PROVIDERS"

    // Пустой DSN выключает Sentry, пустой список провайдеров — push; остальные ключи обязаны иметь значение.
    public var allowsEmpty: Bool { self == .sentryDsn || self == .pushProviders }
  }

  public let env: AppEnv
  public let appEnv: String
  public let apiBaseUrl: String
  public let apiMode: String
  public let authMode: String
  public let deepLinkHost: String
  public let deepLinkScheme: String
  public let sentryDsn: String
  public let pushProviders: [String]

  public init(info: [String: Any]) throws(AppConfigError) {
    let values = try Self.values(from: info)
    appEnv = values[.appEnv, default: ""]
    env = try AppEnv(parsing: appEnv)
    apiBaseUrl = values[.apiBaseUrl, default: ""]
    apiMode = values[.apiMode, default: ""]
    authMode = values[.authMode, default: ""]
    deepLinkHost = values[.deepLinkHost, default: ""]
    deepLinkScheme = values[.deepLinkScheme, default: ""]
    sentryDsn = values[.sentryDsn, default: ""]
    pushProviders = Self.list(values[.pushProviders, default: ""])
  }

  public init(bundle: Bundle) throws(AppConfigError) {
    try self.init(info: bundle.infoDictionary ?? [:])
  }

  public var isMock: Bool { apiMode == "mock" }
  public var sentryEnabled: Bool { !sentryDsn.isEmpty }

  private static func values(from info: [String: Any]) throws(AppConfigError) -> [Key: String] {
    var values: [Key: String] = [:]
    for key in Key.allCases {
      guard let raw = info[key.rawValue] as? String else { throw .missingKey(key.rawValue) }
      let value = raw.trimmingCharacters(in: .whitespacesAndNewlines)
      if value.isEmpty, !key.allowsEmpty { throw .emptyValue(key.rawValue) }
      values[key] = value
    }
    return values
  }

  private static func list(_ raw: String) -> [String] {
    raw.split(separator: ",").map { $0.trimmingCharacters(in: .whitespaces) }.filter { !$0.isEmpty }
  }
}
