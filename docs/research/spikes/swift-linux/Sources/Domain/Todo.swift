import Foundation

public struct TodoID: Hashable, Sendable, RawRepresentable {
    public let rawValue: String
    public init(rawValue: String) { self.rawValue = rawValue }
    public init(_ value: String) { self.rawValue = value }
}

public struct Todo: Equatable, Sendable, Identifiable {
    public let id: TodoID
    public let title: String
    public let done: Bool

    public init(id: TodoID, title: String, done: Bool = false) {
        self.id = id
        self.title = title
        self.done = done
    }
}

public enum TodoError: Error, Equatable {
    case emptyTitle
    case notFound(TodoID)
}
