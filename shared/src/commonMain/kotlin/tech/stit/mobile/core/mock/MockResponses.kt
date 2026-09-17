package tech.stit.mobile.core.mock

import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.HttpResponseData
import io.ktor.http.ContentType
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import tech.stit.mobile.core.api.generated.TokenPairBody

// Ответы MockTransport в форме бэкенда: JSON с конвертом data, problem+json для ошибок, Set-Cookie для cookie-режима.

internal fun <T> MockRequestHandleScope.ok(
    json: Json,
    value: T,
    serializer: KSerializer<T>,
    status: HttpStatusCode = HttpStatusCode.OK,
    cookies: TokenPairBody? = null,
): HttpResponseData = respond(json.encodeToString(serializer, value), status, jsonHeaders(cookies))

internal fun MockRequestHandleScope.ok(rawJson: String): HttpResponseData = respond(rawJson, HttpStatusCode.OK, jsonHeaders(null))

internal fun MockRequestHandleScope.noContent(cookies: TokenPairBody? = null): HttpResponseData =
    respond("", HttpStatusCode.NoContent, jsonHeaders(cookies))

internal fun MockRequestHandleScope.problem(status: HttpStatusCode, code: String): HttpResponseData = respond(
    """{"code":"$code","status":${status.value},"title":"${status.description}","type":"about:blank"}""",
    status,
    problemHeaders,
)

internal val problemHeaders: Headers = headersOf(HttpHeaders.ContentType, ContentType.Application.ProblemJson.toString())

private fun jsonHeaders(cookies: TokenPairBody?): Headers = if (cookies == null) {
    headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())
} else {
    headersOf(
        HttpHeaders.ContentType to listOf(ContentType.Application.Json.toString()),
        HttpHeaders.SetCookie to
            listOf("access_token=${cookies.accessToken}; Path=/; HttpOnly", "refresh_token=${cookies.refreshToken}; Path=/; HttpOnly"),
    )
}

internal fun HttpRequestData.cookie(name: String): String? = headers[HttpHeaders.Cookie]
    ?.split(';')
    ?.map { it.trim() }
    ?.firstOrNull { it.startsWith("$name=") }
    ?.substringAfter('=')
