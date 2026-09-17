import Foundation
import OpenAPIRuntime

/// Тонкая обёртка над сгенерированным клиентом: домен не знает про OpenAPI-типы.
public struct TodoApi: Sendable {
    private let client: any APIProtocol

    public init(client: any APIProtocol) {
        self.client = client
    }

    public func login(email: String, password: String) async throws {
        let response = try await client.login(
            body: .json(.init(email: email, password: password))
        )
        _ = try response.noContent
    }

    public func listTodos() async throws -> [Components.Schemas.Todo] {
        let response = try await client.listTodos()
        return try response.ok.body.json
    }
}
