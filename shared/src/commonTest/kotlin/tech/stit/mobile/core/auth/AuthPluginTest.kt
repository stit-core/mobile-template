package tech.stit.mobile.core.auth

import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.toByteArray
import io.ktor.client.request.HttpRequestData
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.test.runTest
import tech.stit.mobile.core.api.ApiClient
import tech.stit.mobile.core.api.ApiError
import tech.stit.mobile.core.api.AuthApi
import tech.stit.mobile.core.api.skipAuth
import tech.stit.mobile.core.config.AuthMode
import tech.stit.mobile.core.observability.Logger
import tech.stit.mobile.testing.FakeClock
import tech.stit.mobile.testing.FakeSecureStore
import tech.stit.mobile.testing.FakeTelemetry
import tech.stit.mobile.testing.MemoryLogSink
import tech.stit.mobile.testing.testConfig
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

// Плагин поверх настоящего ApiClient: сервер-заглушка принимает только токен текущего поколения.
class AuthPluginTest {
    private val json = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())
    private val requests = mutableListOf<HttpRequestData>()
    private val secure = FakeSecureStore()
    private var generation = 1
    private var refreshFails = false
    private val refreshes get() = requests.count { it.url.encodedPath == "/api/auth/refresh" }

    private val engine = MockEngine { request ->
        requests += request
        when (request.url.encodedPath) {
            "/api/auth/refresh" -> if (refreshFails) {
                respond("""{"code":"AUTH_REFRESH_REUSED"}""", HttpStatusCode.Unauthorized, json)
            } else {
                val presented = request.body.toByteArray().decodeToString()
                assertTrue(presented.contains("refresh-$generation"), presented)
                generation++
                respond(
                    """{"access_token":"good-$generation","refresh_token":"refresh-$generation","token_type":"Bearer","expires_in":900}""",
                    HttpStatusCode.OK,
                    json,
                )
            }

            else -> if (request.headers[HttpHeaders.Authorization] == "Bearer good-$generation") {
                respond("{}", HttpStatusCode.OK, json)
            } else {
                respond("""{"code":"AUTH_REQUIRED"}""", HttpStatusCode.Unauthorized, json)
            }
        }
    }

    private val client: ApiClient
    private val store =
        SessionStore(secure, {
            AuthApi(client).refresh(it)?.let { pair -> TokenPair(pair.accessToken, pair.refreshToken, pair.expiresIn) }
        }, AuthMode.Bearer, FakeClock(), Logger("test", MemoryLogSink()), onSignedOut = {})

    init {
        client = ApiClient(testConfig(), engine, authPlugin(store), FakeTelemetry(), Logger("test", MemoryLogSink()))
    }

    @Test
    fun staleAccessIsRefreshedOnceAndRequestRepeated() = runTest {
        store.signIn(TokenPair("stale", "refresh-1", 900))
        client.execute(HttpMethod.Get, "/api/users/me") {}
        assertEquals(listOf("/api/users/me", "/api/auth/refresh", "/api/users/me"), requests.map { it.url.encodedPath })
        assertEquals("good-2", store.accessToken)
    }

    @Test
    fun parallelStaleRequestsShareOneRefresh() = runTest {
        store.signIn(TokenPair("stale", "refresh-1", 900))
        List(3) { async { client.execute(HttpMethod.Get, "/api/todos") {} } }.awaitAll()
        assertEquals(1, refreshes)
        assertEquals(3, requests.count { it.headers[HttpHeaders.Authorization] == "Bearer good-2" })
    }

    @Test
    fun rejectedRefreshReturnsOriginalUnauthorizedAndSignsOut() = runTest {
        store.signIn(TokenPair("stale", "refresh-1", 900))
        refreshFails = true
        val error = assertFailsWith<ApiError.Http> { client.execute(HttpMethod.Get, "/api/users/me") {} }
        assertEquals("AUTH_REQUIRED", error.code)
        assertEquals(1, refreshes)
        assertEquals(AuthState.SignedOut, store.authState.value)
    }

    @Test
    fun skipAuthRequestsGoWithoutHeaderAndNeverRefresh() = runTest {
        store.signIn(TokenPair("stale", "refresh-1", 900))
        assertFailsWith<ApiError.Http> { client.execute(HttpMethod.Get, "/api/app/requirements") { skipAuth() } }
        assertEquals(0, refreshes)
        assertEquals(null, requests.single().headers[HttpHeaders.Authorization])
    }
}
