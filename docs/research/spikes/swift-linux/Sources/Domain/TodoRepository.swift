public protocol TodoRepository: Sendable {
    func all() async throws -> [Todo]
    func add(title: String) async throws -> Todo
    func toggle(id: TodoID) async throws -> Todo
}
