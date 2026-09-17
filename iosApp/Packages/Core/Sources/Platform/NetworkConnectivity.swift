import Foundation
import Network
import SharedBridge

// NWPathMonitor → ConnectivityMonitor из Shared: Kotlin-holder ConnectivityState даёт StateFlow<Boolean> презентерам.
nonisolated public final class NetworkConnectivity: ConnectivityMonitor {
  public let online: any KotlinStateFlow
  private let state = ConnectivityState(initial: true)
  private let monitor = NWPathMonitor()

  public init() {
    online = state.online
    monitor.pathUpdateHandler = { [state] path in state.update(online: path.status == .satisfied) }
    monitor.start(queue: DispatchQueue(label: "tech.stit.mobile.connectivity", qos: .utility))
  }

  deinit {
    monitor.cancel()
  }
}
