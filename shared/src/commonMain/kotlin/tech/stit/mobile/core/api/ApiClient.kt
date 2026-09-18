package tech.stit.mobile.core.api

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.plugins.HttpRequestRetry
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.api.ClientPlugin
import io.ktor.client.plugins.api.createClientPlugin
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.cookies.HttpCookies
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.accept
import io.ktor.client.request.header
import io.ktor.client.request.request
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.client.statement.request
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import io.ktor.util.AttributeKey
import kotlinx.io.IOException
import kotlinx.serialization.json.Json
import tech.stit.mobile.core.config.AppConfig
import tech.stit.mobile.core.config.AuthMode
import tech.stit.mobile.core.i18n.LocaleProvider
import tech.stit.mobile.core.lib.AppInfo
import tech.stit.mobile.core.lib.Platform
import tech.stit.mobile.core.lib.currentPlatform
import tech.stit.mobile.core.lib.newUuid
import tech.stit.mobile.core.observability.Logger
import tech.stit.mobile.core.observability.TelemetrySink

internal const val HEADER_REQUEST_ID = "X-Request-ID"
internal const val HEADER_CLIENT = "X-Client"
internal const val HEADER_IDEMPOTENCY_KEY = "Idempotency-Key"

// Запрос без Authorization и без повтора по 401: login и сам refresh (иначе рекурсия refresh → 401 → refresh).
internal val SkipAuth = AttributeKey<Boolean>("StitSkipAuth")

// Запрос без ретраев: refresh ротирует токен, повтор с тем же refresh — это reuse и разлогин.
internal val NoRetry = AttributeKey<Boolean>("StitNoRetry")

// Тонкий клиент над Ktor (07 §2.5): X-Request-ID, Idempotency-Key на мутациях, ретраи, problem+json → ApiError.
internal class ApiClient(
    private val config: AppConfig,
    appInfo: AppInfo,
    private val locale: LocaleProvider,
    engine: HttpClientEngine,
    private val auth: ClientPlugin<Unit>,
    private val telemetry: TelemetrySink,
    private val logger: Logger,
    private val json: Json = ApiJson,
) {
    private val client = clientName(config.authMode)

    // Формат 07 §2.6: acme-mobile/1.4.2+1004002 (Android 16; Pixel 8; android).
    private val userAgent = "${appInfo.appId}/${appInfo.versionWithBuild} (${appInfo.osVersion}; ${appInfo.deviceModel}; $client)"

    // По клиенту на движок: demo в рантайме уходит на MockTransport и возвращается на тот же живой движок платформы,
    // поэтому клиенты переиспользуются, а не закрываются под уже летящими запросами.
    private val clients = mutableMapOf<HttpClientEngine, HttpClient>()

    var http: HttpClient = clientFor(engine)
        private set

    fun useEngine(engine: HttpClientEngine) {
        http = clientFor(engine)
    }

    private fun clientFor(engine: HttpClientEngine): HttpClient = clients.getOrPut(engine) { newClient(engine) }

    private fun newClient(engine: HttpClientEngine): HttpClient = HttpClient(engine) {
        expectSuccess = false
        install(ContentNegotiation) { json(json) }
        install(HttpTimeout) {
            requestTimeoutMillis = REQUEST_TIMEOUT_MS
            connectTimeoutMillis = CONNECT_TIMEOUT_MS
        }
        // Порядок важен: retry установлен раньше auth — снаружи него, поэтому 401 сначала лечится refresh'ем, а 503 повторяется целиком.
        install(HttpRequestRetry) {
            maxRetries = RetryPolicy.MAX_RETRIES
            retryIf { request, response ->
                response.status.value in RetryPolicy.retryableStatuses && request.isRepeatable()
            }
            retryOnExceptionIf { request, cause -> cause is IOException && request.isRepeatable() }
            delayMillis(respectRetryAfterHeader = false) { attempt ->
                RetryPolicy.delayMs(attempt, response?.headers?.get(HttpHeaders.RetryAfter))
            }
        }
        install(auth)
        // Cookie-режим (AuthMode.Cookie): сессию держит jar в памяти — «оставаться в системе» не работает, это переходный режим.
        if (config.authMode == AuthMode.Cookie) install(HttpCookies)
        install(requestIdPlugin)
        defaultRequest {
            url(config.apiBaseUrl)
            header(HttpHeaders.UserAgent, userAgent)
            header(HEADER_CLIENT, client)
            header(HttpHeaders.AcceptLanguage, locale.languageTag)
            accept(ContentType.Application.Json)
        }
    }

    suspend inline fun <reified T> get(path: String, noinline block: HttpRequestBuilder.() -> Unit = {}): T =
        execute(HttpMethod.Get, path, block).body()

    suspend inline fun <reified T> post(path: String, noinline block: HttpRequestBuilder.() -> Unit = {}): T =
        execute(HttpMethod.Post, path, block).body()

    suspend inline fun <reified T> put(path: String, noinline block: HttpRequestBuilder.() -> Unit = {}): T =
        execute(HttpMethod.Put, path, block).body()

    suspend inline fun <reified T> patch(path: String, noinline block: HttpRequestBuilder.() -> Unit = {}): T =
        execute(HttpMethod.Patch, path, block).body()

    suspend fun delete(path: String, block: HttpRequestBuilder.() -> Unit = {}) {
        execute(HttpMethod.Delete, path, block)
    }

    suspend fun execute(method: HttpMethod, path: String, block: HttpRequestBuilder.() -> Unit): HttpResponse {
        val response = try {
            http.request(path) {
                this.method = method
                contentType(ContentType.Application.Json)
                if (method != HttpMethod.Get) header(HEADER_IDEMPOTENCY_KEY, newUuid())
                block()
            }
        } catch (e: IOException) {
            logger.warn("$method $path: сеть недоступна", e)
            throw ApiError.Network(e)
        }
        val requestId = response.headers[HEADER_REQUEST_ID] ?: response.request.headers[HEADER_REQUEST_ID]
        telemetry.breadcrumb("http", "${method.value} $path → ${response.status.value} [$requestId]")
        if (!response.status.isSuccess()) throw response.toApiError(requestId)
        return response
    }

    private suspend fun HttpResponse.toApiError(requestId: String?): ApiError.Http {
        val problem = runCatching { json.decodeFromString(ProblemBody.serializer(), bodyAsText()) }.getOrNull()
        val error = ApiError.Http(
            status = status.value,
            code = problem?.code ?: "HTTP_${status.value}",
            detail = problem?.detail,
            requestId = requestId,
            fieldErrors = problem?.fieldErrors().orEmpty(),
        )
        logger.warn("${request.method.value} ${request.url.encodedPath} → ${error.message} [$requestId]")
        return error
    }

    private companion object {
        const val REQUEST_TIMEOUT_MS = 30_000L
        const val CONNECT_TIMEOUT_MS = 10_000L

        val requestIdPlugin: ClientPlugin<Unit> = createClientPlugin("StitRequestId") {
            onRequest { request, _ -> request.headers.append(HEADER_REQUEST_ID, newUuid()) }
        }

        // JVM ведёт себя как Android; cookie-режим представляется web, иначе бэкенд с B-3 ответит токенами вместо cookie.
        fun clientName(authMode: AuthMode): String = when {
            authMode == AuthMode.Cookie -> "web"
            currentPlatform == Platform.Ios -> "ios"
            else -> "android"
        }

        fun io.ktor.client.request.HttpRequest.isRepeatable(): Boolean =
            !attributes.contains(NoRetry) && RetryPolicy.isRepeatable(method, headers.contains(HEADER_IDEMPOTENCY_KEY))

        fun HttpRequestBuilder.isRepeatable(): Boolean =
            !attributes.contains(NoRetry) && RetryPolicy.isRepeatable(method, headers.contains(HEADER_IDEMPOTENCY_KEY))
    }
}

internal fun HttpRequestBuilder.skipAuth() {
    attributes.put(SkipAuth, true)
}

internal fun HttpRequestBuilder.noRetry() {
    attributes.put(NoRetry, true)
}
