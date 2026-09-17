public import Foundation

// Подмножество JSON `swift package dump-package`: имена, типы и зависимости таргетов, продукты, зависимые пакеты.
public struct Manifest: Decodable, Equatable, Sendable {
  public struct Target: Decodable, Equatable, Sendable {
    public let name: String
    public let type: String
    public let dependencies: [Dependency]

    public init(name: String, type: String = "regular", dependencies: [Dependency] = []) {
      self.name = name
      self.type = type
      self.dependencies = dependencies
    }

    public var isTest: Bool { type == "test" }
  }

  public enum Dependency: Decodable, Equatable, Sendable {
    case byName(String)
    case target(String)
    case product(name: String, package: String?)

    // dump-package кодирует зависимость как {"byName": [name, null]} | {"target": [name, null]} | {"product": [name, package, null, null]}.
    public init(from decoder: any Decoder) throws {
      let raw = try decoder.singleValueContainer().decode([String: [String?]].self)
      guard let (kind, values) = raw.first, let name = values.first.flatMap({ $0 }) else {
        throw DecodingError.dataCorrupted(.init(codingPath: decoder.codingPath, debugDescription: "пустая зависимость"))
      }
      switch kind {
      case "byName": self = .byName(name)
      case "target": self = .target(name)
      case "product": self = .product(name: name, package: values.count > 1 ? values[1] : nil)
      default:
        throw DecodingError.dataCorrupted(
          .init(codingPath: decoder.codingPath, debugDescription: "неизвестный вид зависимости \(kind)"))
      }
    }
  }

  public struct Product: Decodable, Equatable, Sendable {
    public let name: String
    public let targets: [String]

    public init(name: String, targets: [String]) {
      self.name = name
      self.targets = targets
    }
  }

  public struct PackageDependency: Decodable, Equatable, Sendable {
    public let identity: String
    public let path: String?

    public init(identity: String, path: String? = nil) {
      self.identity = identity
      self.path = path
    }

    // {"fileSystem": [{identity, path}]} | {"sourceControl": [{identity, location…}]} — берём только identity и path.
    public init(from decoder: any Decoder) throws {
      let raw = try decoder.singleValueContainer().decode([String: [PackageDependencyInfo]].self)
      guard let info = raw.values.first?.first else {
        throw DecodingError.dataCorrupted(.init(codingPath: decoder.codingPath, debugDescription: "пустой пакет"))
      }
      identity = info.identity
      path = info.path
    }
  }

  public let name: String
  public let targets: [Target]
  public let products: [Product]
  public let dependencies: [PackageDependency]

  public init(name: String, targets: [Target], products: [Product] = [], dependencies: [PackageDependency] = []) {
    self.name = name
    self.targets = targets
    self.products = products
    self.dependencies = dependencies
  }

  public static func parse(_ json: Data) throws -> Self {
    try JSONDecoder().decode(Self.self, from: json)
  }
}

private struct PackageDependencyInfo: Decodable {
  let identity: String
  let path: String?
}
