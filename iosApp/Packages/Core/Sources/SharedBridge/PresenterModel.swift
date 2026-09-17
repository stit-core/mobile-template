public import Observation
import Shared
import Synchronization

// Тонкий @Observable-адаптер презентера: одно состояние вниз, интенты вверх; презентер закрывается вместе с моделью.
@Observable
open class PresenterModel<State: AnyObject & Sendable, Intent: AnyObject> {
  public private(set) var state: State
  private let send: (Intent) -> Void
  private let close: @Sendable () -> Void
  // deinit не изолирован на MainActor — задача-подписка живёт за Mutex, чтобы отменяться без unsafe.
  private let subscription = Mutex<Task<Void, Never>?>(nil)

  public init(
    stateFlow: any KotlinStateFlow,
    dispatch: @escaping (Intent) -> Void,
    close: @escaping @Sendable () -> Void
  ) {
    state = stateFlow.current()
    send = dispatch
    self.close = close
    subscription.withLock {
      $0 = Task { [weak self] in
        for await next in stateFlow.stream(of: State.self) {
          self?.state = next
        }
      }
    }
  }

  deinit {
    subscription.withLock { $0?.cancel() }
    close()
  }

  public func dispatch(_ intent: Intent) {
    send(intent)
  }
}
