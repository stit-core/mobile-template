package tech.stit.mobile.core.mock

import io.ktor.client.engine.mock.MockEngine
import kotlinx.coroutines.test.runTest
import tech.stit.mobile.core.api.ApiError
import tech.stit.mobile.core.api.TodosApi
import tech.stit.mobile.core.auth.AuthState
import tech.stit.mobile.core.config.ApiMode
import tech.stit.mobile.core.config.AuthMode
import tech.stit.mobile.testing.FakePlatformDeps
import tech.stit.mobile.testing.signIn
import tech.stit.mobile.testing.testConfig
import tech.stit.mobile.testing.testContainer
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class MockTransportTest {
    @Test
    fun demoModeNeverTouchesTheNetwork() = runTest {
        val demo = testContainer()
        assertTrue(demo.apiClient.http.engine is MockEngine)
        assertNotNull(demo.mockTransport)
        assertNotNull(demo.demoMode)

        val real = testContainer(config = testConfig(apiMode = ApiMode.Real))
        assertNull(real.mockTransport)
        assertNull(real.demoMode)
        assertFalse(real.apiClient.http.engine is MockEngine)
    }

    @Test
    fun scenariosServeTheirFixtures() = runTest {
        val populated = testContainer(MockScenario.Populated)
        populated.signIn()
        assertEquals(3, populated.todoRepository.refresh().size)
        assertEquals("Демо Пользователь", populated.userRepository.refresh().name)

        val empty = testContainer(MockScenario.Empty)
        empty.signIn()
        assertEquals(0, empty.todoRepository.refresh().size)
    }

    @Test
    fun errorScenarioFailsEveryRequestWithProblemJson() = runTest {
        val container = testContainer(MockScenario.Error)
        val error = assertFailsWith<ApiError.Http> { container.signIn() }
        assertEquals(500, error.status)
        assertEquals("INTERNAL", error.code)
        assertEquals("internal error", error.detail)
    }

    @Test
    fun protectedRoutesRequireASession() = runTest {
        val container = testContainer()
        val error = assertFailsWith<ApiError.Http> { container.todoRepository.refresh() }
        assertEquals(401, error.status)
        assertEquals("AUTH_REQUIRED", error.code)
    }

    @Test
    fun loginRejectsBlankCredentials() = runTest {
        val container = testContainer()
        val error = assertFailsWith<ApiError.Http> { container.sessionRepository.login(" ", "x") }
        assertEquals("AUTH_INVALID_CREDENTIALS", error.code)
    }

    @Test
    fun refreshRotatesAndDetectsReuse() = runTest {
        val container = testContainer(MockScenario.Authenticated)
        val fresh = assertNotNull(container.authApi.refresh(MockTransport.SEED_REFRESH_TOKEN))
        assertTrue(fresh.accessToken.startsWith("mock-access-"))
        assertEquals(
            "AUTH_REFRESH_REUSED",
            assertFailsWith<ApiError.Http> {
                container.authApi.refresh(MockTransport.SEED_REFRESH_TOKEN)
            }.code,
        )
        assertEquals("AUTH_REFRESH_EXPIRED", assertFailsWith<ApiError.Http> { container.authApi.refresh("unknown") }.code)
        assertNotNull(container.authApi.refresh(fresh.refreshToken))
    }

    @Test
    fun todoMutationsChangeStateUntilReset() = runTest {
        val container = testContainer()
        container.signIn()
        val api = TodosApi(container.apiClient)
        val created = api.create("Новая").data
        assertEquals(4, api.list().data.size)
        assertEquals(true, api.update(created.id, created.version, done = true).data.done)
        assertEquals("VERSION_CONFLICT", assertFailsWith<ApiError.Http> { api.update(created.id, version = 99, done = false) }.code)
        api.delete(created.id)
        assertEquals("NOT_FOUND", assertFailsWith<ApiError.Http> { api.get(created.id) }.code)
        container.mockTransport!!.reset(MockScenario.Populated)
        assertEquals(3, api.list().data.size)
    }

    @Test
    fun cookieModeWorksThroughTheSameTransport() = runTest {
        val platform = FakePlatformDeps()
        val container = testContainer(platform = platform, config = testConfig(authMode = AuthMode.Cookie))
        container.signIn()
        assertEquals(AuthState.SignedIn, container.sessionStore.authState.value)
        assertTrue(platform.secureStore.values.isEmpty())
        assertEquals("u-1", container.userRepository.refresh().id)
        container.sessionRepository.logout()
        assertEquals(AuthState.SignedOut, container.sessionStore.authState.value)
    }
}
