import Foundation
import Testing

@testable import ArchLintCore

@Suite("Manifest из dump-package")
struct ManifestTests {
  static let json = """
    {
      "name": "Core",
      "targets": [
        {"name": "SharedBridge", "type": "regular",
         "dependencies": [{"byName": ["Routing", null]}, {"product": ["AppFoundation", "Pure", null, null]}]},
        {"name": "RoutingTests", "type": "test", "dependencies": [{"target": ["Routing", null]}]},
        {"name": "Routing", "type": "regular", "dependencies": []}
      ],
      "products": [{"name": "Routing", "targets": ["Routing"], "type": {"library": ["automatic"]}}],
      "dependencies": [
        {"fileSystem": [{"identity": "pure", "path": "/x/Pure", "productFilter": null}]},
        {"sourceControl": [{"identity": "sentry-cocoa", "location": {"remote": [{"urlString": "https://example"}]},
          "requirement": {"range": [{"lowerBound": "9.28.0", "upperBound": "10.0.0"}]}}]}
      ]
    }
    """

  static func manifest(targetDependencies: String) -> Data {
    let json = """
      {"name": "X", "targets": [{"name": "A", "type": "regular", "dependencies": [\(targetDependencies)]}],
       "products": [], "dependencies": []}
      """
    return Data(json.utf8)
  }

  @Test("читает таргеты, зависимости трёх видов, продукты и пакеты")
  func decodes() throws {
    let manifest = try Manifest.parse(Data(Self.json.utf8))
    #expect(manifest.name == "Core")
    #expect(manifest.targets.map(\.name) == ["SharedBridge", "RoutingTests", "Routing"])
    #expect(manifest.targets[0].dependencies == [.byName("Routing"), .product(name: "AppFoundation", package: "Pure")])
    #expect(manifest.targets[1].isTest)
    #expect(manifest.targets[1].dependencies == [.target("Routing")])
    #expect(!manifest.targets[2].isTest)
    #expect(manifest.products == [Manifest.Product(name: "Routing", targets: ["Routing"])])
    #expect(
      manifest.dependencies == [
        Manifest.PackageDependency(identity: "pure", path: "/x/Pure"),
        Manifest.PackageDependency(identity: "sentry-cocoa"),
      ])
  }

  @Test("неизвестный вид зависимости — ошибка разбора")
  func rejectsUnknownKind() {
    #expect(throws: DecodingError.self) {
      try Manifest.parse(Self.manifest(targetDependencies: #"{"weird": ["B", null]}"#))
    }
  }

  @Test("пустая зависимость — ошибка разбора")
  func rejectsEmptyDependency() {
    #expect(throws: DecodingError.self) { try Manifest.parse(Self.manifest(targetDependencies: "{}")) }
  }
}
