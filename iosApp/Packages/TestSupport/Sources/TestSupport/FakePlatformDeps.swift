import Foundation
import SharedBridge
import Synchronization

// Фейки портов PlatformDeps для тестов Core/Features: та же форма, что FakePlatformDeps в shared/commonTest.
nonisolated public final class InMemorySecureStore: SecureStore {
  private let values = Mutex<[String: String]>([:])

  public init() {}

  public func read(key: String) async throws -> String? { values.withLock { $0[key] } }
  public func write(key: String, value: String) async throws { values.withLock { $0[key] = value } }
  public func remove(key: String) async throws { values.withLock { _ = $0.removeValue(forKey: key) } }
  public var snapshot: [String: String] { values.withLock { $0 } }
}

nonisolated public final class InMemorySettings: Settings {
  private let values = Mutex<[String: String]>([:])

  public init() {}

  public func getString(key: String) async throws -> String? { values.withLock { $0[key] } }
  public func putString(key: String, value: String) async throws { values.withLock { $0[key] = value } }
  public func remove(key: String) async throws { values.withLock { _ = $0.removeValue(forKey: key) } }
}

nonisolated public final class FakeNotificationPermission: NotificationPermission {
  private let granted: Mutex<Bool>

  public init(granted: Bool = false) {
    self.granted = Mutex(granted)
  }

  // suspend fun … : Boolean приходит в Swift боксированным KotlinBoolean (completion handler Obj-C-экспорта).
  public func isGranted() async throws -> KotlinBoolean { KotlinBoolean(bool: granted.withLock { $0 }) }

  public func request() async throws -> KotlinBoolean {
    granted.withLock { $0 = true }
    return KotlinBoolean(bool: true)
  }
}

// Планировщик ОС в тестах только запоминает заказ: расписание проверяется по scheduled, как в shared/commonTest.
nonisolated public final class FakeBackgroundScheduler: BackgroundScheduler {
  private let scheduledTasks = Mutex<[BackgroundTask]>([])
  private let cancelledIds = Mutex<[String]>([])

  public init() {}

  public func schedule(task: BackgroundTask) {
    scheduledTasks.withLock { $0.append(task) }
  }

  public func cancel(id: String) {
    cancelledIds.withLock { $0.append(id) }
  }

  public var scheduled: [BackgroundTask] { scheduledTasks.withLock { $0 } }
  public var cancelled: [String] { cancelledIds.withLock { $0 } }
}

nonisolated public final class FakeLocale: LocaleProvider {
  public let languageTag: String
  public let timeZone: String

  public init(languageTag: String = "ru-RU", timeZone: String = "Europe/Moscow") {
    self.languageTag = languageTag
    self.timeZone = timeZone
  }
}

nonisolated public final class FakePlatformDeps: PlatformDeps {
  public let appInfo: AppInfo
  public let secureStore: any SecureStore
  public let settings: any Settings
  public let filesDir: String
  public let pushTransports: [any PushTransport]
  public let notificationPermission: any NotificationPermission
  public let background: any BackgroundScheduler
  public let telemetry: any TelemetrySink
  public let logSink: any LogSink
  public let locale: any LocaleProvider
  public let connectivity: any ConnectivityMonitor

  public init(
    secureStore: InMemorySecureStore = InMemorySecureStore(),
    settings: InMemorySettings = InMemorySettings(),
    notificationPermission: FakeNotificationPermission = FakeNotificationPermission(),
    background: FakeBackgroundScheduler = FakeBackgroundScheduler(),
    connectivity: ConnectivityState = ConnectivityState(initial: true)
  ) {
    appInfo = AppInfo(
      appId: "tech.stit.mobile.tests", version: "0.0.0", build: 1, osVersion: "iOS 27.0", deviceModel: "Simulator",
      store: .appStore)
    self.secureStore = secureStore
    self.settings = settings
    filesDir = FileManager.default.temporaryDirectory.appendingPathComponent("stit-tests-\(UUID().uuidString)").path
    pushTransports = []
    self.notificationPermission = notificationPermission
    self.background = background
    telemetry = NoopTelemetrySink.shared
    logSink = NoopLogSink.shared
    locale = FakeLocale()
    self.connectivity = connectivity
  }
}
