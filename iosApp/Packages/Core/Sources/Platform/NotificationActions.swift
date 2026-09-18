public import SharedBridge
import SystemActions

// Кнопки уведомлений (канал notification:<slug>): оркестрация та же, что у интента и кнопки виджета — решает shared,
// платформа отвечает только видимой пользователю реакцией.
nonisolated public enum NotificationActions {
  // true — ответ разобран как действие; false — обычный тап, дальше его ведёт роутер приложения.
  public static func handle(
    action: String, payload: PushPayload?, notificationId: String, container: SharedContainer
  ) async -> Bool {
    guard action == NotificationCategories.completeTodo else { return false }
    // Адресат берётся из той же ссылки, по которой открывается приложение: отдельного идентификатора у кнопки нет.
    guard let link = payload?.deepLink?.absoluteString,
      let route = container.deepLinkParser.parse(url: link) as? RouteTodoDetail
    else { return true }
    let response = await SystemActions.completeTodo(id: route.id)
    if response.succeeded {
      LocalNotifier.cancel(identifier: notificationId)
      await WidgetRefresh.publishAll(container: container)
    } else {
      // Экрана у действия нет: отказ виден единственным способом — уведомлением со ссылкой, где его починить.
      await LocalNotifier.show(
        title: response.title, body: response.text, deepLink: link, identifier: notificationId)
    }
    return true
  }
}
