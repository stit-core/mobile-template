import Foundation
import Shared

// Kotlin StateFlow → AsyncStream без SKIE: Swift-коллектор подписывается через Obj-C-экспорт kotlinx.coroutines.
extension KotlinStateFlow {
  nonisolated public func stream<State: AnyObject & Sendable>(of type: State.Type = State.self) -> AsyncStream<State> {
    AsyncStream { continuation in
      let collector = Collector<State>(continuation: continuation)
      let subscription = Task { try? await self.collect(collector: collector) }
      continuation.onTermination = { _ in subscription.cancel() }
    }
  }

  nonisolated public func current<State: AnyObject>(as type: State.Type = State.self) -> State {
    guard let state = value as? State else {
      preconditionFailure("StateFlow.value: ожидался \(State.self), получен \(String(describing: value))")
    }
    return state
  }
}

nonisolated private final class Collector<State: AnyObject & Sendable>: NSObject, KotlinFlowCollector {
  private let continuation: AsyncStream<State>.Continuation

  init(continuation: AsyncStream<State>.Continuation) {
    self.continuation = continuation
  }

  func emit(value: Any?) async throws {
    if let state = value as? State { continuation.yield(state) }
  }
}
