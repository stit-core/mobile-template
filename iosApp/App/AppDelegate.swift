import Platform
import SharedBridge
import UIKit
import UserNotifications

// APNs-токен, тап по уведомлению и регистрация фоновых задач приходят только через делегат: остальное живёт в SwiftUI-сцене.
final class AppDelegate: NSObject, UIApplicationDelegate, UNUserNotificationCenterDelegate {
  let environment = AppEnvironment.live()
  private let deduplicator = NotificationDeduplicator()

  func application(
    _ application: UIApplication,
    didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey: Any]? = nil
  ) -> Bool {
    UNUserNotificationCenter.current().delegate = self
    NotificationCategories.register()
    // BGTaskScheduler принимает регистрацию только до конца запуска — позже система задачу уже не поднимет.
    environment.registerBackgroundTasks()
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
    let request = response.notification.request
    let payload = PushPayload(userInfo: request.content.userInfo)
    let handled = await NotificationActions.handle(
      action: response.actionIdentifier,
      payload: payload,
      notificationId: request.identifier,
      container: environment.container
    )
    guard !handled, let link = payload?.deepLink else { return }
    environment.shell.open(url: link)
  }
}
