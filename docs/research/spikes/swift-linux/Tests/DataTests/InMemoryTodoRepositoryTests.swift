import Domain
import Testing

@testable import Data

@Suite("InMemoryTodoRepository")
struct InMemoryTodoRepositoryTests {
    @Test("add добавляет запись и возвращает её")
    func addsTodo() async throws {
        let repo = InMemoryTodoRepository()
        let created = try await repo.add(title: "Прочитать бриф")
        #expect(created.title == "Прочитать бриф")
        #expect(try await repo.all().count == 1)
    }

    @Test("пустой заголовок отвергается")
    func rejectsEmptyTitle() async {
        let repo = InMemoryTodoRepository()
        await #expect(throws: TodoError.emptyTitle) {
            _ = try await repo.add(title: "   ")
        }
    }

    @Test("toggle переключает флаг done")
    func togglesDone() async throws {
        let repo = InMemoryTodoRepository(seed: [Todo(id: TodoID("1"), title: "alpha")])
        let toggled = try await repo.toggle(id: TodoID("1"))
        #expect(toggled.done)
    }

    @Test("toggle несуществующего id бросает notFound")
    func togglesMissing() async {
        let repo = InMemoryTodoRepository()
        await #expect(throws: TodoError.notFound(TodoID("nope"))) {
            _ = try await repo.toggle(id: TodoID("nope"))
        }
    }
}
