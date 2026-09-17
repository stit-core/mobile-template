package tech.stit.mobile.core.mock

import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockEngineConfig
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.toByteArray
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.HttpResponseData
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import tech.stit.mobile.core.api.HEADER_CLIENT
import tech.stit.mobile.core.api.generated.DeviceListOutputBody
import tech.stit.mobile.core.api.generated.DeviceOutputBody
import tech.stit.mobile.core.api.generated.DeviceRegisterInputBody
import tech.stit.mobile.core.api.generated.DeviceView
import tech.stit.mobile.core.api.generated.LoginInputBody
import tech.stit.mobile.core.api.generated.LoginOutputBody
import tech.stit.mobile.core.api.generated.RefreshInputBody
import tech.stit.mobile.core.api.generated.SessionListOutputBody
import tech.stit.mobile.core.api.generated.TodoCreateInputBody
import tech.stit.mobile.core.api.generated.TodoListOutputBody
import tech.stit.mobile.core.api.generated.TodoOutputBody
import tech.stit.mobile.core.api.generated.TodoUpdateInputBody
import tech.stit.mobile.core.api.generated.TodoView
import tech.stit.mobile.core.api.generated.TokenPairBody
import tech.stit.mobile.core.api.generated.UserMeOutputBody
import tech.stit.mobile.core.api.generated.UserUpdateInputBody
import tech.stit.mobile.core.api.generated.UserUpdateOutputBody
import kotlin.time.Clock

// In-process бэкенд на MockEngine (07 §3.2): весь клиентский стек и demo-сборка для App Review идут через него, не в сеть.
class MockTransport internal constructor(private val json: Json, private val clock: Clock, dispatcher: CoroutineDispatcher) {
    var scenario: MockScenario = MockScenario.Populated
        private set

    // Задержка ответа для debug-панели: показать loading-состояния живьём.
    var latencyMs: Long = 0

    private val mutex = Mutex()
    private var state = MockState.load(scenario, json)

    // Обработчик идёт на переданном диспетчере: в тестах это StandardTestDispatcher, и latencyMs — виртуальное время.
    val engine: HttpClientEngine = MockEngine(
        MockEngineConfig().apply {
            this.dispatcher = dispatcher
            addHandler { request -> handle(request) }
        },
    )

    suspend fun reset(scenario: MockScenario) = mutex.withLock {
        this.scenario = scenario
        state = MockState.load(scenario, json)
    }

    private suspend fun MockRequestHandleScope.handle(request: HttpRequestData): HttpResponseData {
        if (latencyMs > 0) delay(latencyMs)
        return mutex.withLock {
            if (scenario == MockScenario.Error) {
                respond(Fixtures.files.getValue("error/error"), HttpStatusCode.InternalServerError, problemHeaders)
            } else {
                route(request)
            }
        }
    }

    private suspend fun MockRequestHandleScope.route(request: HttpRequestData): HttpResponseData {
        val path = request.url.encodedPath
        return when {
            path.startsWith("/api/auth/") -> auth(request, path)
            path == "/api/app/requirements" -> ok(Fixtures.files.getValue("${scenario.fixtureDir}/requirements"))
            !request.isAuthorized() -> problem(HttpStatusCode.Unauthorized, "AUTH_REQUIRED")
            path.startsWith("/api/users/") -> users(request, path)
            path.startsWith("/api/v1/devices") -> devices(request, path)
            path.startsWith("/api/todos") -> todos(request, path)
            else -> problem(HttpStatusCode.NotFound, "NOT_FOUND")
        }
    }

    private suspend fun MockRequestHandleScope.auth(request: HttpRequestData, path: String): HttpResponseData = when {
        request.method != HttpMethod.Post -> sessions(request, path)
        path == "/api/auth/login" -> login(request)
        path == "/api/auth/refresh" -> refresh(request)
        path == "/api/auth/logout" -> noContent()
        !request.isAuthorized() -> problem(HttpStatusCode.Unauthorized, "AUTH_REQUIRED")
        path == "/api/auth/change-password" -> noContent()
        else -> problem(HttpStatusCode.NotFound, "NOT_FOUND")
    }

    private fun MockRequestHandleScope.sessions(request: HttpRequestData, path: String): HttpResponseData = when {
        !request.isAuthorized() -> problem(HttpStatusCode.Unauthorized, "AUTH_REQUIRED")

        request.method == HttpMethod.Get && path == "/api/auth/sessions" ->
            ok(SessionListOutputBody(state.sessions, state.sessions.size.toLong()), SessionListOutputBody.serializer())

        request.method == HttpMethod.Delete && path == "/api/auth/sessions" -> noContent().also { state.sessions.retainAll { it.current } }

        request.method == HttpMethod.Delete && path.startsWith("/api/auth/sessions/") ->
            noContent().also { state.sessions.removeAll { it.jti == path.substringAfterLast('/') } }

        else -> problem(HttpStatusCode.NotFound, "NOT_FOUND")
    }

    private suspend fun MockRequestHandleScope.users(request: HttpRequestData, path: String): HttpResponseData = when (request.method) {
        HttpMethod.Get -> if (path ==
            "/api/users/me"
        ) {
            ok(UserMeOutputBody(state.user), UserMeOutputBody.serializer())
        } else {
            problem(HttpStatusCode.NotFound, "NOT_FOUND")
        }

        HttpMethod.Patch -> {
            val body = request.body<UserUpdateInputBody>()
            state.user = state.user.copy(name = body.name ?: state.user.name, version = state.user.version?.plus(1))
            ok(UserUpdateOutputBody(state.user), UserUpdateOutputBody.serializer())
        }

        HttpMethod.Delete -> noContent()

        else -> problem(HttpStatusCode.NotFound, "NOT_FOUND")
    }

    private suspend fun MockRequestHandleScope.devices(request: HttpRequestData, path: String): HttpResponseData = when (request.method) {
        HttpMethod.Get -> ok(DeviceListOutputBody(state.devices, state.devices.size.toLong()), DeviceListOutputBody.serializer())
        HttpMethod.Put -> registerDevice(request.body())
        HttpMethod.Delete -> noContent().also { state.devices.removeAll { it.id == path.substringAfterLast('/') } }
        else -> problem(HttpStatusCode.NotFound, "NOT_FOUND")
    }

    private suspend fun MockRequestHandleScope.todos(request: HttpRequestData, path: String): HttpResponseData {
        val method = request.method
        val id = path.substringAfterLast('/')
        return when {
            method == HttpMethod.Get && path == "/api/todos" -> ok(
                TodoListOutputBody(state.todos.toList(), 1, state.todos.size.toLong(), state.todos.size.toLong()),
                TodoListOutputBody.serializer(),
            )

            method == HttpMethod.Post && path == "/api/todos" -> createTodo(request.body())

            method == HttpMethod.Get -> state.todos.firstOrNull { it.id == id }
                ?.let { ok(TodoOutputBody(it), TodoOutputBody.serializer()) } ?: problem(HttpStatusCode.NotFound, "NOT_FOUND")

            method == HttpMethod.Patch -> updateTodo(id, request.body())

            method == HttpMethod.Delete -> noContent().also { state.todos.removeAll { it.id == id } }

            else -> problem(HttpStatusCode.NotFound, "NOT_FOUND")
        }
    }

    // Любые непустые email и пароль подходят: demo-учётка для ревьюера магазина не хранится в коде.
    private suspend fun MockRequestHandleScope.login(request: HttpRequestData): HttpResponseData {
        val body = request.body<LoginInputBody>()
        if (body.email.isBlank() || body.password.isBlank()) return problem(HttpStatusCode.Unauthorized, "AUTH_INVALID_CREDENTIALS")
        val tokens = issueTokens()
        return if (request.headers[HEADER_CLIENT] == "web") {
            ok(state.login.copy(tokens = null), LoginOutputBody.serializer(), cookies = tokens)
        } else {
            ok(state.login.copy(tokens = tokens), LoginOutputBody.serializer())
        }
    }

    private suspend fun MockRequestHandleScope.refresh(request: HttpRequestData): HttpResponseData {
        val fromBody = runCatching { request.body<RefreshInputBody>().refreshToken }.getOrNull()
        val presented = fromBody ?: request.cookie("refresh_token")
        return when (presented) {
            state.refreshToken -> issueTokens().let {
                if (fromBody !=
                    null
                ) {
                    ok(it, TokenPairBody.serializer())
                } else {
                    noContent(cookies = it)
                }
            }

            in state.usedRefreshTokens -> problem(HttpStatusCode.Unauthorized, "AUTH_REFRESH_REUSED")

            else -> problem(HttpStatusCode.Unauthorized, "AUTH_REFRESH_EXPIRED")
        }
    }

    private fun issueTokens(): TokenPairBody {
        state.refreshToken?.let(state.usedRefreshTokens::add)
        val n = ++state.issued
        val pair =
            TokenPairBody(
                accessToken = "$ACCESS_PREFIX$n",
                expiresIn = ACCESS_TTL_SECONDS,
                refreshToken = "mock-refresh-$n",
                tokenType = TokenPairBody.TokenType.Bearer,
            )
        state.refreshToken = pair.refreshToken
        return pair
    }

    private fun MockRequestHandleScope.registerDevice(body: DeviceRegisterInputBody): HttpResponseData {
        val now = clock.now()
        val existing = state.devices.firstOrNull { it.provider == body.provider.value && it.appVersion == body.appVersion }
        val device = existing?.copy(lastSeenAt = now) ?: DeviceView(
            appVersion = body.appVersion,
            createdAt = now,
            id = "dev-${state.devices.size + 1}",
            lastSeenAt = now,
            platform = body.platform.value,
            provider = body.provider.value,
            deviceModel = body.deviceModel,
        )
        state.devices.removeAll { it.id == device.id }
        state.devices.add(device)
        return ok(DeviceOutputBody(device), DeviceOutputBody.serializer())
    }

    private fun MockRequestHandleScope.createTodo(body: TodoCreateInputBody): HttpResponseData {
        val now = clock.now()
        val todo = TodoView(createdAt = now, done = false, id = "todo-${++state.todoSeq}", title = body.title, updatedAt = now, version = 1)
        state.todos.add(0, todo)
        return ok(TodoOutputBody(todo), TodoOutputBody.serializer(), HttpStatusCode.Created)
    }

    private fun MockRequestHandleScope.updateTodo(id: String, body: TodoUpdateInputBody): HttpResponseData {
        val index = state.todos.indexOfFirst { it.id == id }
        val current = state.todos.getOrNull(index)
        return when {
            current == null -> problem(HttpStatusCode.NotFound, "NOT_FOUND")

            current.version != body.version -> problem(HttpStatusCode.Conflict, "VERSION_CONFLICT")

            else -> {
                val updated = current.copy(
                    title = body.title ?: current.title,
                    done = body.done ?: current.done,
                    updatedAt = clock.now(),
                    version =
                    current.version + 1,
                )
                state.todos[index] = updated
                ok(TodoOutputBody(updated), TodoOutputBody.serializer())
            }
        }
    }

    // Любой выданный access принимается и после смены сценария: переключение в debug-панели не разлогинивает.
    private fun HttpRequestData.isAuthorized(): Boolean {
        val bearer = headers[HttpHeaders.Authorization]?.removePrefix("Bearer ")
        return (bearer ?: cookie("access_token"))?.startsWith(ACCESS_PREFIX) == true
    }

    private suspend inline fun <reified T> HttpRequestData.body(): T = json.decodeFromString(body.toByteArray().decodeToString())

    private fun <T> MockRequestHandleScope.ok(
        value: T,
        serializer: KSerializer<T>,
        status: HttpStatusCode = HttpStatusCode.OK,
        cookies: TokenPairBody? = null,
    ) = ok(json, value, serializer, status, cookies)

    companion object {
        // Refresh-токен сценария authenticated: DemoMode кладёт его в SecureStore, транспорт принимает как выданный.
        const val SEED_REFRESH_TOKEN = "mock-refresh-seed"
        private const val ACCESS_PREFIX = "mock-access-"
        private const val ACCESS_TTL_SECONDS = 900L
    }
}
