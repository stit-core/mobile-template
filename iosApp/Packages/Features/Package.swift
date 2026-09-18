// swift-tools-version: 6.2
import PackageDescription

// Новая фича = таргет через feature(_:) + строка в umbrella "Features"; .pbxproj не трогается.
let featureNames = ["Shell", "AuthFeature", "ProfileFeature", "TodosFeature", "SettingsFeature"]

// Вью виджетов системы стоят отдельно от экранов: они рисуют Codable-снимок из App Group и Shared не линкуют,
// иначе Kotlin-фреймворк уехал бы внутрь расширения.
let widgetsFeatureName = "WidgetsFeature"

func feature(_ name: String) -> Target {
  .target(
    name: name,
    dependencies: [
      .product(name: "DesignSystem", package: "Core"),
      .product(name: "Routing", package: "Core"),
      .product(name: "SharedBridge", package: "Core"),
      .product(name: "AppFoundation", package: "Pure"),
    ],
    resources: [.process("Resources")]
  )
}

func featureTests(_ name: String) -> Target {
  .testTarget(
    name: "\(name)Tests",
    dependencies: [.target(name: name), .product(name: "TestSupport", package: "TestSupport")]
  )
}

func widgetsFeature() -> Target {
  .target(
    name: widgetsFeatureName,
    dependencies: [
      .product(name: "DesignSystem", package: "Core"),
      .product(name: "Widgets", package: "Core"),
      .product(name: "AppFoundation", package: "Pure"),
    ],
    resources: [.process("Resources")]
  )
}

let package = Package(
  name: "Features",
  defaultLocalization: "ru",
  platforms: [.iOS(.v18)],
  products: [.library(name: "Features", targets: ["Features"])],
  dependencies: [
    .package(path: "../Core"),
    .package(path: "../Pure"),
    .package(path: "../TestSupport"),
  ],
  targets: featureNames.map(feature) + featureNames.map(featureTests) + [
    widgetsFeature(),
    // Тест вью виджета обходится без TestSupport: сценариев MockTransport у снимка нет, есть запись из App Group.
    .testTarget(
      name: "\(widgetsFeatureName)Tests",
      dependencies: [.target(name: widgetsFeatureName), .product(name: "Widgets", package: "Core")]
    ),
    .target(
      name: "Features",
      dependencies: (featureNames + [widgetsFeatureName]).map { .target(name: $0) }
    ),
    .testTarget(
      name: "SnapshotTests",
      dependencies: [
        "Features",
        .product(name: "Widgets", package: "Core"),
        .product(name: "TestSupport", package: "TestSupport"),
      ]
    ),
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
