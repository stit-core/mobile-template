// swift-tools-version: 6.2
import PackageDescription

// Единственный пакет iOS, который собирается и тестируется на Linux: только Foundation, без SwiftUI/UIKit/Shared.
let package = Package(
  name: "Pure",
  platforms: [.iOS(.v18), .macOS(.v15)],
  products: [
    .library(name: "AppFoundation", targets: ["AppFoundation"]),
    .library(name: "DesignSystemTokens", targets: ["DesignSystemTokens"]),
  ],
  targets: [
    .target(name: "AppFoundation"),
    .target(name: "DesignSystemTokens"),
    .testTarget(name: "AppFoundationTests", dependencies: ["AppFoundation"]),
    .testTarget(name: "DesignSystemTokensTests", dependencies: ["DesignSystemTokens"]),
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
