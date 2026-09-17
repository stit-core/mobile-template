public import Foundation
import Synchronization
import UserNotifications

// Payload data-first (docs/stack/push.md): текст строит клиент, deep_link — https-URL из таблицы deep-links.
public struct PushPayload: Equatable, Sendable {
  public let notificationId: String?
  public let category: String?
  public let deepLink: URL?

  public init?(userInfo: [AnyHashable: Any]) {
    guard !userInfo.isEmpty else { return nil }
    notificationId = userInfo["notification_id"] as? String
    category = userInfo["category"] as? String
    deepLink = (userInfo["deep_link"] as? String).flatMap(URL.init(string:))
  }
}

public enum NotificationCategories {
  public static let general = "GENERAL"

  public static func register(in center: UNUserNotificationCenter = .current()) {
    center.setNotificationCategories([
      UNNotificationCategory(identifier: general, actions: [], intentIdentifiers: [])
    ])
  }
}

// Один сигнал может прийти двумя транспортами: повтор notification_id не показывается второй раз.
nonisolated public final class NotificationDeduplicator {
  private let seen = Mutex<[String]>([])
  private let capacity: Int

  public init(capacity: Int = 64) {
    self.capacity = capacity
  }

  public func isNew(_ payload: PushPayload) -> Bool {
    guard let id = payload.notificationId else { return true }
    return seen.withLock { seen in
      if seen.contains(id) { return false }
      seen.append(id)
      if seen.count > capacity { seen.removeFirst() }
      return true
    }
  }
}
