public struct Violation: Equatable, Sendable, CustomStringConvertible {
  public let rule: String
  public let detail: String

  public init(rule: String, detail: String) {
    self.rule = rule
    self.detail = detail
  }

  public var description: String { "\(detail): \(rule)" }
}

// Матрица зависимостей iOS из docs/reference/architecture.md; исключение из «фича ✗ фича» — umbrella и тесты своей фичи.
public enum ArchRules {
  public static let pure = "Pure"
  public static let core = "Core"
  public static let features = "Features"
  public static let testSupport = "TestSupport"
  public static let umbrella = "Features"
  static let platformTargets: Set<String> = ["Persistence", "Platform"]

  public static func check(_ graph: ArchGraph) -> [Violation] {
    var violations = graph.edges.compactMap { edge in check(edge, isTest: graph.testModules.contains(edge.from)) }
    let cycle = graph.firstCycle()
    if !cycle.isEmpty {
      violations.append(
        Violation(rule: "граф пакетов должен быть ацикличен", detail: cycle.map(\.description).joined(separator: " → "))
      )
    }
    return violations
  }

  static func check(_ edge: Edge, isTest: Bool) -> Violation? {
    let from = edge.from
    let to = edge.to
    switch from.package {
    case pure where to.package != pure:
      return Violation(rule: "Pure зависит только от Foundation", detail: edge.description)
    case core where to.package == features:
      return Violation(rule: "Core не импортирует Features", detail: edge.description)
    case testSupport where to.package == features:
      return Violation(rule: "TestSupport не импортирует Features", detail: edge.description)
    case features where to.package == core && platformTargets.contains(to.target):
      return Violation(rule: "Features не импортируют Core/Persistence и Core/Platform", detail: edge.description)
    case features where to.package == features && !featureMayDepend(from: from, on: to, isTest: isTest):
      return Violation(rule: "фича не зависит от другой фичи", detail: edge.description)
    default:
      return nil
    }
  }

  // Umbrella собирает все фичи; тест фичи видит только свою фичу; SnapshotTests — только umbrella.
  private static func featureMayDepend(from: Module, on to: Module, isTest: Bool) -> Bool {
    if from.target == umbrella { return true }
    guard isTest else { return false }
    let owner = from.target.hasSuffix("Tests") ? String(from.target.dropLast("Tests".count)) : from.target
    return to.target == owner || to.target == umbrella
  }
}
