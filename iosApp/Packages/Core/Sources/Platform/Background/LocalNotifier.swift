import AppFoundation
public import Foundation
public import SharedBridge
import UserNotifications

// Локальные уведомления фоновой задачи и ответы каналов: провайдеров push, PushRegistrar и бэкенда в цепочке нет.
// Payload тот же, что у push (category, deep_link, notification_id) — тап разбирает общий PushPayload.
nonisolated public enum LocalNotifier {
  // Обещанный момент — это момент показа, а не исполнения кода: текст собран заранее, дальше показывает система.
  public static func schedule(_ notify: BackgroundResultNotify, at date: Date) async {
    await add(content(notify), identifier: notify.notificationId, trigger: trigger(at: date))
  }

  public static func show(_ notify: BackgroundResultNotify) async {
    await add(content(notify), identifier: notify.notificationId, trigger: nil)
  }

  // Ответ канала без экрана (кнопка уведомления): показать его больше негде.
  public static func show(title: String, body: String, deepLink: String?, identifier: String) async {
    let content = UNMutableNotificationContent()
    content.title = title
    content.body = body
    content.categoryIdentifier = NotificationCategories.general
    content.userInfo = userInfo(
      category: NotificationCategories.general, notificationId: identifier, deepLink: deepLink)
    await add(content, identifier: identifier, trigger: nil)
  }

  public static func cancel(identifier: String) {
    let center = UNUserNotificationCenter.current()
    center.removePendingNotificationRequests(withIdentifiers: [identifier])
    center.removeDeliveredNotifications(withIdentifiers: [identifier])
  }

  private static func content(_ notify: BackgroundResultNotify) -> UNMutableNotificationContent {
    let content = UNMutableNotificationContent()
    content.title = AppStrings.text(notify.titleKey, bundle: .module, arguments: notify.args)
    content.body = AppStrings.text(notify.bodyKey, bundle: .module, arguments: notify.args)
    content.categoryIdentifier = notify.category
    content.sound = .default
    content.userInfo = userInfo(
      category: notify.category, notificationId: notify.notificationId, deepLink: notify.deepLink)
    return content
  }

  private static func userInfo(category: String, notificationId: String, deepLink: String?) -> [String: String] {
    var userInfo = ["category": category, "notification_id": notificationId]
    userInfo["deep_link"] = deepLink
    return userInfo
  }

  // Момент уже прошёл (устройство было выключено, система разбудила позже) — показываем сразу, а не молчим.
  private static func trigger(at date: Date) -> UNNotificationTrigger? {
    guard date > Date() else { return nil }
    let fields = Calendar.current.dateComponents([.year, .month, .day, .hour, .minute, .second], from: date)
    return UNCalendarNotificationTrigger(dateMatching: fields, repeats: false)
  }

  private static func add(
    _ content: UNMutableNotificationContent, identifier: String, trigger: UNNotificationTrigger?
  ) async {
    let request = UNNotificationRequest(identifier: identifier, content: content, trigger: trigger)
    // Разрешение могло быть не выдано или отозвано: это отказ пользователя, а не ошибка фоновой работы.
    try? await UNUserNotificationCenter.current().add(request)
  }
}
