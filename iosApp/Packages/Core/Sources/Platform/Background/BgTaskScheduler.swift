import AppFoundation
import BackgroundTasks
import Foundation
public import SharedBridge
import Synchronization

// Порт планировщика ОС: механизм выбирается по trigger и timing. Обещанный момент iOS даёт только показу уведомления,
// периодику и окно ведёт BGTaskScheduler, длящаяся работа сюда не отдаётся.
nonisolated public final class BgTaskScheduler: BackgroundScheduler, Sendable {
  private enum Mechanism {
    case notification
    case appRefresh
    case processing
    case unsupported
  }

  private let bundleId: String
  private let logger = AppLogger(category: "Background")
  private let sharedContainer = Mutex<SharedContainer?>(nil)

  public init(bundleId: String) {
    self.bundleId = bundleId
  }

  // Контейнер собирается из PlatformDeps, частью которых планировщик и является: связь замыкается после сборки.
  public func bind(container: SharedContainer) {
    sharedContainer.withLock { $0 = container }
  }

  // Регистрация обязана уложиться в didFinishLaunching: позже система задачу уже не поднимет.
  public func register(tasks: [BackgroundTask]) {
    for task in tasks {
      switch mechanism(for: task) {
      case .appRefresh, .processing:
        register(task)
      case .notification, .unsupported:
        continue
      }
    }
  }

  public func schedule(task: BackgroundTask) {
    switch mechanism(for: task) {
    case .notification:
      scheduleNotification(task)
    case .appRefresh:
      submit(BGAppRefreshTaskRequest(identifier: identifier(of: task.id)), task: task)
    case .processing:
      submit(processingRequest(task), task: task)
    case .unsupported:
      logger.warning("механизма под задачу \(task.id) нет: continuous и remote-event планировщику не отдаются")
    }
  }

  public func cancel(id: String) {
    BGTaskScheduler.shared.cancel(taskRequestWithIdentifier: identifier(of: id))
    // Напоминание — это уже поставленное уведомление с тем же ключом: отмена задачи обязана снять и его.
    LocalNotifier.cancel(identifier: id)
  }

  private func register(_ task: BackgroundTask) {
    let registered = BGTaskScheduler.shared.register(
      forTaskWithIdentifier: identifier(of: task.id),
      using: .main
    ) { [self] bgTask in
      // Обработчик приходит в главную очередь (using: .main), а BGTask не Sendable — работа идёт в её же домене.
      MainActor.assumeIsolated { launch(bgTask, task: task) }
    }
    if !registered {
      logger.error(
        "задача \(task.id) не зарегистрирована — её идентификатора нет в BGTaskSchedulerPermittedIdentifiers")
    }
  }

  @MainActor
  private func launch(_ bgTask: BGTask, task: BackgroundTask) {
    // Периодичности у BGTaskScheduler нет: следующий заход заказывается сразу, иначе задача больше не повторится.
    schedule(task: task)
    let work = Task { @MainActor in
      bgTask.setTaskCompleted(success: await run(id: task.id))
    }
    bgTask.expirationHandler = { work.cancel() }
  }

  private func run(id: String) async -> Bool {
    guard let graph = current(), let runner = graph.backgroundRunner(id: id) else {
      logger.error("нет исполнителя для \(id)")
      return false
    }
    guard let result = try? await runner.run() else { return false }
    return await BackgroundDelivery.deliver(result, container: graph)
  }

  // Точного исполнения кода в фоне iOS не даёт: текст собирается сейчас, показывает его система в обещанный момент.
  private func scheduleNotification(_ task: BackgroundTask) {
    guard let at = task.at?.date else {
      logger.error("exact-задача \(task.id) без момента показа")
      return
    }
    Task { [self] in
      guard let runner = current()?.backgroundRunner(id: task.id) else {
        logger.error("нет исполнителя для \(task.id)")
        return
      }
      let result = try? await runner.run()
      guard let notify = result as? BackgroundResultNotify else { return }
      await LocalNotifier.schedule(notify, at: at)
    }
  }

  private func submit(_ request: BGTaskRequest, task: BackgroundTask) {
    request.earliestBeginDate = earliestBeginDate(task)
    do {
      try BGTaskScheduler.shared.submit(request)
    } catch {
      // Симулятор и сборка без background modes отказывают в постановке — старт приложения из-за этого не ломается.
      logger.warning("не удалось запланировать \(task.id): \(error)")
    }
  }

  private func processingRequest(_ task: BackgroundTask) -> BGProcessingTaskRequest {
    let request = BGProcessingTaskRequest(identifier: identifier(of: task.id))
    request.requiresNetworkConnectivity =
      requires(.network, task) || requires(.unmetered, task) || task.trigger == .connectivity
    request.requiresExternalPower = requires(.charging, task)
    return request
  }

  private func earliestBeginDate(_ task: BackgroundTask) -> Date? {
    if let at = task.at?.date { return at }
    guard task.periodMinutes > 0 else { return nil }
    return Date().addingTimeInterval(TimeInterval(task.periodMinutes) * 60)
  }

  private func mechanism(for task: BackgroundTask) -> Mechanism {
    switch task.timing {
    case .exact:
      return .notification
    case .continuous:
      return .unsupported
    default:
      break
    }
    if task.trigger == .remoteEvent { return .unsupported }
    // Периодическое обновление и «когда удобно» — это фоновое обновление приложения; остальное — фоновая обработка.
    if task.trigger == .periodic || task.timing == .opportunistic { return .appRefresh }
    return .processing
  }

  private func requires(_ condition: BackgroundCondition, _ task: BackgroundTask) -> Bool {
    task.conditions.contains { $0 == condition }
  }

  private func identifier(of taskId: String) -> String {
    BackgroundTaskIdentifier.qualified(bundleId: bundleId, taskId: taskId)
  }

  private func current() -> SharedContainer? {
    sharedContainer.withLock { $0 }
  }
}
