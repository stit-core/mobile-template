public struct Module: Hashable, Comparable, Sendable, CustomStringConvertible {
  public let package: String
  public let target: String

  public init(_ package: String, _ target: String) {
    self.package = package
    self.target = target
  }

  public var description: String { "\(package)/\(target)" }

  public static func < (lhs: Self, rhs: Self) -> Bool { lhs.description < rhs.description }
}

public struct Edge: Hashable, Comparable, Sendable, CustomStringConvertible {
  public let from: Module
  public let to: Module

  public init(_ from: Module, _ to: Module) {
    self.from = from
    self.to = to
  }

  public var description: String { "\(from) → \(to)" }

  public static func < (lhs: Self, rhs: Self) -> Bool { lhs.description < rhs.description }
}

// Граф «таргет → таргет» по манифестам: продукты локальных пакетов раскрываются в их таргеты, внешние — узел <identity>/<product>.
public struct ArchGraph: Sendable {
  public let modules: [Module]
  public let edges: [Edge]
  public let testModules: Set<Module>

  public init(manifests: [Manifest]) {
    var modules: [Module] = []
    var edges = Set<Edge>()
    var tests = Set<Module>()
    for manifest in manifests {
      for target in manifest.targets {
        let module = Module(manifest.name, target.name)
        modules.append(module)
        if target.isTest { tests.insert(module) }
        for dependency in target.dependencies {
          for resolved in Self.resolve(dependency, in: manifest, all: manifests) {
            edges.insert(Edge(module, resolved))
          }
        }
      }
    }
    self.modules = modules.sorted()
    self.edges = edges.sorted()
    testModules = tests
  }

  public func dependencies(of module: Module) -> [Module] {
    edges.filter { $0.from == module }.map(\.to)
  }

  // Цикл возвращается путём модулей; пустой массив — граф ацикличен.
  public func firstCycle() -> [Module] {
    var visiting = Set<Module>()
    var done = Set<Module>()
    var stack: [Module] = []

    func visit(_ module: Module) -> [Module]? {
      if visiting.contains(module) {
        let start = stack.firstIndex(of: module) ?? 0
        return Array(stack[start...]) + [module]
      }
      if done.contains(module) { return nil }
      visiting.insert(module)
      stack.append(module)
      for next in dependencies(of: module) {
        if let cycle = visit(next) { return cycle }
      }
      stack.removeLast()
      visiting.remove(module)
      done.insert(module)
      return nil
    }

    for module in modules {
      if let cycle = visit(module) { return cycle }
    }
    return []
  }

  private static func resolve(_ dependency: Manifest.Dependency, in manifest: Manifest, all: [Manifest]) -> [Module] {
    switch dependency {
    case .target(let name):
      return [Module(manifest.name, name)]
    case .byName(let name):
      if manifest.targets.contains(where: { $0.name == name }) { return [Module(manifest.name, name)] }
      return product(named: name, package: nil, from: manifest, all: all)
    case .product(let name, let package):
      return product(named: name, package: package, from: manifest, all: all)
    }
  }

  private static func product(
    named name: String, package: String?, from manifest: Manifest, all: [Manifest]
  ) -> [Module] {
    let candidates = all.filter { other in
      other.name != manifest.name
        && manifest.dependencies.contains { $0.identity == other.name.lowercased() }
        && (package == nil || package?.lowercased() == other.name.lowercased())
    }
    for candidate in candidates {
      if let product = candidate.products.first(where: { $0.name == name }) {
        return product.targets.map { Module(candidate.name, $0) }
      }
    }
    return [Module(package ?? "external", name)]
  }
}
