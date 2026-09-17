package tech.stit.mobile.core.auth

import io.ktor.client.plugins.api.ClientPlugin
import io.ktor.client.plugins.api.Send
import io.ktor.client.plugins.api.createClientPlugin
import io.ktor.client.request.bearerAuth
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import tech.stit.mobile.core.api.SkipAuth

// Свой плагин вместо ktor Auth: single-flight refresh живёт в SessionStore и один для Bearer и cookie-режима.
internal fun authPlugin(session: SessionStore): ClientPlugin<Unit> = createClientPlugin("StitAuth") {
    onRequest { request, _ ->
        if (request.attributes.contains(SkipAuth)) return@onRequest
        session.validAccessToken()?.let { request.bearerAuth(it) }
    }
    on(Send) { request ->
        val call = proceed(request)
        if (call.response.status != HttpStatusCode.Unauthorized || request.attributes.contains(SkipAuth)) return@on call
        val stale = request.headers[HttpHeaders.Authorization]?.removePrefix(BEARER_PREFIX)
        if (!session.refreshAfter(stale)) return@on call
        session.accessToken?.let { request.headers[HttpHeaders.Authorization] = "$BEARER_PREFIX$it" }
        proceed(request)
    }
}

private const val BEARER_PREFIX = "Bearer "
