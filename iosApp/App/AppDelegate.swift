import Platform
import SharedBridge
import UIKit
import UserNotifications

// APNs-токен и тап по уведомлению приходят только через делегат: остальное живёт в SwiftUI-сцене.
final class AppDelegate: NSObject, UIApplicationDelegate, UNUserNotificationCenterDelegate {
  let environment = AppEnvironment.live()
  private let deduplicator = NotificationDeduplicator()

  func application(
    _ application: UIApplication,
    didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey: Any]? = nil
  ) -> Bool {
    UNUserNotificationCenter.current().delegate = self
    NotificationCategories.register()
    return true
  }

  func application(_ application: UIApplication, didRegisterForRemoteNotificationsWithDeviceToken deviceToken: Data) {
    environment.apns.didRegister(deviceToken: deviceToken)
  }

  func application(_ application: UIApplication, didFailToRegisterForRemoteNotificationsWithError error: any Error) {
    environment.apns.didFailToRegister()
  }

  func userNotificationCenter(
    _ center: UNUserNotificationCenter,
    willPresent notification: UNNotification
  ) async -> UNNotificationPresentationOptions {
    guard let payload = PushPayload(userInfo: notification.request.content.userInfo), deduplicator.isNew(payload) else {
      return []
    }
    return [.banner, .sound]
  }

  func userNotificationCenter(_ center: UNUserNotificationCenter, didReceive response: UNNotificationResponse) async {
    guard let payload = PushPayload(userInfo: response.notification.request.content.userInfo),
      let link = payload.deepLink
    else { return }
    environment.shell.open(url: link)
  }
}
