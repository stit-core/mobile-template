// swift-tools-version: 6.2
import PackageDescription

// Фейки PlatformDeps, сценарии MockTransport и конфиг snapshot-тестов; только для тест-таргетов Core/Features.
let package = Package(
  name: "TestSupport",
  platforms: [.iOS(.v18)],
  products: [.library(name: "TestSupport", targets: ["TestSupport"])],
  dependencies: [
    .package(path: "../Core"),
    .package(path: "../Pure"),
    .package(url: "https://github.com/pointfreeco/swift-snapshot-testing", from: "1.19.5"),
  ],
  targets: [
    .target(
      name: "TestSupport",
      dependencies: [
        .product(name: "SharedBridge", package: "Core"),
        .product(name: "AppFoundation", package: "Pure"),
        .product(name: "SnapshotTesting", package: "swift-snapshot-testing"),
      ]
    )
  ]
)

// Одна политика строгости на все таргеты: Swift 6 mode, MainActor по умолчанию, предупреждения = ошибки.
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
