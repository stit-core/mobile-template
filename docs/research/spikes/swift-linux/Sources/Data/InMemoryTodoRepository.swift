import Domain
import Foundation

public actor InMemoryTodoRepository: TodoRepository {
    private var storage: [Todo]
    private var counter: Int

    public init(seed: [Todo] = []) {
        self.storage = seed
        self.counter = seed.count
    }

    public func all() async throws -> [Todo] { storage }

    public func add(title: String) async throws -> Todo {
        let trimmed = title.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !trimmed.isEmpty else { throw TodoError.emptyTitle }
        counter += 1
        let todo = Todo(id: TodoID("todo-\(counter)"), title: trimmed)
        storage.append(todo)
        return todo
    }

    public func toggle(id: TodoID) async throws -> Todo {
        guard let index = storage.firstIndex(where: { $0.id == id }) else {
            throw TodoError.notFound(id)
        }
        let current = storage[index]
        let updated = Todo(id: current.id, title: current.title, done: !current.done)
        storage[index] = updated
        return updated
    }
}
