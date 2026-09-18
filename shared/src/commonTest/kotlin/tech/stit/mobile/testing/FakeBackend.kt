package tech.stit.mobile.testing

import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockEngineConfig
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.HttpResponseData
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.CoroutineDispatcher

// Подставной «настоящий» бэкенд для тестов runtime-переключателя demo: сюда уходят запросы обычного режима и
// не уходит ни один запрос demo-режима — по списку paths это и проверяется.
class FakeBackend(dispatcher: CoroutineDispatcher) {
    var demoLogin: Boolean = true

    // Бэкенд без B-17 отвечает 404 — клиент requirements обязан остаться на безопасном Default.
    var hasRequirements: Boolean = true
    val paths: MutableList<String> = mutableListOf()

    val engine: HttpClientEngine = MockEngine(
        MockEngineConfig().apply {
            this.dispatcher = dispatcher
            addHandler { request ->
                val path = request.url.encodedPath
                paths += path
                route(path)
            }
        },
    )

    private fun MockRequestHandleScope.route(path: String): HttpResponseData = when (path) {
        "/api/app/requirements" -> if (hasRequirements) respond(requirements(), HttpStatusCode.OK, JSON) else notFound()
        "/api/auth/refresh" -> respond(TOKENS, HttpStatusCode.OK, JSON)
        "/api/users/me" -> respond(USER, HttpStatusCode.OK, JSON)
        "/api/todos" -> respond(TODOS, HttpStatusCode.OK, JSON)
        else -> notFound()
    }

    private fun MockRequestHandleScope.notFound(): HttpResponseData = respond(NOT_FOUND, HttpStatusCode.NotFound, JSON)

    private fun requirements(): String = """
        {"data":{"status":"ok","min_version":"1.0.0","latest_version":"1.0.0","flags":{"demo_login":"$demoLogin"},
        "links":{"privacy_policy":"https://real.example.com/privacy"}}}
    """.trimIndent().replace("\n", "")

    private companion object {
        val JSON = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())
        const val TOKENS =
            """{"access_token":"real-access-1","expires_in":900,"refresh_token":"real-refresh-2","token_type":"Bearer"}"""
        const val USER = """{"data":{"id":"real-1","email":"real@example.com","name":"Настоящий Пользователь",""" +
            """"status":"active","created_at":"2026-01-10T09:00:00Z","updated_at":"2026-09-01T12:00:00Z","version":1}}"""
        const val TODOS = """{"data":[{"id":"real-todo","title":"Настоящая задача","done":false,""" +
            """"created_at":"2026-09-10T08:00:00Z","updated_at":"2026-09-10T08:00:00Z","version":1}],""" +
            """"page":1,"size":100,"total":1}"""
        const val NOT_FOUND = """{"code":"NOT_FOUND","status":404,"title":"Not Found","type":"about:blank"}"""
    }
}
