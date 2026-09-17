import Foundation
import Testing

@testable import AppFoundation

private final class Marker {}

@Suite("AppConfig из Info.plist")
struct AppConfigTests {
  static let dev: [String: Any] = [
    "APP_ENV": "dev",
    "API_BASE_URL": "https://api.dev.example.com",
    "API_MODE": "real",
    "AUTH_MODE": "bearer",
    "DEEP_LINK_HOST": "app.dev.example.com",
    "DEEP_LINK_SCHEME": "stit",
    "SENTRY_DSN": "",
    "PUSH_PROVIDERS": "apns",
  ]

  @Test("все ключи читаются как есть, env типизирован")
  func parsesAllKeys() throws {
    let config = try AppConfig(info: Self.dev)
    #expect(config.env == .dev)
    #expect(config.appEnv == "dev")
    #expect(config.apiBaseUrl == "https://api.dev.example.com")
    #expect(config.apiMode == "real")
    #expect(config.authMode == "bearer")
    #expect(config.deepLinkHost == "app.dev.example.com")
    #expect(config.deepLinkScheme == "stit")
    #expect(config.sentryDsn.isEmpty)
    #expect(config.pushProviders == ["apns"])
    #expect(!config.isMock)
    #expect(!config.sentryEnabled)
  }

  @Test("demo: mock-режим и пустой список провайдеров")
  func demoIsMock() throws {
    var info = Self.dev
    info["APP_ENV"] = "demo"
    info["API_MODE"] = "mock"
    info["PUSH_PROVIDERS"] = ""
    let config = try AppConfig(info: info)
    #expect(config.env == .demo)
    #expect(config.isMock)
    #expect(config.pushProviders.isEmpty)
  }

  @Test("список провайдеров: пробелы и пустые элементы отбрасываются")
  func splitsProviders() throws {
    var info = Self.dev
    info["PUSH_PROVIDERS"] = " apns , ntfy,, "
    #expect(try AppConfig(info: info).pushProviders == ["apns", "ntfy"])
  }

  @Test("непустой DSN включает Sentry")
  func sentryEnabled() throws {
    var info = Self.dev
    info["SENTRY_DSN"] = "https://key@glitchtip.example.com/1"
    #expect(try AppConfig(info: info).sentryEnabled)
  }

  @Test("отсутствующий ключ", arguments: AppConfig.Key.allCases)
  func missingKey(key: AppConfig.Key) {
    var info = Self.dev
    info.removeValue(forKey: key.rawValue)
    #expect(throws: AppConfigError.missingKey(key.rawValue)) { try AppConfig(info: info) }
  }

  @Test("пустое значение обязательного ключа", arguments: AppConfig.Key.allCases.filter { !$0.allowsEmpty })
  func emptyRequiredValue(key: AppConfig.Key) {
    var info = Self.dev
    info[key.rawValue] = "  "
    #expect(throws: AppConfigError.emptyValue(key.rawValue)) { try AppConfig(info: info) }
  }

  @Test("ключ не строка — как отсутствующий")
  func nonStringValue() {
    var info = Self.dev
    info["API_MODE"] = 42
    #expect(throws: AppConfigError.missingKey("API_MODE")) { try AppConfig(info: info) }
  }

  @Test("Bundle без Info.plist — как отсутствующий ключ")
  func bundleWithoutInfo() {
    #expect(throws: AppConfigError.missingKey("APP_ENV")) { try AppConfig(bundle: Bundle(for: Marker.self)) }
  }

  @Test("неизвестный APP_ENV")
  func unknownEnv() {
    var info = Self.dev
    info["APP_ENV"] = "qa"
    #expect(throws: AppConfigError.unknownEnv("qa")) { try AppConfig(info: info) }
  }
}

@Suite("AppEnv")
struct AppEnvTests {
  @Test(
    "разбор без учёта регистра и пробелов",
    arguments: [("dev", AppEnv.dev), (" Prod ", .prod), ("STAGING", .staging), ("demo", .demo)]
  )
  func parses(raw: String, expected: AppEnv) throws {
    #expect(try AppEnv(parsing: raw) == expected)
  }

  @Test("только prod считается production")
  func production() {
    #expect(AppEnv.prod.isProduction)
    #expect(AppEnv.allCases.filter(\.isProduction) == [.prod])
  }
}
