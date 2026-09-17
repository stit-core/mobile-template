package tech.stit.mobile.testing

import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import okio.fakefilesystem.FakeFileSystem
import tech.stit.mobile.SharedContainer
import tech.stit.mobile.core.config.ApiMode
import tech.stit.mobile.core.config.AppConfig
import tech.stit.mobile.core.config.AppEnv
import tech.stit.mobile.core.config.AuthMode
import tech.stit.mobile.core.mock.MockScenario

fun testConfig(apiMode: ApiMode = ApiMode.Mock, authMode: AuthMode = AuthMode.Bearer, env: AppEnv = AppEnv.Demo) = AppConfig(
    env = env,
    apiBaseUrl = "https://api.demo.invalid",
    apiMode = apiMode,
    authMode = authMode,
    deepLinkHost = "app.demo.invalid",
    deepLinkScheme = "stit",
    sentryDsn = "",
)

// Контейнер на MockTransport и фейках платформы: сценарные тесты презентеров идут через весь стек без сети.
suspend fun TestScope.testContainer(
    scenario: MockScenario = MockScenario.Populated,
    platform: FakePlatformDeps = FakePlatformDeps(),
    config: AppConfig = testConfig(),
    clock: FakeClock = FakeClock(),
    fileSystem: FakeFileSystem = FakeFileSystem(),
): SharedContainer {
    val container = SharedContainer(platform, config, TestDispatchers(StandardTestDispatcher(testScheduler)), clock, fileSystem)
    container.mockTransport?.reset(scenario)
    return container
}

// Вход как у пользователя demo: любые непустые email и пароль принимаются MockTransport.
suspend fun SharedContainer.signIn() = sessionRepository.login("demo@example.com", "password")
