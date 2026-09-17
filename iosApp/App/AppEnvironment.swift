import AppFoundation
import Features
import Foundation
import Observation
import Persistence
import Platform
import Routing
import SharedBridge

// Composition root: единственное место, где Swift-реализации PlatformDeps собираются в SharedContainer.
@Observable
final class AppEnvironment {
  let config: AppFoundation.AppConfig
  let container: SharedContainer
  let router = Router()
  let shell: ShellModel
  let requirements: RequirementsModel
  let settings: SettingsModel
  let apns: ApnsRegistrar
  private let secureStore: any SecureStore

  static func live() -> AppEnvironment {
    do {
      return try AppEnvironment(bundle: .main)
    } catch {
      fatalError("Info.plist не содержит конфиг схемы (Config/*.xcconfig): \(error)")
    }
  }

  init(bundle: Bundle) throws {
    config = try AppFoundation.AppConfig(bundle: bundle)
    let appInfo = DeviceInfo.appInfo(bundle: bundle)
    let secureStore = KeychainSecureStore(service: appInfo.appId)
    self.secureStore = secureStore
    let apns = ApnsRegistrar()
    self.apns = apns
    let platform = IosPlatformDeps(
      appInfo: appInfo,
      secureStore: secureStore,
      filesDir: try FilesDirectory.applicationSupport(),
      pushTransports: config.pushProviders.contains("apns") ? [apns] : [],
      telemetry: SentryCrashReporter(
        dsn: config.sentryDsn, environment: config.appEnv, release: "\(appInfo.appId)@\(appInfo.versionWithBuild)"),
      logSink: SharedLogSink(subsystem: appInfo.appId)
    )
    let sharedConfig = SharedAppConfig.companion.parse(
      env: config.appEnv,
      apiBaseUrl: config.apiBaseUrl,
      apiMode: config.apiMode,
      authMode: config.authMode,
      deepLinkHost: config.deepLinkHost,
      deepLinkScheme: config.deepLinkScheme,
      sentryDsn: config.sentryDsn
    )
    container = SharedContainer(platform: platform, config: sharedConfig)
    shell = ShellModel(presenter: container.shellPresenter())
    requirements = RequirementsModel(presenter: container.requirementsPresenter())
    settings = SettingsModel(presenter: container.settingsPresenter())
  }

  // Порядок старта: чистка Keychain первой установки → shell (demo → кэши → сессия → route) → requirements.
  func start() async {
    try? await FirstLaunchReset().run(secureStore: secureStore)
    shell.start()
    requirements.start()
  }

  func loginModel() -> LoginModel { LoginModel(presenter: container.loginPresenter()) }
  func sessionsModel() -> SessionsModel { SessionsModel(presenter: container.sessionsPresenter()) }
  func deleteAccountModel() -> DeleteAccountModel { DeleteAccountModel(presenter: container.deleteAccountPresenter()) }
  func profileModel() -> ProfileModel { ProfileModel(presenter: container.profilePresenter()) }
  func todoListModel() -> TodoListModel { TodoListModel(presenter: container.todoListPresenter()) }
  func todoDetailModel(id: String) -> TodoDetailModel {
    TodoDetailModel(presenter: container.todoDetailPresenter(id: id))
  }
  func pushOptInModel() -> PushOptInModel { PushOptInModel(presenter: container.pushOptInPresenter()) }
  func demoPanelModel() -> DemoPanelModel { DemoPanelModel(presenter: container.demoPanelPresenter()) }
}

// Реализации портов Shared на iOS: Keychain/UserDefaults из фреймворка, остальное — Core/Platform.
nonisolated private final class IosPlatformDeps: PlatformDeps {
  let appInfo: AppInfo
  let secureStore: any SecureStore
  let settings: any Settings = UserDefaultsSettings(defaults: .standard)
  let filesDir: String
  let pushTransports: [any PushTransport]
  let notificationPermission: any NotificationPermission = NotificationCenterPermission()
  let telemetry: any TelemetrySink
  let logSink: any LogSink
  let locale: any LocaleProvider = DeviceLocale()
  let connectivity: any ConnectivityMonitor = NetworkConnectivity()

  init(
    appInfo: AppInfo,
    secureStore: any SecureStore,
    filesDir: String,
    pushTransports: [any PushTransport],
    telemetry: any TelemetrySink,
    logSink: any LogSink
  ) {
    self.appInfo = appInfo
    self.secureStore = secureStore
    self.filesDir = filesDir
    self.pushTransports = pushTransports
    self.telemetry = telemetry
    self.logSink = logSink
  }
}
