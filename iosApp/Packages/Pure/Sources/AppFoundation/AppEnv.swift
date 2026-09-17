import Foundation

nonisolated public enum AppEnv: String, CaseIterable, Sendable {
  case dev
  case staging
  case prod
  case demo

  public init(parsing raw: String) throws(AppConfigError) {
    guard let env = Self(rawValue: raw.trimmingCharacters(in: .whitespaces).lowercased()) else {
      throw .unknownEnv(raw)
    }
    self = env
  }

  public var isProduction: Bool { self == .prod }
}
