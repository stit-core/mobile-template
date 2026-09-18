package tech.stit.mobile.testing

import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.plugins.api.ClientPlugin
import io.ktor.client.plugins.api.createClientPlugin
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import okio.fakefilesystem.FakeFileSystem
import tech.stit.mobile.SharedContainer
import tech.stit.mobile.core.api.ApiClient
import tech.stit.mobile.core.api.platformHttpEngine
import tech.stit.mobile.core.config.ApiMode
import tech.stit.mobile.core.config.AppConfig
import tech.stit.mobile.core.config.AppEnv
import tech.stit.mobile.core.config.AuthMode
import tech.stit.mobile.core.mock.MockScenario
import tech.stit.mobile.core.observability.Logger

fun testConfig(apiMode: ApiMode = ApiMode.Mock, authMode: AuthMode = AuthMode.Bearer, env: AppEnv = AppEnv.Demo) = AppConfig(
    env = env,
    apiBaseUrl = "https://api.demo.invalid",
    apiMode = apiMode,
    authMode = authMode,
    deepLinkHost = "app.demo.invalid",
    deepLinkScheme = "stit",
    sentryDsn = "",
)

// ApiClient на подставном движке и фейках платформы; auth по умолчанию — заглушка, чтобы тест ресурса не зависел от SessionStore.
internal fun testApiClient(engine: HttpClientEngine, auth: ClientPlugin<Unit> = createClientPlugin("NoopAuth") {}): ApiClient =
    ApiClient(testConfig(), FakePlatformDeps().appInfo, FakeLocale, engine, auth, FakeTelemetry(), Logger("test", MemoryLogSink()))

// Контейнер на MockTransport и фейках платформы: сценарные тесты презентеров идут через весь стек без сети.
// backend подставляет «настоящий» бэкенд вместо движка платформы — так тестируется обычная сборка (ApiMode.Real).
suspend fun TestScope.testContainer(
    scenario: MockScenario = MockScenario.Populated,
    platform: FakePlatformDeps = FakePlatformDeps(),
    config: AppConfig = testConfig(),
    clock: FakeClock = FakeClock(),
    fileSystem: FakeFileSystem = FakeFileSystem(),
    backend: FakeBackend? = null,
): SharedContainer {
    val container = SharedContainer(platform, config, TestDispatchers(StandardTestDispatcher(testScheduler)), clock, fileSystem) {
        backend?.engine ?: platformHttpEngine()
    }
    container.mockTransport?.reset(scenario)
    return container
}

// Обычная сборка (не demo-флейвор) на подставном бэкенде: среда prod, API_MODE=real, тот же стек shared.
suspend fun TestScope.prodContainer(
    platform: FakePlatformDeps = FakePlatformDeps(),
    backend: FakeBackend = FakeBackend(StandardTestDispatcher(testScheduler)),
    clock: FakeClock = FakeClock(),
    fileSystem: FakeFileSystem = FakeFileSystem(),
): SharedContainer = testContainer(
    platform = platform,
    config = testConfig(apiMode = ApiMode.Real, env = AppEnv.Prod),
    clock = clock,
    fileSystem = fileSystem,
    backend = backend,
)

// Вход как у пользователя demo: любые непустые email и пароль принимаются MockTransport.
suspend fun SharedContainer.signIn() = sessionRepository.login("demo@example.com", "password")
