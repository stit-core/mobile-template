import Foundation
import SharedBridge
import UIKit

// LocaleProvider из Shared: языковой тег и таймзона уходят в регистрацию устройства и Accept-Language.
nonisolated public final class DeviceLocale: LocaleProvider {
  public init() {}

  public var languageTag: String { Locale.current.identifier(.bcp47) }
  public var timeZone: String { TimeZone.current.identifier }
}

public enum DeviceInfo {
  // AppInfo для User-Agent и /api/app/requirements; канал установки на iOS всегда App Store.
  public static func appInfo(bundle: Bundle = .main) -> AppInfo {
    let info = bundle.infoDictionary ?? [:]
    return AppInfo(
      appId: bundle.bundleIdentifier ?? "",
      version: info["CFBundleShortVersionString"] as? String ?? "0.0.0",
      build: Int64(info["CFBundleVersion"] as? String ?? "") ?? 1,
      osVersion: "iOS \(UIDevice.current.systemVersion)",
      deviceModel: UIDevice.current.model,
      store: .appStore
    )
  }
}
