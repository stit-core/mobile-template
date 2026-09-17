// swift-tools-version: 6.2
import PackageDescription

// Валидатор графа SwiftPM-пакетов над `swift package dump-package`; работает на Linux, входит в make check.
let package = Package(
  name: "archlint",
  platforms: [.macOS(.v15)],
  products: [.executable(name: "archlint", targets: ["archlint"])],
  targets: [
    .target(name: "ArchLintCore"),
    .executableTarget(name: "archlint", dependencies: ["ArchLintCore"]),
    .testTarget(name: "ArchLintCoreTests", dependencies: ["ArchLintCore"]),
  ]
)

// Та же политика строгости, что у пакетов приложения.
for target in package.targets {
  target.swiftSettings =
    (target.swiftSettings ?? []) + [
      .defaultIsolation(MainActor.self),
      .enableUpcomingFeature("NonisolatedNonsendingByDefault"),
      .enableUpcomingFeature("InferIsolatedConformances"),
      .enableUpcomingFeature("ExistentialAny"),
      .enableUpcomingFeature("InternalImportsByDefault"),
      .enableUpcomingFeature("MemberImportVisibility"),
      .swiftLanguageMode(.v6),
      .treatAllWarnings(as: .error),
    ]
}
