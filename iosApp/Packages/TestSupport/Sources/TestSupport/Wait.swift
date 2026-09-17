import Foundation

public enum WaitError: Error {
  case timeout(String)
}

// Состояние презентера меняется асинхронно (корутины Kotlin): тесты ждут условие, а не спят фиксированно.
public func waitUntil(
  _ description: String = "условие",
  timeout: Duration = .seconds(3),
  _ condition: @MainActor () -> Bool
) async throws {
  let deadline = ContinuousClock.now + timeout
  while !condition() {
    guard ContinuousClock.now < deadline else { throw WaitError.timeout(description) }
    try await Task.sleep(for: .milliseconds(20))
  }
}
