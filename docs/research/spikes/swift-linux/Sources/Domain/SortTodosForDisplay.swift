/// Доменное правило: невыполненные сверху, внутри группы — по заголовку.
public struct SortTodosForDisplay: Sendable {
    public init() {}

    public func callAsFunction(_ items: [Todo]) -> [Todo] {
        items.sorted { lhs, rhs in
            if lhs.done != rhs.done { return !lhs.done }
            return lhs.title.lowercased() < rhs.title.lowercased()
        }
    }
}
