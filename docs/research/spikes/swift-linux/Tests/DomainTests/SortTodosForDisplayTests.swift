import Testing

@testable import Domain

@Suite("SortTodosForDisplay")
struct SortTodosForDisplayTests {
    @Test("невыполненные идут первыми, внутри группы — по заголовку")
    func ordering() {
        let sorted = SortTodosForDisplay()([
            Todo(id: TodoID("1"), title: "Beta", done: true),
            Todo(id: TodoID("2"), title: "alpha"),
            Todo(id: TodoID("3"), title: "Gamma"),
        ])
        #expect(sorted.map(\.title) == ["alpha", "Gamma", "Beta"])
    }

    @Test(
        "регистр в заголовке не влияет на порядок",
        arguments: [
            (["b", "A"], ["A", "b"]),
            (["Z", "a"], ["a", "Z"]),
            (["mm", "MA"], ["MA", "mm"]),
        ])
    func caseInsensitive(input: [String], expected: [String]) {
        let items = input.enumerated().map { Todo(id: TodoID("\($0.offset)"), title: $0.element) }
        #expect(SortTodosForDisplay()(items).map(\.title) == expected)
    }
}
