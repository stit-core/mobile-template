import SharedBridge

// Результат фоновой работы доносится до пользователя; повтора просит только отказ по ошибке — остальные причины
// означают, что работа сделана и будить процесс снова незачем.
nonisolated enum BackgroundDelivery {
  static func deliver(_ result: BackgroundResult, container: SharedContainer) async -> Bool {
    switch result {
    case let notify as BackgroundResultNotify:
      await LocalNotifier.show(notify)
      return true
    case let refresh as BackgroundResultRefreshWidget:
      await WidgetRefresh.publish(slug: refresh.slug, container: container)
      return true
    case let rejected as BackgroundResultRejected:
      return rejected.reason != .failed
    default:
      return true
    }
  }
}
