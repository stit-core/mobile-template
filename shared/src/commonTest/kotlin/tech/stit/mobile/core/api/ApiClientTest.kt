package tech.stit.mobile.core.api

import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.api.createClientPlugin
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.HttpResponseData
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlinx.io.IOException
import tech.stit.mobile.core.api.generated.TodoCreateInputBody
import tech.stit.mobile.core.config.AuthMode
import tech.stit.mobile.core.observability.Logger
import tech.stit.mobile.testing.FakeTelemetry
import tech.stit.mobile.testing.MemoryLogSink
import tech.stit.mobile.testing.testConfig
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class ApiClientTest {
    private val requests = mutableListOf<HttpRequestData>()
    private val telemetry = FakeTelemetry()
    private val json = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())
    private val problem = headersOf(HttpHeaders.ContentType, ContentType.Application.ProblemJson.toString())

    private fun client(
        authMode: AuthMode = AuthMode.Bearer,
        handler: suspend MockRequestHandleScope.(HttpRequestData, Int) -> HttpResponseData,
    ) = ApiClient(
        config = testConfig(authMode = authMode),
        engine = MockEngine { request ->
            requests += request
            handler(request, requests.size)
        },
        auth = createClientPlugin("NoopAuth") {},
        telemetry = telemetry,
        logger = Logger("test", MemoryLogSink()),
    )

    @Test
    fun everyRequestCarriesRequestIdClientAndMutationKey() = runTest {
        val client = client { _, _ -> respond("", HttpStatusCode.NoContent, json) }
        client.execute(HttpMethod.Post, "/api/todos") { setBody(TodoCreateInputBody(title = "x")) }
        client.execute(HttpMethod.Get, "/api/todos") {}
        val (post, get) = requests
        assertEquals("android", post.headers[HEADER_CLIENT])
        assertEquals(36, post.headers[HEADER_REQUEST_ID]?.length)
        assertEquals(36, post.headers[HEADER_IDEMPOTENCY_KEY]?.length)
        assertEquals(null, get.headers[HEADER_IDEMPOTENCY_KEY])
        assertEquals("https://api.demo.invalid/api/todos", get.url.toString())
        assertTrue(telemetry.breadcrumbs.first().startsWith("http: POST /api/todos → 204"), telemetry.breadcrumbs.first())
    }

    @Test
    fun cookieModeIntroducesItselfAsWeb() = runTest {
        val client = client(AuthMode.Cookie) { _, _ -> respond("", HttpStatusCode.NoContent, json) }
        client.execute(HttpMethod.Get, "/api/users/me") {}
        assertEquals("web", requests.single().headers[HEADER_CLIENT])
    }

    @Test
    fun problemJsonBecomesTypedError() = runTest {
        val body = """{"code":"VALIDATION","detail":"bad","status":422,"errors":[{"location":"body.email","message":"invalid"}]}"""
        val client = client { _, _ -> respond(body, HttpStatusCode.UnprocessableEntity, problem) }
        val error = assertFailsWith<ApiError.Http> { client.execute(HttpMethod.Post, "/api/todos") {} }
        assertEquals(422, error.status)
        assertEquals("VALIDATION", error.code)
        assertEquals("bad", error.detail)
        assertEquals(mapOf("email" to "invalid"), error.fieldErrors)
        assertNotNull(error.requestId)
        assertEquals("error.validation", error.messageKey.value)
    }

    @Test
    fun unparsableErrorBodyFallsBackToStatusCode() = runTest {
        val client = client { _, _ -> respond("<html/>", HttpStatusCode.BadGateway, json) }
        val error = assertFailsWith<ApiError.Http> { client.execute(HttpMethod.Post, "/api/todos") { noRetry() } }
        assertEquals("HTTP_502", error.code)
    }

    @Test
    fun retriesRetryableStatusesHonouringRetryAfter() = runTest {
        val client = client { _, attempt ->
            if (attempt ==
                1
            ) {
                respond("", HttpStatusCode.ServiceUnavailable, headersOf(HttpHeaders.RetryAfter, "1"))
            } else {
                respond("", HttpStatusCode.NoContent, json)
            }
        }
        client.execute(HttpMethod.Post, "/api/todos") {}
        assertEquals(2, requests.size)
        assertEquals(requests[0].headers[HEADER_REQUEST_ID], requests[1].headers[HEADER_REQUEST_ID])
        assertEquals(requests[0].headers[HEADER_IDEMPOTENCY_KEY], requests[1].headers[HEADER_IDEMPOTENCY_KEY])
    }

    @Test
    fun noRetryRequestsFailOnFirstAttempt() = runTest {
        val client = client { _, _ -> respond("", HttpStatusCode.ServiceUnavailable, json) }
        val error = assertFailsWith<ApiError.Http> { client.execute(HttpMethod.Post, "/api/auth/refresh") { noRetry() } }
        assertEquals(503, error.status)
        assertEquals(1, requests.size)
    }

    @Test
    fun networkFailureBecomesNetworkErrorAfterRetries() = runTest {
        val client = client { _, _ -> throw IOException("connection reset") }
        assertFailsWith<ApiError.Network> { client.execute(HttpMethod.Get, "/api/todos") {} }
        assertEquals(1 + RetryPolicy.MAX_RETRIES, requests.size)
    }
}
