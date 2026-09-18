// BGTaskScheduler поднимает только идентификаторы из BGTaskSchedulerPermittedIdentifiers, а shared знает задачу по slug:
// связь между ними — префикс bundle id, и она обязана читаться в обе стороны.
nonisolated public enum BackgroundTaskIdentifier {
  public static func qualified(bundleId: String, taskId: String) -> String {
    "\(bundleId).\(taskId)"
  }

  public static func taskId(from qualified: String, bundleId: String) -> String? {
    let prefix = "\(bundleId)."
    guard qualified.hasPrefix(prefix), qualified.count > prefix.count else { return nil }
    return String(qualified.dropFirst(prefix.count))
  }
}
