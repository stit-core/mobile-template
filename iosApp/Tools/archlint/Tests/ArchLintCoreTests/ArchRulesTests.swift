import Testing

@testable import ArchLintCore

// Модель дерева iosApp/Packages в миниатюре; каждое правило проверяется добавлением одного запрещённого ребра.
enum Fixture {
  static func target(_ name: String, type: String = "regular", _ deps: [Manifest.Dependency] = []) -> Manifest.Target {
    Manifest.Target(name: name, type: type, dependencies: deps)
  }

  static func product(_ name: String) -> Manifest.Product { Manifest.Product(name: name, targets: [name]) }

  static let pure = Manifest(
    name: "Pure",
    targets: [target("AppFoundation"), target("DesignSystemTokens")],
    products: [product("AppFoundation"), product("DesignSystemTokens")]
  )

  static func core(extra: [Manifest.Dependency] = []) -> Manifest {
    Manifest(
      name: "Core",
      targets: [
        target("Routing"),
        target("SharedBridge", [.byName("Routing"), .product(name: "AppFoundation", package: "Pure")] + extra),
        target("Platform", [.byName("SharedBridge"), .product(name: "Sentry", package: "sentry-cocoa")]),
        target("Persistence", [.byName("SharedBridge")]),
        target("RoutingTests", type: "test", [.target("Routing")]),
      ],
      products: [product("Routing"), product("SharedBridge"), product("Platform"), product("Persistence")],
      dependencies: [
        .init(identity: "pure", path: "/Pure"), .init(identity: "sentry-cocoa"), .init(identity: "features"),
      ]
    )
  }

  static func features(auth extra: [Manifest.Dependency] = [], authTests: [Manifest.Dependency] = []) -> Manifest {
    Manifest(
      name: "Features",
      targets: [
        target("AuthFeature", [.product(name: "SharedBridge", package: "Core")] + extra),
        target("TodosFeature", [.product(name: "SharedBridge", package: "Core")]),
        target("Features", [.target("AuthFeature"), .target("TodosFeature")]),
        target(
          "AuthFeatureTests", type: "test",
          [.target("AuthFeature"), .product(name: "TestSupport", package: "TestSupport")] + authTests),
        target("SnapshotTests", type: "test", [.byName("Features")]),
      ],
      products: [product("Features")],
      dependencies: [.init(identity: "core", path: "/Core"), .init(identity: "testsupport", path: "/TestSupport")]
    )
  }

  static func testSupport(extra: [Manifest.Dependency] = []) -> Manifest {
    Manifest(
      name: "TestSupport",
      targets: [target("TestSupport", [.product(name: "SharedBridge", package: "Core")] + extra)],
      products: [product("TestSupport")],
      dependencies: [.init(identity: "core", path: "/Core"), .init(identity: "features", path: "/Features")]
    )
  }

  static func graph(
    pure: Manifest = pure, core: Manifest = core(), features: Manifest = features(),
    testSupport: Manifest = testSupport()
  ) -> ArchGraph {
    ArchGraph(manifests: [pure, core, features, testSupport])
  }
}

@Suite("ArchGraph")
struct ArchGraphTests {
  @Test("продукт локального пакета раскрывается в его таргет")
  func resolvesLocalProduct() {
    let graph = Fixture.graph()
    #expect(graph.edges.contains(Edge(Module("Core", "SharedBridge"), Module("Pure", "AppFoundation"))))
    #expect(graph.edges.contains(Edge(Module("Features", "AuthFeature"), Module("Core", "SharedBridge"))))
  }

  @Test("byName: сначала таргет своего пакета, потом продукт соседа")
  func resolvesByName() {
    let graph = Fixture.graph()
    #expect(graph.edges.contains(Edge(Module("Core", "Platform"), Module("Core", "SharedBridge"))))
    #expect(graph.edges.contains(Edge(Module("Features", "SnapshotTests"), Module("Features", "Features"))))
  }

  @Test("внешний продукт — узел <пакет>/<продукт>")
  func externalProduct() {
    let graph = Fixture.graph()
    #expect(graph.edges.contains(Edge(Module("Core", "Platform"), Module("sentry-cocoa", "Sentry"))))
  }

  @Test("тестовые таргеты помечены")
  func testTargets() {
    let graph = Fixture.graph()
    #expect(graph.testModules.contains(Module("Core", "RoutingTests")))
    #expect(!graph.testModules.contains(Module("Core", "Routing")))
  }

  @Test("модули и рёбра отсортированы, без дублей")
  func sortedAndUnique() {
    let graph = Fixture.graph()
    #expect(graph.modules == graph.modules.sorted())
    #expect(graph.edges == graph.edges.sorted())
    #expect(Set(graph.edges).count == graph.edges.count)
  }

  @Test("цикл находится и печатается путём")
  func cycle() {
    let cyclic = Manifest(
      name: "P",
      targets: [
        Fixture.target("A", [.target("B")]), Fixture.target("B", [.target("C")]), Fixture.target("C", [.target("A")]),
      ])
    let cycle = ArchGraph(manifests: [cyclic]).firstCycle()
    #expect(cycle.first == cycle.last)
    #expect(cycle.count == 4)
    #expect(Fixture.graph().firstCycle().isEmpty)
  }
}

@Suite("ArchRules")
struct ArchRulesTests {
  @Test("эталонное дерево без нарушений")
  func cleanTree() {
    #expect(ArchRules.check(Fixture.graph()).isEmpty)
  }

  @Test("фича → фича")
  func featureToFeature() {
    let violations = ArchRules.check(Fixture.graph(features: Fixture.features(auth: [.target("TodosFeature")])))
    #expect(violations.map(\.rule) == ["фича не зависит от другой фичи"])
    #expect(violations[0].detail == "Features/AuthFeature → Features/TodosFeature")
  }

  @Test("тест фичи видит только свою фичу")
  func featureTestsOwnFeatureOnly() {
    let violations = ArchRules.check(Fixture.graph(features: Fixture.features(authTests: [.target("TodosFeature")])))
    #expect(violations.map(\.detail) == ["Features/AuthFeatureTests → Features/TodosFeature"])
  }

  @Test("фича → Core/Platform и Core/Persistence")
  func featureToPlatform() {
    let features = Fixture.features(auth: [
      .product(name: "Platform", package: "Core"), .product(name: "Persistence", package: "Core"),
    ])
    let violations = ArchRules.check(Fixture.graph(features: features))
    #expect(violations.count == 2)
    #expect(violations.allSatisfy { $0.rule == "Features не импортируют Core/Persistence и Core/Platform" })
  }

  @Test("Core → Features")
  func coreToFeatures() {
    let violations = ArchRules.check(
      Fixture.graph(core: Fixture.core(extra: [.product(name: "Features", package: "Features")])))
    #expect(violations.map(\.rule) == ["Core не импортирует Features", "граф пакетов должен быть ацикличен"])
  }

  @Test("Pure — только Foundation")
  func pureFoundationOnly() {
    let pure = Manifest(
      name: "Pure",
      targets: [Fixture.target("AppFoundation", [.product(name: "Routing", package: "Core")])],
      products: [Fixture.product("AppFoundation")],
      dependencies: [.init(identity: "core", path: "/Core")]
    )
    let violations = ArchRules.check(Fixture.graph(pure: pure))
    #expect(violations.map(\.rule) == ["Pure зависит только от Foundation"])
  }

  @Test("TestSupport → Features")
  func testSupportToFeatures() {
    let violations = ArchRules.check(
      Fixture.graph(testSupport: Fixture.testSupport(extra: [.product(name: "Features", package: "Features")])))
    #expect(violations.map(\.rule) == ["TestSupport не импортирует Features"])
  }

  @Test("цикл — нарушение с путём")
  func cycleViolation() {
    let cyclic = Manifest(
      name: "P", targets: [Fixture.target("A", [.target("B")]), Fixture.target("B", [.target("A")])])
    let violations = ArchRules.check(ArchGraph(manifests: [cyclic]))
    #expect(violations.map(\.rule) == ["граф пакетов должен быть ацикличен"])
    #expect(violations[0].detail.contains("P/A → P/B"))
  }
}

@Suite("Report")
struct ReportTests {
  @Test("OK-строка со статистикой")
  func okLine() {
    let graph = Fixture.graph()
    #expect(
      Report.lines(for: graph, violations: []) == [
        "OK: archlint — модулей \(graph.modules.count), рёбер \(graph.edges.count)"
      ])
  }

  @Test("FAIL-строки: заголовок и по строке на нарушение")
  func failLines() {
    let violation = Violation(rule: "r", detail: "A → B")
    #expect(
      Report.lines(for: Fixture.graph(), violations: [violation]) == ["FAIL: archlint — нарушений 1", "  A → B: r"])
  }

  @Test("mermaid-граф")
  func mermaid() {
    let text = Report.mermaid(Fixture.graph())
    #expect(text.hasPrefix("graph LR\n"))
    #expect(text.contains("Core_SharedBridge[\"Core/SharedBridge\"] --> Pure_AppFoundation[\"Pure/AppFoundation\"]"))
  }
}
