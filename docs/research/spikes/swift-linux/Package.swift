// swift-tools-version: 6.2
import PackageDescription

let package = Package(
    name: "StitMobileCore",
    platforms: [
        .iOS(.v17),
        .macOS(.v14),
    ],
    products: [
        .library(name: "Domain", targets: ["Domain"]),
        .library(name: "Data", targets: ["Data"]),
        .library(name: "ApiClient", targets: ["ApiClient"]),
    ],
    dependencies: [
        .package(url: "https://github.com/apple/swift-openapi-generator", from: "1.13.1"),
        .package(url: "https://github.com/apple/swift-openapi-runtime", from: "1.12.1"),
        .package(url: "https://github.com/apple/swift-openapi-urlsession", from: "1.3.1"),
    ],
    targets: [
        .target(name: "Domain"),
        .target(name: "Data", dependencies: ["Domain"]),
        .target(
            name: "ApiClient",
            dependencies: [
                .product(name: "OpenAPIRuntime", package: "swift-openapi-runtime"),
                .product(name: "OpenAPIURLSession", package: "swift-openapi-urlsession"),
            ],
            plugins: [
                .plugin(name: "OpenAPIGenerator", package: "swift-openapi-generator")
            ]
        ),
        .testTarget(name: "DomainTests", dependencies: ["Domain"]),
        .testTarget(name: "DataTests", dependencies: ["Domain", "Data"]),
    ]
)
