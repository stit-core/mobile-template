import SharedBridge
import UserNotifications

// Разрешение запрашивается в момент ценности (PushOptInPresenter), не на первом экране.
nonisolated public final class NotificationCenterPermission: NotificationPermission {
  public init() {}

  // suspend fun … : Boolean приходит в Swift боксированным KotlinBoolean (completion handler Obj-C-экспорта).
  public func isGranted() async throws -> KotlinBoolean {
    let settings = await UNUserNotificationCenter.current().notificationSettings()
    return KotlinBoolean(bool: [.authorized, .provisional, .ephemeral].contains(settings.authorizationStatus))
  }

  public func request() async throws -> KotlinBoolean {
    KotlinBoolean(
      bool: try await UNUserNotificationCenter.current().requestAuthorization(options: [.alert, .badge, .sound]))
  }
}
