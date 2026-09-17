import SharedBridge

// Тот же demo-конфиг, что у схемы Demo: контейнер на MockTransport с выбранным сценарием фикстур mock/.
public enum Scenario {
  public static func container(
    _ scenario: MockScenario = .populated, platform: FakePlatformDeps = FakePlatformDeps()
  ) async throws -> SharedContainer {
    let config = AppConfig.companion.parse(
      env: "demo",
      apiBaseUrl: "https://api.demo.invalid",
      apiMode: "mock",
      authMode: "bearer",
      deepLinkHost: "app.demo.invalid",
      deepLinkScheme: "stit",
      sentryDsn: ""
    )
    let container = SharedContainer(platform: platform, config: config)
    try await container.demoMode?.apply(scenario: scenario)
    return container
  }
}
