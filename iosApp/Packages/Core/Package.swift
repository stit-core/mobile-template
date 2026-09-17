// swift-tools-version: 6.2
import PackageDescription

// Собирается только на Mac (SwiftUI/Shared); на Linux манифест читает archlint через swift package dump-package.
let package = Package(
  name: "Core",
  defaultLocalization: "ru",
  platforms: [.iOS(.v18)],
  products: [
    .library(name: "DesignSystem", targets: ["DesignSystem"]),
    .library(name: "Routing", targets: ["Routing"]),
    .library(name: "Persistence", targets: ["Persistence"]),
    .library(name: "Platform", targets: ["Platform"]),
    .library(name: "SharedBridge", targets: ["SharedBridge"]),
  ],
  dependencies: [
    .package(path: "../Pure"),
    .package(url: "https://github.com/getsentry/sentry-cocoa", from: "9.28.0"),
  ],
  targets: [
    .target(name: "Routing"),
    .target(
      name: "SharedBridge",
      dependencies: ["Routing", .product(name: "AppFoundation", package: "Pure")]
    ),
    .target(
      name: "DesignSystem",
      dependencies: [.product(name: "DesignSystemTokens", package: "Pure")],
      resources: [.process("Resources")]
    ),
    .target(name: "Persistence", dependencies: ["SharedBridge"]),
    .target(
      name: "Platform",
      dependencies: [
        "SharedBridge",
        .product(name: "AppFoundation", package: "Pure"),
        .product(name: "Sentry", package: "sentry-cocoa"),
      ]
    ),
    .testTarget(name: "RoutingTests", dependencies: ["Routing"]),
    .testTarget(name: "SharedBridgeTests", dependencies: ["SharedBridge"]),
    .testTarget(name: "DesignSystemTests", dependencies: ["DesignSystem"]),
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
