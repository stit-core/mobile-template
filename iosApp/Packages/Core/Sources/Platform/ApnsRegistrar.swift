public import Foundation
import SharedBridge
import Synchronization
import UIKit

// APNs-токен приходит в AppDelegate; PushRegistrar из Shared забирает его через PushTransport.token().
nonisolated public final class ApnsRegistrar: PushTransport {
  public let provider: PushProvider = .apns
  private let token = Mutex<String?>(nil)
  private let waiters = Mutex<[CheckedContinuation<String?, Never>]>([])

  public init() {}

  public func token() async throws -> String? {
    if let known = token.withLock({ $0 }) { return known }
    await MainActor.run { UIApplication.shared.registerForRemoteNotifications() }
    return await withCheckedContinuation { continuation in
      waiters.withLock { $0.append(continuation) }
    }
  }

  public func didRegister(deviceToken: Data) {
    let hex = deviceToken.map { String(format: "%02x", $0) }.joined()
    token.withLock { $0 = hex }
    resumeWaiters(with: hex)
  }

  public func didFailToRegister() {
    resumeWaiters(with: nil)
  }

  private func resumeWaiters(with value: String?) {
    let pending = waiters.withLock { waiters in
      defer { waiters.removeAll() }
      return waiters
    }
    for continuation in pending { continuation.resume(returning: value) }
  }
}
